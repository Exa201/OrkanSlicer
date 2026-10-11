package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.BuildConfig
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.fragment.ProfileListItem
import java.util.Collections
import java.util.HashMap
import java.util.regex.Pattern

/** Mutable profile value bag with OrcaSlicer key normalization at its Java API boundary. */
open class ConfigObject : ProfileListItem {
    companion object {
        const val PROFILE_LIST_PRINT = 0
        const val PROFILE_LIST_FILAMENT = 1
        const val PROFILE_LIST_PRINTER = 2

        @JvmField
        val KEY_MIGRATION: MutableMap<String, String> = buildKeyMigration()

        // Orca profile names accepted by older releases but replaced by current keys.
        private val ORCA_KEY_ALIASES: Map<String, String> = Collections.unmodifiableMap(
            hashMapOf(
                "wall_filament" to "outer_wall_filament_id",
                "wall_filament_id" to "outer_wall_filament_id",
                "outer_wall_filament" to "outer_wall_filament_id",
                "inner_wall_filament" to "inner_wall_filament_id",
                "sparse_infill_filament" to "sparse_infill_filament_id",
                "solid_infill_filament" to "internal_solid_filament_id",
                "top_solid_infill_filament" to "top_surface_filament_id",
                "bottom_solid_infill_filament" to "bottom_surface_filament_id",
                "top_one_wall_type" to "only_one_wall_top"
            )
        )

        private val PRINT_CONFIG_KEY_ALIASES: List<String> = Collections.unmodifiableList(
            listOf(
                "wall_filament",
                "wall_filament_id",
                "outer_wall_filament",
                "inner_wall_filament",
                "sparse_infill_filament",
                "solid_infill_filament",
                "top_solid_infill_filament",
                "bottom_solid_infill_filament",
                "top_one_wall_type"
            )
        )

        private val KEY_MIGRATION_REVERSE: Map<String, String> = buildReverse()

        private fun buildKeyMigration(): MutableMap<String, String> = HashMap<String, String>().apply {
            put("bed_shape", "printable_area")
            put("max_print_height", "printable_height")
            put("bed_temperature", "hot_plate_temp")
            put("first_layer_bed_temperature", "hot_plate_temp_initial_layer")
            put("temperature", "nozzle_temperature")
            put("first_layer_temperature", "nozzle_temperature_initial_layer")
            put("extrusion_multiplier", "filament_flow_ratio")
            put("cooling", "slow_down_for_layer_cooling")
            put("fan_always_on", "reduce_fan_stop_start_freq")
            put("fan_below_layer_time", "fan_cooling_layer_time")
            put("slowdown_below_layer_time", "slow_down_layer_time")
            put("min_print_speed", "slow_down_min_speed")
            put("max_fan_speed", "fan_max_speed")
            put("min_fan_speed", "fan_min_speed")
            put("disable_fan_first_layers", "close_fan_the_first_x_layers")
            put("bridge_fan_speed", "overhang_fan_speed")
            put("start_gcode", "machine_start_gcode")
            put("end_gcode", "machine_end_gcode")
            put("before_layer_gcode", "before_layer_change_gcode")
            put("layer_gcode", "layer_change_gcode")
            put("toolchange_gcode", "change_filament_gcode")
            put("pause_print_gcode", "machine_pause_gcode")
            put("between_objects_gcode", "printing_by_object_gcode")
            put("color_change_gcode", "change_extrusion_role_gcode")
            put("start_filament_gcode", "filament_start_gcode")
            put("end_filament_gcode", "filament_end_gcode")
            put("retract_length", "retraction_length")
            put("retract_speed", "retraction_speed")
            put("deretract_speed", "deretraction_speed")
            put("retract_lift", "z_hop")
            put("retract_before_travel", "retraction_minimum_travel")
            put("retract_layer_change", "retract_when_changing_layer")
            put("filament_retract_length", "filament_retraction_length")
            put("filament_retract_speed", "filament_retraction_speed")
            put("filament_deretract_speed", "filament_deretraction_speed")
            put("filament_retract_before_travel", "filament_retraction_minimum_travel")
            put("filament_retract_layer_change", "filament_retract_when_changing_layer")
            put("filament_retract_lift", "filament_z_hop")
            put("perimeters", "wall_loops")
            put("perimeter_speed", "inner_wall_speed")
            put("external_perimeter_speed", "outer_wall_speed")
            put("perimeter_extruder", "wall_filament")
            put("perimeter_extrusion_width", "inner_wall_line_width")
            put("external_perimeter_extrusion_width", "outer_wall_line_width")
            put("perimeter_generator", "wall_generator")
            put("external_perimeter_acceleration", "outer_wall_acceleration")
            put("thin_walls", "detect_thin_wall")
            put("top_one_perimeter_type", "top_one_wall_type")
            put("only_one_perimeter_first_layer", "only_one_wall_first_layer")
            put("extra_perimeters", "extra_perimeters_on_overhangs")
            put("avoid_crossing_perimeters", "reduce_crossing_wall")
            put("avoid_crossing_perimeters_max_detour", "max_travel_detour_distance")
            put("only_retract_when_crossing_perimeters", "reduce_infill_retraction")
            put("fill_density", "sparse_infill_density")
            put("fill_pattern", "sparse_infill_pattern")
            put("fill_angle", "infill_direction")
            put("infill_speed", "sparse_infill_speed")
            put("infill_extruder", "sparse_infill_filament")
            put("infill_extrusion_width", "sparse_infill_line_width")
            put("infill_acceleration", "sparse_infill_acceleration")
            put("infill_overlap", "infill_wall_overlap")
            put("infill_every_layers", "infill_combination")
            put("infill_first", "is_infill_first")
            put("solid_infill_speed", "internal_solid_infill_speed")
            put("solid_infill_extruder", "solid_infill_filament")
            put("solid_infill_extrusion_width", "internal_solid_infill_line_width")
            put("solid_infill_acceleration", "internal_solid_infill_acceleration")
            put("solid_infill_below_area", "minimum_sparse_infill_area")
            put("top_solid_infill_speed", "top_surface_speed")
            put("top_solid_infill_acceleration", "top_surface_acceleration")
            put("top_infill_extrusion_width", "top_surface_line_width")
            put("bridge_flow_ratio", "bridge_flow")
            put("gap_fill_speed", "gap_infill_speed")
            put("gap_fill_enabled", "gap_fill_target")
            put("top_solid_layers", "top_shell_layers")
            put("bottom_solid_layers", "bottom_shell_layers")
            put("top_solid_min_thickness", "top_shell_thickness")
            put("bottom_solid_min_thickness", "bottom_shell_thickness")
            put("top_fill_pattern", "top_surface_pattern")
            put("bottom_fill_pattern", "bottom_surface_pattern")
            put("extrusion_width", "line_width")
            put("first_layer_extrusion_width", "initial_layer_line_width")
            put("first_layer_height", "initial_layer_print_height")
            put("arc_fitting", "enable_arc_fitting")
            put("overhangs", "detect_overhang_wall")
            put("ironing_flowrate", "ironing_flow")
            put("spiral_vase", "spiral_mode")
            put("support_material_speed", "support_speed")
            put("support_material_interface_speed", "support_interface_speed")
            put("first_layer_speed", "initial_layer_speed")
            put("first_layer_acceleration", "initial_layer_acceleration")
            put("overhang_speed_0", "overhang_1_4_speed")
            put("overhang_speed_1", "overhang_2_4_speed")
            put("overhang_speed_2", "overhang_3_4_speed")
            put("overhang_speed_3", "overhang_4_4_speed")
            put("max_volumetric_extrusion_rate_slope_positive", "max_volumetric_extrusion_rate_slope")
            put("support_material", "enable_support")
            put("support_material_threshold", "support_threshold_angle")
            put("support_material_enforce_layers", "enforce_support_layers")
            put("support_material_style", "support_style")
            put("support_material_pattern", "support_base_pattern")
            put("support_material_spacing", "support_base_pattern_spacing")
            put("support_material_angle", "support_angle")
            put("support_material_contact_distance", "support_top_z_distance")
            put("support_material_bottom_contact_distance", "support_bottom_z_distance")
            put("support_material_buildplate_only", "support_on_build_plate_only")
            put("support_material_xy_spacing", "support_object_xy_distance")
            put("support_material_extruder", "support_filament")
            put("support_material_interface_extruder", "support_interface_filament")
            put("support_material_interface_layers", "support_interface_top_layers")
            put("support_material_bottom_interface_layers", "support_interface_bottom_layers")
            put("support_material_interface_pattern", "support_interface_pattern")
            put("support_material_interface_spacing", "support_interface_spacing")
            put("support_material_interface_contact_loops", "support_interface_loop_pattern")
            put("support_material_extrusion_width", "support_line_width")
            put("dont_support_bridges", "bridge_no_support")
            put("support_tree_angle", "tree_support_branch_angle")
            put("support_tree_angle_slow", "tree_support_angle_slow")
            put("support_tree_branch_diameter", "tree_support_branch_diameter")
            put("support_tree_branch_diameter_angle", "tree_support_branch_diameter_angle")
            put("support_tree_branch_distance", "tree_support_branch_distance")
            put("support_tree_tip_diameter", "tree_support_tip_diameter")
            put("support_tree_top_rate", "tree_support_top_rate")
            put("skirts", "skirt_loops")
            put("brim_separation", "brim_object_gap")
            put("wipe_tower", "enable_prime_tower")
            put("wipe_tower_width", "prime_tower_width")
            put("wipe_tower_brim_width", "prime_tower_brim_width")
            put("wipe_tower_extruder", "wipe_tower_filament")
            put("extruder_clearance_height", "extruder_clearance_height_to_rod")
            put("fuzzy_skin_point_dist", "fuzzy_skin_point_distance")
            put("output_filename_format", "filename_format")
            put("xy_size_compensation", "xy_contour_compensation")
            put("machine_max_feedrate_x", "machine_max_speed_x")
            put("machine_max_feedrate_y", "machine_max_speed_y")
            put("machine_max_feedrate_z", "machine_max_speed_z")
            put("machine_max_feedrate_e", "machine_max_speed_e")
        }

        private fun buildReverse(): Map<String, String> {
            val reverse = HashMap<String, String>()
            KEY_MIGRATION.forEach { (legacy, current) -> reverse[current] = legacy }
            return reverse
        }

        /** Legacy process keys that must be recognized when importing profile bundles. */
        @JvmStatic
        fun printConfigKeyAliases(): List<String> = PRINT_CONFIG_KEY_ALIASES

        /** Resolve a config key to its OrcaSlicer engine name. */
        @JvmStatic
        fun migrateKey(key: String): String {
            val mapped = KEY_MIGRATION[key] ?: key
            return ORCA_KEY_ALIASES[mapped] ?: mapped
        }

        /** Convert legacy feature-filament values to Orca's zero-means-inherit convention. */
        private fun migrateValue(key: String, value: String?): String? {
            if (value == null) return null
            val normalizedKey = KEY_MIGRATION[key] ?: key
            if (ORCA_KEY_ALIASES.containsKey(normalizedKey)) {
                if (normalizedKey == "top_one_wall_type") return if (value == "none") "0" else "1"
                if (value == "1") return "0"
            }
            return value
        }

        /** Resolve an OrcaSlicer key back to its legacy name. */
        @JvmStatic
        fun legacyKey(key: String): String = KEY_MIGRATION_REVERSE[key] ?: key

        @JvmStatic
        fun normalizeSerializedValue(key: String, value: String?): String? {
            if (value == null) return null
            if (key == "before_layer_change_gcode" && value.contains("G92 E0")) {
                val cleaned = StringBuilder()
                val lines = Pattern.compile("\n").split(value, -1)
                for (line in lines) {
                    if (!line.contains("G92 E0")) {
                        if (cleaned.isNotEmpty()) cleaned.append('\n')
                        cleaned.append(line)
                    }
                }
                return cleaned.toString()
            }
            if (key.endsWith("_gcode")) {
                return value
                    .replace("[bed_temperature_initial_layer_single]", "{first_layer_bed_temperature[0]}")
                    .replace("[bed_temperature_initial_layer]", "{first_layer_bed_temperature[0]}")
                    .replace("[nozzle_temperature_initial_layer]", "{first_layer_temperature[0]}")
            }
            return value
        }

        @JvmStatic
        fun createCustomPrinterProfile(): ConfigObject {
            val custom = ConfigObject(SliceBeam.requireInstance().getString(R.string.IntroCustomProfileName))
            custom.profileListType = PROFILE_LIST_PRINTER
            custom.put("printer_technology", "FFF")
            custom.put("bed_shape", "0x0,200x0,200x200,0x200")
            custom.put("gcode_flavor", "marlin")
            custom.put("max_print_height", "200")
            custom.put("min_layer_height", "0.15")
            custom.put("max_layer_height", "0.30")
            custom.put("layer_height", "0.2")
            custom.put("nozzle_diameter", "0.4")
            custom.put("z_offset", "0")
            custom.put("retract_length", "0.5")
            custom.put("retract_speed", "30")
            custom.put("deretract_speed", "30")
            custom.put("retract_before_travel", "2")

            custom.put("emit_machine_limits_to_gcode", "0")
            custom.put("machine_max_acceleration_e", "5000")
            custom.put("machine_max_acceleration_extruding", "500")
            custom.put("machine_max_acceleration_retracting", "1000")
            custom.put("machine_max_acceleration_travel", "500")
            custom.put("machine_max_acceleration_x", "500")
            custom.put("machine_max_acceleration_y", "500")
            custom.put("machine_max_acceleration_z", "100")
            custom.put("machine_max_feedrate_e", "60")
            custom.put("machine_max_feedrate_x", "500")
            custom.put("machine_max_feedrate_y", "500")
            custom.put("machine_max_feedrate_z", "10")
            custom.put("machine_max_jerk_e", "5")
            custom.put("machine_max_jerk_x", "8")
            custom.put("machine_max_jerk_y", "8")
            custom.put("machine_max_jerk_z", "0.4")
            custom.put("machine_min_extruding_rate", "0")
            custom.put("machine_min_travel_rate", "0")

            custom.put("start_gcode", "G90 ; use absolute coordinates\\nM83 ; extruder relative mode\\nM104 S{is_nil(idle_temperature[0]) ? 150 : idle_temperature[0]} ; set temporary nozzle temp to prevent oozing during homing\\nM140 S{first_layer_bed_temperature[0]} ; set final bed temp\\nG4 S30 ; allow partial nozzle warmup\\nG28 ; home all axis\\nG1 Z50 F240\\nG1 X2.0 Y10 F3000\\nM104 S{first_layer_temperature[0]} ; set final nozzle temp\\nM190 S{first_layer_bed_temperature[0]} ; wait for bed temp to stabilize\\nM109 S{first_layer_temperature[0]} ; wait for nozzle temp to stabilize\\nG1 Z0.28 F240\\nG92 E0\\nG1 X2.0 Y140 E10 F1500 ; prime the nozzle\\nG1 X2.3 Y140 F5000\\nG92 E0\\nG1 X2.3 Y10 E10 F1200 ; prime the nozzle\\nG92 E0")
            custom.put("end_gcode", "{if max_layer_z < max_print_height}G1 Z{z_offset+min(max_layer_z+2, max_print_height)} F600 ; Move print head up{endif}\\nG1 X5 Y{print_bed_max[1]*0.85} F{travel_speed*60} ; present print\\n{if max_layer_z < max_print_height-10}G1 Z{z_offset+min(max_layer_z+70, max_print_height-10)} F600 ; Move print head further up{endif}\\n{if max_layer_z < max_print_height*0.6}G1 Z{max_print_height*0.6} F600 ; Move print head further up{endif}\\nM140 S0 ; turn off heatbed\\nM104 S0 ; turn off temperature\\nM107 ; turn off fan\\nM84 X Y E ; disable motors")
            return custom
        }

        @JvmStatic
        fun createCustomFilamentProfile(): ConfigObject {
            val genericFilament = ConfigObject(
                SliceBeam.requireInstance().getString(R.string.IntroCustomProfileFilamentName)
            )
            genericFilament.profileListType = PROFILE_LIST_FILAMENT
            genericFilament.put("first_layer_bed_temperature", "60")
            genericFilament.put("bed_temperature", "60")
            genericFilament.put("first_layer_temperature", "210")
            genericFilament.put("temperature", "210")
            genericFilament.put("filament_type", "PLA")
            genericFilament.put("slowdown_below_layer_time", "8")
            genericFilament.put("cooling", "1")
            genericFilament.put("fan_always_on", "1")
            genericFilament.put("fan_below_layer_time", "20")
            genericFilament.put("idle_temperature", "150")
            return genericFilament
        }
    }

    @JvmField
    var values: MutableMap<String, String> = HashMap()

    @JvmField
    var thumbnailUrl: String? = null

    @JvmField
    var profileListType: Int = 0

    private var profileTitle: String? = null
    open override val title: String? get() = profileTitle
    open fun setTitle(title: String?) { profileTitle = title }

    constructor() : super()

    constructor(title: String?) : this() {
        profileTitle = title
    }

    constructor(from: ConfigObject) : this(from.title) {
        values.putAll(from.values)
        thumbnailUrl = from.thumbnailUrl
        profileListType = from.profileListType
    }

    /** Suitable only for printer profiles. */
    open val extruderCount: Int
        get() = get("nozzle_diameter")?.replace(Regex("[^.]+"), "")?.length ?: 1

    open fun has(key: String): Boolean = values.containsKey(migrateKey(key))

    open operator fun get(key: String): String? = values[migrateKey(key)]

    open fun remove(key: String) {
        values.remove(migrateKey(key))
    }

    open fun put(key: String, value: String?) {
        val normalized = migrateValue(key, value)
        if (normalized != null) {
            values[migrateKey(key)] = normalized
        } else {
            @Suppress("UNCHECKED_CAST")
            (values as MutableMap<String, String?>)[migrateKey(key)] = null
        }
    }

    override fun isSelected(): Boolean =
        when (profileListType) {
            PROFILE_LIST_PRINT -> title!!.equals(SliceBeam.requireConfig().presets!!["print"])
            PROFILE_LIST_FILAMENT -> title!!.equals(SliceBeam.requireConfig().presets!!["filament"])
            PROFILE_LIST_PRINTER -> title!!.equals(SliceBeam.requireConfig().presets!!["printer"])
            else -> false
        }

    open fun serialize(): String = buildString {
        append("# generated by Slice Beam ").append(BuildConfig.VERSION_NAME).append("\n\n")
        for ((key, rawValue) in values) {
            val value = normalizeSerializedValue(key, rawValue) ?: continue
            append(key).append(" = ").append(value.replace("\n", "\\n")).append('\n')
        }
    }
}
