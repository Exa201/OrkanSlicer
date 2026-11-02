#include "ModelArrange.hpp"

#include <algorithm>
#include <numeric>
#include <libslic3r/Model.hpp>
#include <libslic3r/Geometry/ConvexHull.hpp>
#include <libslic3r/ClipperUtils.hpp>
#include <libslic3r/Print.hpp>
#include "MTUtils.hpp"

namespace Slic3r {

arrangement::ArrangePolygons get_arrange_polys(const Model &model, ModelInstancePtrs &instances)
{
    size_t count = 0;
    for (auto obj : model.objects) count += obj->instances.size();

    ArrangePolygons input;
    input.reserve(count);
    instances.clear(); instances.reserve(count);
    ArrangePolygon ap;
    for (ModelObject *mo : model.objects)
        for (ModelInstance *minst : mo->instances) {
            minst->get_arrange_polygon(&ap);
            input.emplace_back(ap);
            instances.emplace_back(minst);
        }

    return input;
}

void fill_compound_arrange_gaps(ArrangePolygons &polygons, const Points &bed)
{
    // The arrange packer treats each model as one rigid polygon. For concave and multipart
    // models, its nominal placement can leave usable regions or overlap projected islands.
    // Reposition only whole instances, using exact projected geometry for each collision check.
    if (bed.size() < 3 || polygons.size() < 2)
        return;

    struct PartGeometry { ExPolygons parts; ExPolygons clearance; BoundingBox bounds; };
    std::vector<PartGeometry> geometry(polygons.size());
    std::vector<size_t> hosts;
    for (size_t i = 0; i < polygons.size(); ++i) {
        ArrangePolygon &item = polygons[i];
        if (item.bed_idx != 0 || item.physical_parts.empty())
            continue;
        geometry[i].parts = item.physical_parts;
        Points points;
        double exact_area = 0.0;
        for (ExPolygon &part : geometry[i].parts) {
            part.rotate(item.rotation);
            part.translate(item.translation.x(), item.translation.y());
            geometry[i].bounds.merge(part.contour.bounding_box());
            exact_area += part.area();
            points.insert(points.end(), part.contour.points.begin(), part.contour.points.end());
        }
        geometry[i].clearance = offset_ex(geometry[i].parts, float(item.inflation), ClipperLib::jtSquare);
        const Polygon convex = Geometry::convex_hull(std::move(points));
        if (geometry[i].parts.size() > 1 ||
            (exact_area > 0.0 && convex.area() > exact_area * 1.05))
            hosts.push_back(i);
    }
    if (hosts.empty())
        return;

    std::stable_sort(hosts.begin(), hosts.end(), [&](size_t lhs, size_t rhs) {
        const Vec2crd a = geometry[lhs].bounds.size(), b = geometry[rhs].bounds.size();
        return double(a.x()) * a.y() > double(b.x()) * b.y();
    });
    std::vector<size_t> candidates(polygons.size());
    std::iota(candidates.begin(), candidates.end(), size_t(0));
    std::stable_sort(candidates.begin(), candidates.end(), [&](size_t lhs, size_t rhs) {
        const Vec2crd a = geometry[lhs].bounds.size(), b = geometry[rhs].bounds.size();
        return double(a.x()) * a.y() < double(b.x()) * b.y();
    });

    const Polygon bed_polygon(bed);
    const BoundingBox bed_bounds(bed);
    const coord_t search_step = scaled(2.0);
    constexpr size_t MAX_SEARCH_POSITIONS = 12000;
    for (size_t candidate_idx : candidates) {
        ArrangePolygon &candidate = polygons[candidate_idx];
        if (candidate.bed_idx != 0 || geometry[candidate_idx].parts.empty())
            continue;
        ExPolygons local_parts = candidate.physical_parts;
        for (ExPolygon &part : local_parts) part.rotate(candidate.rotation);
        ExPolygons local_clearance = offset_ex(local_parts, float(candidate.inflation), ClipperLib::jtSquare);
        BoundingBox local_bounds;
        for (const ExPolygon &part : local_parts) local_bounds.merge(part.contour.bounding_box());
        if (!local_bounds.defined) continue;

        size_t visited = 0;
        bool placed = false;
        bool has_unsafe_overlap = false;
        for (size_t host_idx : hosts) {
            if (host_idx == candidate_idx || !geometry[host_idx].bounds.defined) continue;
            const BoundingBox &host_bounds = geometry[host_idx].bounds;
            if (geometry[host_idx].clearance.empty() || geometry[candidate_idx].clearance.empty() ||
                local_bounds.size().x() + 2 * candidate.inflation >= host_bounds.size().x() ||
                local_bounds.size().y() + 2 * candidate.inflation >= host_bounds.size().y()) continue;

            // Repack a nearby item into a real opening even when the first-pass polygon
            // packer placed it just outside the compound bounds. Far-away objects remain
            // untouched, and a failed optional gap search never invalidates a safe placement.
            const BoundingBox &current_bounds = geometry[candidate_idx].bounds;
            const coord_t gap_x = std::max<coord_t>(0, std::max(
                host_bounds.min.x() - current_bounds.max.x(),
                current_bounds.min.x() - host_bounds.max.x()));
            const coord_t gap_y = std::max<coord_t>(0, std::max(
                host_bounds.min.y() - current_bounds.max.y(),
                current_bounds.min.y() - host_bounds.max.y()));
            const coord_t nearby_x = local_bounds.size().x() + 2 * candidate.inflation;
            const coord_t nearby_y = local_bounds.size().y() + 2 * candidate.inflation;
            const bool nearby = gap_x <= nearby_x && gap_y <= nearby_y;
            const bool overlaps_clearance = host_bounds.overlap(current_bounds) &&
                !intersection_ex(geometry[host_idx].clearance,
                                geometry[candidate_idx].clearance).empty();
            if (!nearby && !overlaps_clearance) continue;
            has_unsafe_overlap |= overlaps_clearance;

            for (coord_t y = host_bounds.min.y();
                 y + local_bounds.size().y() <= host_bounds.max.y() && !placed; y += search_step) {
                for (coord_t x = host_bounds.min.x();
                     x + local_bounds.size().x() <= host_bounds.max.x(); x += search_step) {
                    if (++visited > MAX_SEARCH_POSITIONS) break;
                    const Vec2crd delta{x - local_bounds.min.x(), y - local_bounds.min.y()};
                    ExPolygons proposed = local_clearance;
                    for (ExPolygon &part : proposed) part.translate(delta.x(), delta.y());
                    BoundingBox proposed_bounds;
                    bool inside_bed = true;
                    for (const ExPolygon &part : proposed) {
                        proposed_bounds.merge(part.contour.bounding_box());
                        for (const Point &point : part.contour.points)
                            if (!bed_polygon.contains(point)) { inside_bed = false; break; }
                        if (!inside_bed) break;
                    }
                    if (!inside_bed || proposed_bounds.min.x() < bed_bounds.min.x() ||
                        proposed_bounds.min.y() < bed_bounds.min.y() ||
                        proposed_bounds.max.x() > bed_bounds.max.x() ||
                        proposed_bounds.max.y() > bed_bounds.max.y()) continue;

                    bool collision = false;
                    for (size_t other_idx = 0; other_idx < geometry.size() && !collision; ++other_idx) {
                        if (other_idx == candidate_idx || polygons[other_idx].bed_idx != 0 ||
                            geometry[other_idx].clearance.empty()) continue;
                        BoundingBox other_bounds;
                        for (const ExPolygon &part : geometry[other_idx].clearance)
                            other_bounds.merge(part.contour.bounding_box());
                        if (proposed_bounds.overlap(other_bounds))
                            collision = !intersection_ex(proposed, geometry[other_idx].clearance).empty();
                    }
                    if (collision) continue;

                    candidate.translation = delta;
                    geometry[candidate_idx].parts = local_parts;
                    geometry[candidate_idx].bounds.reset();
                    for (ExPolygon &part : geometry[candidate_idx].parts) {
                        part.translate(delta.x(), delta.y());
                        geometry[candidate_idx].bounds.merge(part.contour.bounding_box());
                    }
                    geometry[candidate_idx].clearance = std::move(proposed);
                    placed = true;
                    break;
                }
                if (visited > MAX_SEARCH_POSITIONS) break;
            }
            if (placed || visited > MAX_SEARCH_POSITIONS) break;
        }
        // Never report a successful one-bed arrangement while retaining a known collision.
        // The import caller will retry the model on an automatically created overflow plate.
        if (has_unsafe_overlap && !placed)
            candidate.bed_idx = arrangement::UNARRANGED;
    }
}

bool validate_arrange_placements(ArrangePolygons &polygons, const Points &bed)
{
    // Validate final bed containment and packed outlines before applying transforms to instances.
    // This is a last safety net for imported meshes whose projection is approximated by a
    // hull or whose exact-outline gap-fill changed a placement after the main NFP pass.
    std::vector<ExPolygons> clearances(polygons.size());
    std::vector<BoundingBox> bounds(polygons.size());
    const Polygon bed_polygon(bed);
    for (size_t i = 0; i < polygons.size(); ++i) {
        ArrangePolygon &item = polygons[i];
        if (item.bed_idx != 0)
            continue;

        ExPolygons parts = item.physical_parts;
        if (parts.empty() && !item.poly.contour.empty())
            parts.emplace_back(item.poly);
        for (ExPolygon &part : parts) {
            part.rotate(item.rotation);
            part.translate(item.translation.x(), item.translation.y());
            for (const Point &point : part.contour.points) {
                if (!bed_polygon.contains(point)) {
                    item.bed_idx = arrangement::UNARRANGED;
                    return false;
                }
            }
            bounds[i].merge(part.contour.bounding_box());
        }
        if (parts.empty())
            continue;
        clearances[i] = item.inflation > 0
            ? offset_ex(parts, float(item.inflation), ClipperLib::jtSquare)
            : std::move(parts);
    }

    for (size_t first = 0; first < polygons.size(); ++first) {
        if (polygons[first].bed_idx != 0 || clearances[first].empty())
            continue;
        for (size_t second = first + 1; second < polygons.size(); ++second) {
            if (polygons[second].bed_idx != 0 || clearances[second].empty() ||
                !bounds[first].overlap(bounds[second]))
                continue;
            if (!intersection_ex(clearances[first], clearances[second]).empty()) {
                // Keep already-arranged objects in place and let the importer retry this
                // later object on the current or a new plate. No partial transforms commit.
                polygons[second].bed_idx = arrangement::UNARRANGED;
                return false;
            }
        }
    }
    return true;
}

bool apply_arrange_polys(ArrangePolygons &input, ModelInstancePtrs &instances, VirtualBedFn vfn)
{
    // Arrange is committed to one physical bed. Applying only the objects that fit after
    // another item was sent to a virtual overflow bin leaves a partially shifted model.
    // Keep the source transforms intact so the caller can retry the overflow on another plate.
    const bool fits_single_bed = std::all_of(input.begin(), input.end(), [](const ArrangePolygon& polygon) {
        return polygon.bed_idx == 0;
    });
    if (!fits_single_bed) {
        if (vfn)
            for (ArrangePolygon& polygon : input)
                if (polygon.bed_idx != 0) vfn(polygon);
        return false;
    }

    for (size_t i = 0; i < input.size(); ++i)
        instances[i]->apply_arrange_result(input[i].translation.cast<double>(), input[i].rotation);
    return true;
}

Slic3r::arrangement::ArrangePolygon get_arrange_poly(const Model &model)
{
    ArrangePolygon ap;
    Points &apts = ap.poly.contour.points;
    for (const ModelObject *mo : model.objects)
        for (const ModelInstance *minst : mo->instances) {
            ArrangePolygon obj_ap;
            minst->get_arrange_polygon(&obj_ap);
            ap.poly.contour.rotate(obj_ap.rotation);
            ap.poly.contour.translate(obj_ap.translation.x(), obj_ap.translation.y());
            const Points &pts = obj_ap.poly.contour.points;
            std::copy(pts.begin(), pts.end(), std::back_inserter(apts));
        }

    apts = std::move(Geometry::convex_hull(apts).points);
    return ap;
}

void duplicate(Model &model, Slic3r::arrangement::ArrangePolygons &copies, VirtualBedFn vfn)
{
    for (ModelObject *o : model.objects) {
        // make a copy of the pointers in order to avoid recursion when appending their copies
        ModelInstancePtrs instances = o->instances;
        o->instances.clear();
        for (const ModelInstance *i : instances) {
            for (arrangement::ArrangePolygon &ap : copies) {
                if (ap.bed_idx != 0) vfn(ap);
                ModelInstance *instance = o->add_instance(*i);
                Vec2d pos = unscale(ap.translation);
                instance->set_offset(instance->get_offset() + to_3d(pos, 0.));
            }
        }
        o->invalidate_bounding_box();
    }
}

void duplicate_objects(Model &model, size_t copies_num)
{
    for (ModelObject *o : model.objects) {
        // make a copy of the pointers in order to avoid recursion when appending their copies
        ModelInstancePtrs instances = o->instances;
        for (const ModelInstance *i : instances)
            for (size_t k = 2; k <= copies_num; ++ k)
                o->add_instance(*i);
    }
}

// Set up arrange polygon for a ModelInstance and Wipe tower
template<class T>
arrangement::ArrangePolygon get_arrange_poly(T obj, const Slic3r::DynamicPrintConfig& config)
{
    ArrangePolygon ap = obj.get_arrange_polygon(config);
    //BBS: always set bed_idx to 0 to use original transforms with no bed_idx
    //if this object is not arranged, it can keep the original transforms
    //ap.bed_idx        = ap.translation.x() / bed_stride_x(plater);
    ap.bed_idx = 0;
    ap.setter = [obj](const ArrangePolygon& p) {
        if (p.is_arranged()) {
            Vec2d t = p.translation.cast<double>();
            //BBS: change to sudoku-style computation, do it in partplate list
            //t.x() += p.bed_idx * bed_stride(plater);
            //t.x() += col * bed_stride_x(plater);
            //t.y() -= row * bed_stride_y(plater);
            T{ obj }.apply_arrange_result(t, p.rotation, p.itemid);
        }
    };

    return ap;
}

template<>
arrangement::ArrangePolygon get_arrange_poly(ModelInstance* inst, const Slic3r::DynamicPrintConfig& config)
{
    return get_arrange_poly(PtrWrapper{ inst },config);
}

ArrangePolygon get_instance_arrange_poly(ModelInstance* instance, const Slic3r::DynamicPrintConfig& config)
{
    ArrangePolygon ap = get_arrange_poly(PtrWrapper{ instance }, config);

    //BBS: add temperature information
    if (config.has("curr_bed_type")) {
        ap.bed_temp = 0;
        ap.first_bed_temp = 0;
        BedType curr_bed_type = config.opt_enum<BedType>("curr_bed_type");

        const ConfigOptionInts* bed_opt = config.option<ConfigOptionInts>(get_bed_temp_key(curr_bed_type));
        if (bed_opt != nullptr)
            ap.bed_temp = bed_opt->get_at(ap.extrude_ids.front()-1);

        const ConfigOptionInts* bed_opt_1st_layer = config.option<ConfigOptionInts>(get_bed_temp_1st_layer_key(curr_bed_type));
        if (bed_opt_1st_layer != nullptr)
            ap.first_bed_temp = bed_opt_1st_layer->get_at(ap.extrude_ids.front()-1);
    }

    if (config.has("nozzle_temperature")) //get the print temperature
        ap.print_temp = config.opt_int("nozzle_temperature", ap.extrude_ids.front() - 1);
    if (config.has("nozzle_temperature_initial_layer")) //get the nozzle_temperature_initial_layer
        ap.first_print_temp = config.opt_int("nozzle_temperature_initial_layer", ap.extrude_ids.front() - 1);

    if (config.has("temperature_vitrification")) {
        ap.vitrify_temp = config.opt_int("temperature_vitrification", ap.extrude_ids.front() - 1);
    }

    // get filament temp types
    auto* filament_types_opt = dynamic_cast<const ConfigOptionStrings*>(config.option("filament_type"));
    if (filament_types_opt) {
        std::set<int> filament_temp_types;
        for (auto i : ap.extrude_ids) {
            std::string type_str = filament_types_opt->get_at(i-1);
            int temp_type = Print::get_filament_temp_type(type_str);
            filament_temp_types.insert(temp_type);
        }
        ap.filament_temp_type = Print::get_compatible_filament_type(filament_temp_types);
    }

    // get brim width
    auto obj = instance->get_object();

    ap.brim_width = 1.0;
    // For by-layer printing, need to shrink bed a little, so the support won't go outside bed.
    // We set it to 5mm because that's how much a normal support will grow by default.
    // normal support 5mm, other support 22mm, no support 0mm
    auto supp_type_ptr = obj->get_config_value<ConfigOptionBool>(config, "enable_support");
    auto support_type_ptr = obj->get_config_value<ConfigOptionEnum<SupportType>>(config, "support_type");
    auto support_type = support_type_ptr->value;
    auto enable_support = supp_type_ptr->getBool();
    int support_int = support_type_ptr->getInt();

    if (enable_support && (support_type == stNormalAuto || support_type == stNormal))
        ap.brim_width = 6.0;
    else if (enable_support) {
        ap.brim_width = 24.0; // 2*MAX_BRANCH_RADIUS_FIRST_LAYER
        ap.has_tree_support = true;
    }

    auto size = obj->instance_convex_hull_bounding_box(instance).size();
    ap.height = size.z();
    ap.name = obj->name;
    return ap;
}

} // namespace Slic3r
