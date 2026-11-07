#include "ModelFilamentRemap.hpp"

#include "libslic3r/Model.hpp"

namespace Slic3r {

void remap_model_filaments(Model& model, const std::array<int, 17>& source_to_target)
{
    EnforcerBlockerStateMap state_map;
    for (size_t state = 0; state < state_map.size(); ++state)
        state_map[state] = static_cast<EnforcerBlockerType>(state);
    for (size_t source = 1; source < source_to_target.size(); ++source) {
        const int target = source_to_target[source];
        if (target >= 1 && target <= 16)
            state_map[source] = static_cast<EnforcerBlockerType>(target);
    }

    for (ModelObject* object : model.objects) {
        if (object == nullptr) continue;
        for (ModelVolume* volume : object->volumes) {
            if (volume == nullptr || !volume->is_model_part()) continue;
            const int source = volume->extruder_id();
            if (source >= 1 && source < static_cast<int>(source_to_target.size()) && source_to_target[source] >= 1)
                volume->config.set("extruder", source_to_target[source]);

            const auto& painting = volume->mmu_segmentation_facets.get_data();
            if (!painting.triangles_to_split.empty()) {
                TriangleSelector selector(volume->mesh());
                selector.deserialize(painting, false);
                selector.remap_triangle_state(state_map);
                volume->mmu_segmentation_facets.set(selector);
            }
        }
    }
}

} // namespace Slic3r
