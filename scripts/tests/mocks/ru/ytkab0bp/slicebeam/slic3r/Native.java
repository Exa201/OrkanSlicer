package ru.ytkab0bp.slicebeam.slic3r;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import java.util.*;

public class Native {
    public static final Companion Companion = new Companion();
    public static final class Companion {
        public void get_print_config_def(ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef arg0) { Native.get_print_config_def(arg0); }
        public void set_svg_path_prefix(java.lang.String arg0) { Native.set_svg_path_prefix(arg0); }
        public long shader_init_from_texts(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2) { return Native.shader_init_from_texts(arg0, arg1, arg2); }
        public int shader_get_id(long arg0) { return Native.shader_get_id(arg0); }
        public int shader_get_uniform_location(long arg0, java.lang.String arg1) { return Native.shader_get_uniform_location(arg0, arg1); }
        public int shader_get_attrib_location(long arg0, java.lang.String arg1) { return Native.shader_get_attrib_location(arg0, arg1); }
        public void shader_start_using(long arg0) { Native.shader_start_using(arg0); }
        public void shader_stop_using(long arg0) { Native.shader_stop_using(arg0); }
        public void shader_release(long arg0) { Native.shader_release(arg0); }
        public long glmodel_create() { return Native.glmodel_create(); }
        public void glmodel_init_from_model(long arg0, long arg1) { Native.glmodel_init_from_model(arg0, arg1); }
        public void glmodel_init_from_model_object(long arg0, long arg1, int arg2) { Native.glmodel_init_from_model_object(arg0, arg1, arg2); }
        public void glmodel_init_raycast_data(long arg0) { Native.glmodel_init_raycast_data(arg0); }
        public void glmodel_set_color(long arg0, float arg1, float arg2, float arg3, float arg4) { Native.glmodel_set_color(arg0, arg1, arg2, arg3, arg4); }
        public void glmodel_render(long arg0) { Native.glmodel_render(arg0); }
        public void glmodel_stilized_arrow(long arg0, float arg1, float arg2, float arg3, float arg4) { Native.glmodel_stilized_arrow(arg0, arg1, arg2, arg3, arg4); }
        public void glmodel_init_background_triangles(long arg0) { Native.glmodel_init_background_triangles(arg0); }
        public void glmodel_init_box(long arg0, float arg1, float arg2, float arg3) { Native.glmodel_init_box(arg0, arg1, arg2, arg3); }
        public void glmodel_init_bounding_box(long arg0, long arg1, int arg2) { Native.glmodel_init_bounding_box(arg0, arg1, arg2); }
        public boolean glmodel_is_initialized(long arg0) { return Native.glmodel_is_initialized(arg0); }
        public boolean glmodel_is_empty(long arg0) { return Native.glmodel_is_empty(arg0); }
        public double[] glmodel_raycast_closest_hit(long arg0, double[] arg1, double[] arg2) { return Native.glmodel_raycast_closest_hit(arg0, arg1, arg2); }
        public void glmodel_reset(long arg0) { Native.glmodel_reset(arg0); }
        public void glmodel_release(long arg0) { Native.glmodel_release(arg0); }
        public long bed_create(long[] arg0) { return Native.bed_create(arg0); }
        public int bed_get_bounding_volume_max_size(long arg0) { return Native.bed_get_bounding_volume_max_size(arg0); }
        public double[] bed_get_bounding_volume(long arg0) { return Native.bed_get_bounding_volume(arg0); }
        public void bed_configure(long arg0, java.lang.String arg1) { Native.bed_configure(arg0, arg1); }
        public void bed_init_triangles_mesh(long arg0, long arg1) { Native.bed_init_triangles_mesh(arg0, arg1); }
        public boolean bed_arrange(long arg0, long arg1) { return Native.bed_arrange(arg0, arg1); }
        public void bed_release(long arg0) { Native.bed_release(arg0); }
        public long models_merge(long[] arg0) { return Native.models_merge(arg0); }
        public long model_create() { return Native.model_create(); }
        public int model_get_objects_count(long arg0) { return Native.model_get_objects_count(arg0); }
        public void model_add_object_from_another(long arg0, long arg1, int arg2) { Native.model_add_object_from_another(arg0, arg1, arg2); }
        public void model_delete_object(long arg0, int arg1) { Native.model_delete_object(arg0, arg1); }
        public double[] model_get_translation(long arg0, int arg1) { return Native.model_get_translation(arg0, arg1); }
        public double[] model_get_scale(long arg0, int arg1) { return Native.model_get_scale(arg0, arg1); }
        public double[] model_get_mirror(long arg0, int arg1) { return Native.model_get_mirror(arg0, arg1); }
        public double[] model_get_rotation(long arg0, int arg1) { return Native.model_get_rotation(arg0, arg1); }
        public double[] model_get_bounding_box_exact(long arg0, int arg1) { return Native.model_get_bounding_box_exact(arg0, arg1); }
        public double[] model_get_bounding_box_approx(long arg0, int arg1) { return Native.model_get_bounding_box_approx(arg0, arg1); }
        public double[] model_get_bounding_box_exact_global(long arg0) { return Native.model_get_bounding_box_exact_global(arg0); }
        public double[] model_get_bounding_box_approx_global(long arg0) { return Native.model_get_bounding_box_approx_global(arg0); }
        public boolean model_is_left_handed(long arg0, int arg1) { return Native.model_is_left_handed(arg0, arg1); }
        public void model_translate(long arg0, int arg1, double arg2, double arg3, double arg4) { Native.model_translate(arg0, arg1, arg2, arg3, arg4); }
        public void model_translate_global(long arg0, double arg1, double arg2, double arg3) { Native.model_translate_global(arg0, arg1, arg2, arg3); }
        public void model_ensure_on_bed(long arg0, int arg1) { Native.model_ensure_on_bed(arg0, arg1); }
        public void model_scale(long arg0, int arg1, double arg2, double arg3, double arg4) { Native.model_scale(arg0, arg1, arg2, arg3, arg4); }
        public void model_rotate(long arg0, int arg1, double arg2, double arg3, double arg4) { Native.model_rotate(arg0, arg1, arg2, arg3, arg4); }
        public void model_flatten_rotate(long arg0, int arg1, long arg2) { Native.model_flatten_rotate(arg0, arg1, arg2); }
        public long[] model_create_flatten_planes(long arg0, int arg1) { return Native.model_create_flatten_planes(arg0, arg1); }
        public void model_auto_orient(long arg0, int arg1) { Native.model_auto_orient(arg0, arg1); }
        public boolean model_is_big_object(long arg0, int arg1) { return Native.model_is_big_object(arg0, arg1); }
        public int model_get_extruder(long arg0, int arg1) { return Native.model_get_extruder(arg0, arg1); }
        public void model_set_extruder(long arg0, int arg1, int arg2) { Native.model_set_extruder(arg0, arg1, arg2); }
        public long paint_begin(long arg0, int arg1, int arg2) { return Native.paint_begin(arg0, arg1, arg2); }
        public double[] paint_raycast(long arg0, double[] arg1, double[] arg2) { return Native.paint_raycast(arg0, arg1, arg2); }
        public void paint_brush(long arg0, double[] arg1, int arg2, double arg3, int arg4, double[] arg5, boolean arg6) { Native.paint_brush(arg0, arg1, arg2, arg3, arg4, arg5, arg6); }
        public void paint_bucket(long arg0, double[] arg1, int arg2, int arg3, boolean arg4, double arg5) { Native.paint_bucket(arg0, arg1, arg2, arg3, arg4, arg5); }
        public void paint_height(long arg0, double arg1, double arg2, int arg3) { Native.paint_height(arg0, arg1, arg2, arg3); }
        public void paint_clear(long arg0) { Native.paint_clear(arg0); }
        public void paint_commit(long arg0) { Native.paint_commit(arg0); }
        public int glmodel_init_from_paint(long arg0, long arg1, int arg2) { return Native.glmodel_init_from_paint(arg0, arg1, arg2); }
        public void paint_end(long arg0) { Native.paint_end(arg0); }
        public int model_paint_max_filament(long arg0, int arg1) { return Native.model_paint_max_filament(arg0, arg1); }
        public int[] model_get_used_filaments(long arg0, int arg1) { return Native.model_get_used_filaments(arg0, arg1); }
        public long model_slice(long arg0, java.lang.String arg1, java.lang.String arg2, ru.ytkab0bp.slicebeam.slic3r.SliceListener arg3, int arg4, int[] arg5, int arg6, double arg7, double arg8, double arg9) throws ru.ytkab0bp.slicebeam.slic3r.Slic3rRuntimeError { return Native.model_slice(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9); }
        public void model_export_3mf(long arg0, java.lang.String arg1, java.lang.String arg2) throws ru.ytkab0bp.slicebeam.slic3r.Slic3rRuntimeError { Native.model_export_3mf(arg0, arg1, arg2); }
        public void model_release(long arg0) { Native.model_release(arg0); }
        public long gcoderesult_load_file(java.lang.String arg0, java.lang.String arg1) { return Native.gcoderesult_load_file(arg0, arg1); }
        public java.lang.String gcoderesult_get_recommended_name(long arg0) { return Native.gcoderesult_get_recommended_name(arg0); }
        public double gcoderesult_get_used_filament_mm(long arg0, int arg1) { return Native.gcoderesult_get_used_filament_mm(arg0, arg1); }
        public double gcoderesult_get_used_filament_g(long arg0, int arg1) { return Native.gcoderesult_get_used_filament_g(arg0, arg1); }
        public void gcoderesult_release(long arg0) { Native.gcoderesult_release(arg0); }
        public long vgcode_create() { return Native.vgcode_create(); }
        public void vgcode_init(long arg0) { Native.vgcode_init(arg0); }
        public boolean vgcode_is_initialized(long arg0) { return Native.vgcode_is_initialized(arg0); }
        public void vgcode_set_colors(long arg0, int[] arg1) { Native.vgcode_set_colors(arg0, arg1); }
        public long vgcode_get_layers_count(long arg0) { return Native.vgcode_get_layers_count(arg0); }
        public void vgcode_load(long arg0, long arg1) { Native.vgcode_load(arg0, arg1); }
        public void vgcode_render(long arg0, float[] arg1, float[] arg2) { Native.vgcode_render(arg0, arg1, arg2); }
        public void vgcode_set_layers_view_range(long arg0, long arg1, long arg2) { Native.vgcode_set_layers_view_range(arg0, arg1, arg2); }
        public long[] vgcode_get_layers_view_range(long arg0) { return Native.vgcode_get_layers_view_range(arg0); }
        public float vgcode_get_estimated_time(long arg0) { return Native.vgcode_get_estimated_time(arg0); }
        public float vgcode_get_estimated_time_role(long arg0, int arg1) { return Native.vgcode_get_estimated_time_role(arg0, arg1); }
        public boolean vgcode_is_extrusion_role_visible(long arg0, int arg1) { return Native.vgcode_is_extrusion_role_visible(arg0, arg1); }
        public void vgcode_toggle_extrusion_role_visibility(long arg0, int arg1) { Native.vgcode_toggle_extrusion_role_visibility(arg0, arg1); }
        public void vgcode_reset(long arg0) { Native.vgcode_reset(arg0); }
        public void vgcode_release(long arg0) { Native.vgcode_release(arg0); }
        public long utils_config_create(java.lang.String arg0) { return Native.utils_config_create(arg0); }
        public boolean utils_config_check_compatibility(long arg0, java.lang.String arg1) { return Native.utils_config_check_compatibility(arg0, arg1); }
        public java.lang.String utils_config_eval(long arg0, java.lang.String arg1) throws ru.ytkab0bp.slicebeam.slic3r.Slic3rRuntimeError { return Native.utils_config_eval(arg0, arg1); }
        public void utils_config_release(long arg0) { Native.utils_config_release(arg0); }
        public void utils_calc_view_normal_matrix(double[] arg0, double[] arg1, double[] arg2) { Native.utils_calc_view_normal_matrix(arg0, arg1, arg2); }
        public double[] utils_unproject(double[] arg0, double[] arg1, int arg2, int arg3, double arg4, double arg5) { return Native.utils_unproject(arg0, arg1, arg2, arg3, arg4, arg5); }
        public java.lang.String orca_bundle_read(java.lang.String arg0, java.lang.String arg1) throws java.io.IOException, org.json.JSONException { return Native.orca_bundle_read(arg0, arg1); }
    }

    static {
        // Shadow class: No System.loadLibrary call!
    }

    // Mock State Management
    private static long nextPtr = 1;
    
    public static class MockModelObject {
        public double[] translation = {0.0, 0.0, 0.0};
        public double[] rotation = {0.0, 0.0, 0.0};
        public double[] scale = {1.0, 1.0, 1.0};
        public double[] mirror = {1.0, 1.0, 1.0};
        public double[] boundingBox = {-50.0, -50.0, 0.0, 50.0, 50.0, 100.0}; // xmin, ymin, zmin, xmax, ymax, zmax
        public int extruder = 0;
        public int colorIndex = 0;
        public boolean hasSupport = false;

        public MockModelObject clone() {
            MockModelObject o = new MockModelObject();
            o.translation = translation.clone();
            o.rotation = rotation.clone();
            o.scale = scale.clone();
            o.mirror = mirror.clone();
            o.boundingBox = boundingBox.clone();
            o.extruder = extruder;
            o.colorIndex = colorIndex;
            o.hasSupport = hasSupport;
            return o;
        }
    }

    public static class MockModel {
        public List<MockModelObject> objects = new ArrayList<>();
        public MockModel() {
            // Start with one object by default
            objects.add(new MockModelObject());
        }
    }

    public static class MockBed {
        public long[] data;
        public String configPath;
        public boolean isValid = true;
    }

    private static Map<Long, MockModel> models = new HashMap<>();
    private static Map<Long, MockBed> beds = new HashMap<>();
    private static String lastSliceConfig;
    private static final List<String> nativeLifecycleEvents = new ArrayList<>();
    private static String lastGCodePath;
    private static String lastGCodeName;
    private static int lastGCodeMMRole = -1;
    private static int lastGCodeGRole = -1;
    private static int gCodeReleaseCalls;
    private static long lastGCodeReleasedPtr;
    private static int bedReleaseCalls;
    private static long lastBedReleasedPtr;
    private static int glModelReleaseCalls;
    private static long lastGlModelReleasedPtr;
    private static long nextPaintSessionPtr = 9000L;
    private static long lastPaintModelPtr;
    private static long lastPaintEndedPtr;
    private static int lastPaintObjectIndex;
    private static int lastPaintMode;
    private static int lastPaintFacetStart;
    private static int lastPaintFilament;
    private static double lastPaintRadius;
    private static double lastPaintAngle;
    private static double lastPaintZMin;
    private static double lastPaintZMax;
    private static boolean lastPaintSphere;
    private static boolean lastPaintPropagate;
    private static int paintEndCalls;

    public static Map<Long, MockModel> getMockModels() { return models; }
    public static Map<Long, MockBed> getMockBeds() { return beds; }
    public static String getLastSliceConfig() { return lastSliceConfig; }
    public static List<String> getNativeLifecycleEvents() { return new ArrayList<>(nativeLifecycleEvents); }
    public static String getLastGCodePath() { return lastGCodePath; }
    public static String getLastGCodeName() { return lastGCodeName; }
    public static int getLastGCodeMMRole() { return lastGCodeMMRole; }
    public static int getLastGCodeGRole() { return lastGCodeGRole; }
    public static int getGCodeReleaseCalls() { return gCodeReleaseCalls; }
    public static long getLastGCodeReleasedPtr() { return lastGCodeReleasedPtr; }
    public static int getBedReleaseCalls() { return bedReleaseCalls; }
    public static long getLastBedReleasedPtr() { return lastBedReleasedPtr; }
    public static int getGlModelReleaseCalls() { return glModelReleaseCalls; }
    public static long getLastGlModelReleasedPtr() { return lastGlModelReleasedPtr; }
    public static long getLastPaintModelPtr() { return lastPaintModelPtr; }
    public static long getLastPaintEndedPtr() { return lastPaintEndedPtr; }
    public static int getLastPaintObjectIndex() { return lastPaintObjectIndex; }
    public static int getLastPaintMode() { return lastPaintMode; }
    public static int getLastPaintFacetStart() { return lastPaintFacetStart; }
    public static int getLastPaintFilament() { return lastPaintFilament; }
    public static double getLastPaintRadius() { return lastPaintRadius; }
    public static double getLastPaintAngle() { return lastPaintAngle; }
    public static double getLastPaintZMin() { return lastPaintZMin; }
    public static double getLastPaintZMax() { return lastPaintZMax; }
    public static boolean getLastPaintSphere() { return lastPaintSphere; }
    public static boolean getLastPaintPropagate() { return lastPaintPropagate; }
    public static int getPaintEndCalls() { return paintEndCalls; }
    public static void setNextPaintSessionPtr(long pointer) { nextPaintSessionPtr = pointer; }

    public static void resetMockState() {
        nextPtr = 1;
        models.clear();
        beds.clear();
        lastSliceConfig = null;
        nativeLifecycleEvents.clear();
        lastGCodePath = null;
        lastGCodeName = null;
        lastGCodeMMRole = -1;
        lastGCodeGRole = -1;
        gCodeReleaseCalls = 0;
        lastGCodeReleasedPtr = 0L;
        bedReleaseCalls = 0;
        lastBedReleasedPtr = 0L;
        glModelReleaseCalls = 0;
        lastGlModelReleasedPtr = 0L;
        nextPaintSessionPtr = 9000L;
        lastPaintModelPtr = 0L;
        lastPaintEndedPtr = 0L;
        lastPaintObjectIndex = 0;
        lastPaintMode = 0;
        lastPaintFacetStart = 0;
        lastPaintFilament = 0;
        lastPaintRadius = 0.0;
        lastPaintAngle = 0.0;
        lastPaintZMin = 0.0;
        lastPaintZMax = 0.0;
        lastPaintSphere = false;
        lastPaintPropagate = false;
        paintEndCalls = 0;
    }

    public static void set_svg_path_prefix(String prefix) {}

    public static void get_print_config_def(PrintConfigDef def) {
        // Mock print config definition
        ConfigOptionDef brimTypeOpt = new ConfigOptionDef();
        brimTypeOpt.key = "brim_type";
        brimTypeOpt.defaultValue = "auto_brim";
        brimTypeOpt.enumValues = new String[]{"auto_brim", "outer_only", "no_brim"};
        def.options.put("brim_type", brimTypeOpt);
        for (String key : Arrays.asList(
                "top_shell_layers", "bottom_shell_layers", "spiral_mode", "bridge_flow", "bridge_speed")) {
            ConfigOptionDef option = new ConfigOptionDef();
            option.key = key;
            def.options.put(key, option);
        }
    }

    public static long bed_create(long[] data) {
        long ptr = nextPtr++;
        for (int i = 0; i < data.length; i++) data[i] = nextPtr++;
        MockBed bed = new MockBed();
        bed.data = data;
        beds.put(ptr, bed);
        return ptr;
    }

    public static int bed_get_bounding_volume_max_size(long ptr) { return 250; }
    public static double[] bed_get_bounding_volume(long ptr) { return new double[]{-125, -125, 0, 125, 125, 200}; }
    public static void bed_configure(long ptr, String configPath) {
        MockBed bed = beds.get(ptr);
        if (bed != null) {
            bed.configPath = configPath;
        }
    }
    public static void bed_init_triangles_mesh(long ptr, long triangles) {}
    public static boolean bed_arrange(long ptr, long modelPtr) {
        MockModel model = models.get(modelPtr);
        if (model != null) {
            // Simulate placing them nicely
            for (int i = 0; i < model.objects.size(); i++) {
                model.objects.get(i).translation[0] = i * 20.0;
                model.objects.get(i).translation[1] = i * 20.0;
            }
            return true;
        }
        return false;
    }
    public static void bed_release(long ptr) {
        beds.remove(ptr);
        bedReleaseCalls++;
        lastBedReleasedPtr = ptr;
    }

    public static long model_create() {
        long ptr = nextPtr++;
        models.put(ptr, new MockModel());
        return ptr;
    }

    public static long models_merge(long... ptrs) {
        long ptr = nextPtr++;
        MockModel merged = new MockModel();
        merged.objects.clear();
        for (long p : ptrs) {
            MockModel m = models.get(p);
            if (m != null) {
                for (MockModelObject o : m.objects) {
                    merged.objects.add(o.clone());
                }
            }
        }
        models.put(ptr, merged);
        return ptr;
    }

    public static long model_read_from_file(String path, String baseName) throws Slic3rRuntimeError {
        long ptr = nextPtr++;
        MockModel model = new MockModel();
        models.put(ptr, model);
        return ptr;
    }

    public static int model_get_objects_count(long ptr) {
        MockModel model = models.get(ptr);
        return model != null ? model.objects.size() : 0;
    }

    public static int model_paint_max_filament(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model == null || objectIndex < 0 || objectIndex >= model.objects.size()) return 0;
        return Math.max(0, model.objects.get(objectIndex).colorIndex);
    }

    public static int[] model_get_used_filaments(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model == null || objectIndex < 0 || objectIndex >= model.objects.size()) return new int[0];
        MockModelObject object = model.objects.get(objectIndex);
        int base = Math.max(1, object.extruder);
        if (object.colorIndex > 0 && object.colorIndex != base) {
            return new int[] { Math.min(base, object.colorIndex), Math.max(base, object.colorIndex) };
        }
        return new int[] { base };
    }

    public static void model_add_object_from_another(long ptr, long from, int i) {
        MockModel toModel = models.get(ptr);
        MockModel fromModel = models.get(from);
        if (toModel != null && fromModel != null && i >= 0 && i < fromModel.objects.size()) {
            toModel.objects.add(fromModel.objects.get(i).clone());
        }
    }

    public static void model_delete_object(long ptr, int i) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            model.objects.remove(i);
        }
    }

    public static double[] model_get_translation(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model != null && objectIndex >= 0 && objectIndex < model.objects.size()) {
            return model.objects.get(objectIndex).translation;
        }
        return new double[]{0, 0, 0};
    }

    public static double[] model_get_scale(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model != null && objectIndex >= 0 && objectIndex < model.objects.size()) {
            return model.objects.get(objectIndex).scale;
        }
        return new double[]{1, 1, 1};
    }

    public static double[] model_get_mirror(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model != null && objectIndex >= 0 && objectIndex < model.objects.size()) {
            return model.objects.get(objectIndex).mirror;
        }
        return new double[]{1, 1, 1};
    }

    public static double[] model_get_rotation(long ptr, int objectIndex) {
        MockModel model = models.get(ptr);
        if (model != null && objectIndex >= 0 && objectIndex < model.objects.size()) {
            return model.objects.get(objectIndex).rotation;
        }
        return new double[]{0, 0, 0};
    }

    public static double[] model_get_bounding_box_exact(long ptr, int i) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            return model.objects.get(i).boundingBox;
        }
        return new double[]{-50, -50, 0, 50, 50, 100};
    }

    public static double[] model_get_bounding_box_approx(long ptr, int i) {
        return model_get_bounding_box_exact(ptr, i);
    }

    public static double[] model_get_bounding_box_exact_global(long ptr) {
        MockModel model = models.get(ptr);
        if (model == null || model.objects.isEmpty()) return new double[]{0, 0, 0, 0, 0, 0};
        double xmin = Double.MAX_VALUE, ymin = Double.MAX_VALUE, zmin = Double.MAX_VALUE;
        double xmax = -Double.MAX_VALUE, ymax = -Double.MAX_VALUE, zmax = -Double.MAX_VALUE;
        for (MockModelObject o : model.objects) {
            xmin = Math.min(xmin, o.boundingBox[0] + o.translation[0]);
            ymin = Math.min(ymin, o.boundingBox[1] + o.translation[1]);
            zmin = Math.min(zmin, o.boundingBox[2] + o.translation[2]);
            xmax = Math.max(xmax, o.boundingBox[3] + o.translation[0]);
            ymax = Math.max(ymax, o.boundingBox[4] + o.translation[1]);
            zmax = Math.max(zmax, o.boundingBox[5] + o.translation[2]);
        }
        return new double[]{xmin, ymin, zmin, xmax, ymax, zmax};
    }

    public static double[] model_get_bounding_box_approx_global(long ptr) {
        return model_get_bounding_box_exact_global(ptr);
    }

    public static boolean model_is_left_handed(long ptr, int i) { return false; }

    public static void model_translate(long ptr, int i, double x, double y, double z) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            model.objects.get(i).translation[0] += x;
            model.objects.get(i).translation[1] += y;
            model.objects.get(i).translation[2] += z;
        }
    }

    public static void model_translate_global(long ptr, double x, double y, double z) {
        MockModel model = models.get(ptr);
        if (model != null) {
            for (MockModelObject o : model.objects) {
                o.translation[0] += x;
                o.translation[1] += y;
                o.translation[2] += z;
            }
        }
    }

    public static void model_ensure_on_bed(long ptr, int i) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            // align z_min to 0
            model.objects.get(i).translation[2] = -model.objects.get(i).boundingBox[2];
        }
    }

    public static void model_scale(long ptr, int i, double x, double y, double z) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            model.objects.get(i).scale[0] *= x;
            model.objects.get(i).scale[1] *= y;
            model.objects.get(i).scale[2] *= z;
        }
    }

    public static void model_rotate(long ptr, int i, double x, double y, double z) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            model.objects.get(i).rotation[0] += x;
            model.objects.get(i).rotation[1] += y;
            model.objects.get(i).rotation[2] += z;
        }
    }

    public static void model_flatten_rotate(long ptr, int i, long surfacePtr) {}
    public static long[] model_create_flatten_planes(long ptr, int i) { return new long[0]; }
    public static void model_auto_orient(long ptr, int i) {}
    public static boolean model_is_big_object(long ptr, int i) { return false; }
    public static int model_get_extruder(long ptr, int i) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            return model.objects.get(i).extruder;
        }
        return 0;
    }
    public static void model_set_extruder(long ptr, int i, int extruder) {
        MockModel model = models.get(ptr);
        if (model != null && i >= 0 && i < model.objects.size()) {
            model.objects.get(i).extruder = extruder;
        }
    }

    public static long model_slice(long ptr, String configPath, String path, SliceListener listener,
                                   int numFilaments, int[] filamentColors, int calibMode,
                                   double calibStart, double calibEnd, double calibStep) throws Slic3rRuntimeError {
        try {
            lastSliceConfig = java.nio.file.Files.readString(java.nio.file.Paths.get(configPath));
        } catch (IOException error) {
            throw new RuntimeException("Could not read config handed to native slicer", error);
        }
        // Notify listener to simulate real slicing progress
        if (listener != null) {
            listener.onProgress(10, "Preparing model");
            listener.onProgress(50, "Slicing model");
            listener.onProgress(100, "Finished");
        }
        return 5000L; // Mock gcoderesult pointer
    }

    public static void model_export_3mf(long ptr, String configPath, String path) throws Slic3rRuntimeError {}
    public static void model_release(long ptr) { models.remove(ptr); }

    public static long gcoderesult_load_file(String path, String name) {
        lastGCodePath = path;
        lastGCodeName = name;
        nativeLifecycleEvents.add("gcoderesult_load_file");
        return 5000L;
    }
    public static String gcoderesult_get_recommended_name(long ptr) {
        nativeLifecycleEvents.add("gcoderesult_get_recommended_name");
        return "mock_slice.gcode";
    }
    public static double gcoderesult_get_used_filament_mm(long ptr, int role) {
        lastGCodeMMRole = role;
        nativeLifecycleEvents.add("gcoderesult_get_used_filament_mm");
        return 1500.0;
    }
    public static double gcoderesult_get_used_filament_g(long ptr, int role) {
        lastGCodeGRole = role;
        nativeLifecycleEvents.add("gcoderesult_get_used_filament_g");
        return 4.5;
    }
    public static void gcoderesult_release(long ptr) {
        gCodeReleaseCalls++;
        lastGCodeReleasedPtr = ptr;
        nativeLifecycleEvents.add("gcoderesult_release");
    }

    // Painting-session native bridge mocks used by Java/Kotlin ABI and resource-lifecycle tests.
    public static long paint_begin(long modelPtr, int objIdx, int mode) {
        lastPaintModelPtr = modelPtr;
        lastPaintObjectIndex = objIdx;
        lastPaintMode = mode;
        nativeLifecycleEvents.add("paint_begin");
        return nextPaintSessionPtr;
    }
    public static double[] paint_raycast(long sessionPtr, double[] origin, double[] dir) {
        nativeLifecycleEvents.add("paint_raycast");
        return new double[]{4.0, 5.0, 6.0, 7.0};
    }
    public static void paint_brush(long sessionPtr, double[] hit, int facetStart, double radius,
                                   int filamentIdx, double[] cameraPos, boolean sphere) {
        lastPaintFacetStart = facetStart;
        lastPaintRadius = radius;
        lastPaintFilament = filamentIdx;
        lastPaintSphere = sphere;
        nativeLifecycleEvents.add("paint_brush");
    }
    public static void paint_bucket(long sessionPtr, double[] hit, int facetStart, int filamentIdx,
                                    boolean propagate, double angle) {
        lastPaintFacetStart = facetStart;
        lastPaintFilament = filamentIdx;
        lastPaintPropagate = propagate;
        lastPaintAngle = angle;
        nativeLifecycleEvents.add("paint_bucket");
    }
    public static void paint_height(long sessionPtr, double zMin, double zMax, int filamentIdx) {
        lastPaintZMin = zMin;
        lastPaintZMax = zMax;
        lastPaintFilament = filamentIdx;
        nativeLifecycleEvents.add("paint_height");
    }
    public static void paint_clear(long sessionPtr) { nativeLifecycleEvents.add("paint_clear"); }
    public static void paint_commit(long sessionPtr) { nativeLifecycleEvents.add("paint_commit"); }
    public static int glmodel_init_from_paint(long glPtr, long sessionPtr, int filamentIdx) {
        lastPaintFilament = filamentIdx;
        nativeLifecycleEvents.add("glmodel_init_from_paint");
        return 29;
    }
    public static void paint_end(long sessionPtr) {
        paintEndCalls++;
        lastPaintEndedPtr = sessionPtr;
        nativeLifecycleEvents.add("paint_end");
    }

    // Shader mocks
    public static long shader_init_from_texts(String name, String fragment, String vertex) { return 100L; }
    public static int shader_get_id(long ptr) { return 1; }
    public static int shader_get_uniform_location(long ptr, String name) { return 0; }
    public static int shader_get_attrib_location(long ptr, String name) { return 0; }
    public static void shader_start_using(long ptr) {}
    public static void shader_stop_using(long ptr) {}
    public static void shader_release(long ptr) {}

    // GLModel mocks
    public static long glmodel_create() { return nextPtr++; }
    public static void glmodel_init_from_model(long ptr, long model) {}
    public static void glmodel_init_from_model_object(long ptr, long model, int i) {}
    public static void glmodel_init_raycast_data(long ptr) {}
    public static void glmodel_set_color(long ptr, float red, float green, float blue, float alpha) {}
    public static void glmodel_render(long ptr) {}
    public static void glmodel_stilized_arrow(long ptr, float tipRadius, float tipLength, float stemRadius, float stemLength) {}
    public static void glmodel_init_background_triangles(long ptr) {}
    public static void glmodel_init_box(long ptr, float width, float depth, float height) {}
    public static void glmodel_init_bounding_box(long ptr, long modelPtr, int i) {}
    public static boolean glmodel_is_initialized(long ptr) { return true; }
    public static boolean glmodel_is_empty(long ptr) { return false; }
    public static double[] glmodel_raycast_closest_hit(long ptr, double[] point, double[] direction) {
        // Return hit at (0,0,0) with normal (0,0,1)
        return new double[]{0, 0, 0, 0, 0, 1};
    }
    public static void glmodel_reset(long ptr) {}
    public static void glmodel_release(long ptr) {
        glModelReleaseCalls++;
        lastGlModelReleasedPtr = ptr;
    }

    // VGCode mocks
    public static long vgcode_create() { return 6000L; }
    public static void vgcode_init(long ptr) {}
    public static boolean vgcode_is_initialized(long ptr) { return true; }
    public static void vgcode_set_colors(long ptr, int[] colors) {}
    public static long vgcode_get_layers_count(long ptr) { return 100L; }
    public static void vgcode_load(long ptr, long resultPtr) {}
    public static void vgcode_render(long ptr, float[] viewMatrix, float[] projectionMatrix) {}
    public static void vgcode_set_layers_view_range(long ptr, long min, long max) {}
    public static long[] vgcode_get_layers_view_range(long ptr) { return new long[]{0, 100}; }
    public static float vgcode_get_estimated_time(long ptr) { return 3600f; }
    public static float vgcode_get_estimated_time_role(long ptr, int role) { return 600f; }
    public static boolean vgcode_is_extrusion_role_visible(long ptr, int role) { return true; }
    public static void vgcode_toggle_extrusion_role_visibility(long ptr, int role) {}
    public static void vgcode_reset(long ptr) {}
    public static void vgcode_release(long ptr) {}

    // Utils config mocks
    public static long utils_config_create(String config) { return 7000L; }
    public static boolean utils_config_check_compatibility(long ptr, String condition) { return true; }
    public static String utils_config_eval(long ptr, String condition) throws Slic3rRuntimeError { return "eval_result"; }
    public static void utils_config_release(long ptr) {}

    public static void utils_calc_view_normal_matrix(double[] viewMatrix, double[] worldMatrix, double[] normalMatrix) {}
    public static double[] utils_unproject(double[] viewMatrix, double[] projectionMatrix, int screenWidth, int screenHeight, double x, double y) {
        return new double[]{0.0, 0.0, 0.0};
    }

    // Zip bundle extraction mock helper
    public static String orca_bundle_read(String archivePath, String extractDir) throws IOException, JSONException {
        // Return a mock JSON bundle
        JSONObject result = new JSONObject();
        result.put("bundle_structure_json", "{}");
        result.put("files", new JSONArray());
        return result.toString();
    }
}
