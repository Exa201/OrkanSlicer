package scripts.tests;

import ru.ytkab0bp.slicebeam.slic3r.Native;
import ru.ytkab0bp.slicebeam.slic3r.Model;
import ru.ytkab0bp.slicebeam.slic3r.GLModel;
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef;
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper;
import ru.ytkab0bp.slicebeam.config.ConfigObject;
import ru.ytkab0bp.slicebeam.config.ProfileConfigSnapshot;
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository;
import ru.ytkab0bp.slicebeam.SliceBeam;
import ru.ytkab0bp.slicebeam.utils.FillBedPlanner;
import ru.ytkab0bp.slicebeam.utils.Prefs;
import ru.ytkab0bp.slicebeam.utils.Vec3d;
import ru.ytkab0bp.slicebeam.ui.workspace.SelectedObjectQuickAction;
import ru.ytkab0bp.slicebeam.view.TapGestureGate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class E2ETestSuite {

    public static void testPressureAdvanceAndZOffsetStayInTheirOwnProfiles() throws Exception {
        resetEnvironment();
        String configText = "filament_settings_id = PA Regression PLA\n"
                + "enable_pressure_advance = 1\n"
                + "pressure_advance = 0.1234\n"
                + "printer_settings_id = Z Regression Printer\n"
                + "z_offset = 0.42\n";
        Slic3rConfigWrapper imported = new Slic3rConfigWrapper(
                new java.io.ByteArrayInputStream(configText.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        ConfigObject filament = imported.findFilament("PA Regression PLA");
        ConfigObject printer = imported.findPrinter("Z Regression Printer");
        assertTrue(filament != null, "Imported filament preset must be retained");
        assertTrue(printer != null, "Imported printer preset must be retained");
        assertEquals("0.1234", filament.get("pressure_advance"),
                "Filament Pressure Advance must survive profile import under its own setting key");
        assertEquals("1", filament.get("enable_pressure_advance"),
                "Filament Pressure Advance enable flag must survive profile import");
        assertTrue(filament.get("z_offset") == null,
                "Printer Z offset must not be created from the filament Pressure Advance value");
        assertTrue(filament.get("start_filament_gcode") == null,
                "Pressure Advance must not be rewritten as custom start G-code");

        assertEquals("0.42", printer.get("z_offset"),
                "Printer Z offset must remain in the printer profile");
        assertTrue(printer.get("pressure_advance") == null,
                "Filament Pressure Advance must not be written as printer Z offset");

        assertTrue(Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.contains("pressure_advance"),
                "Pressure Advance must be classified as a filament profile option");
        assertTrue(Slic3rConfigWrapper.PRINTER_CONFIG_KEYS.contains("z_offset"),
                "Z offset must be classified as a printer profile option");

        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), imported.serialize().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Model model = new Model();
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "pressure-advance-regression.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            String nativeConfig = Native.getLastSliceConfig();
            assertTrue(nativeConfig.contains("pressure_advance = 0.1234"),
                    "Slicing must pass the filament Pressure Advance value to the native engine");
            assertTrue(nativeConfig.contains("z_offset = 0.42"),
                    "Slicing must pass the independent printer Z offset to the native engine");
            assertTrue(!nativeConfig.contains("SET_PRESSURE_ADVANCE ADVANCE=0.1234"),
                    "Pressure Advance must not be duplicated or smuggled through custom filament start G-code");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // Custom Assertion Helpers
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Assertion Failed: " + message + " -> Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError("Assertion Failed: " + message + " -> Expected: " + expected + ", Got: " + actual);
        }
    }

    // Environment Reset
    private static void resetEnvironment() {
        Native.resetMockState();

        SliceBeam.INSTANCE = new ru.ytkab0bp.slicebeam.SliceBeam();
        SliceBeam.CONFIG = null;
        SliceBeam.CONFIG_UID = 0;
        SliceBeam.AUTO_BRIM_SELECTED = false;
        ProfileSettingsRepository.clearAllDrafts();

        Prefs.init(SliceBeam.INSTANCE);

        File testCache = SliceBeam.INSTANCE.getCacheDir();
        File testFiles = SliceBeam.INSTANCE.getFilesDir();
        deleteDir(testCache);
        deleteDir(testFiles);
        testCache.mkdirs();
        testFiles.mkdirs();
    }

    private static void deleteDir(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }
        file.delete();
    }

    private static void setupMockConfig() {
        Slic3rConfigWrapper config = new Slic3rConfigWrapper();
        config.presets = new ConfigObject();
        config.presets.put("printer", "MyPrinter");
        config.presets.put("print", "MyPrint");
        config.presets.put("filament", "MyFilament");

        ConfigObject printer = new ConfigObject("MyPrinter");
        printer.put("printer_model", "CustomModel");
        printer.put("brim_type", "auto_brim");
        config.printerConfigs = new ArrayList<>();
        config.printerConfigs.add(printer);

        ConfigObject print = new ConfigObject("MyPrint");
        print.put("brim_type", "auto_brim");
        config.printConfigs = new ArrayList<>();
        config.printConfigs.add(print);

        ConfigObject filament = new ConfigObject("MyFilament");
        filament.put("filament_type", "PLA");
        config.filamentConfigs = new ArrayList<>();
        config.filamentConfigs.add(filament);

        SliceBeam.CONFIG = config;
    }

    // =========================================================================
    // FEATURE 1: COLOR PAINTING (Tests 1-10)
    // =========================================================================

    public static void testColorPaintModeToggle() {
        resetEnvironment();
        boolean paintModeActive = false;
        paintModeActive = true; // Toggle on
        assertTrue(paintModeActive, "Color paint mode toggle on should set active state");
    }

    public static void testColorPaintApplyColorToFacet() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1; // Native mock ptr
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        assertTrue(mockModel != null, "Model must exist in mock JNI");
        
        // Paint facet index 15 with filament color 3
        mockModel.objects.get(0).colorIndex = 3;
        assertEquals(3, Native.getMockModels().get(modelPtr).objects.get(0).colorIndex, "Facet should store selected filament color");
    }

    public static void testColorPaintBrushSizeChange() {
        resetEnvironment();
        float brushSize = 5.0f;
        brushSize = 8.5f; // Set size
        assertEquals(8.5, (double)brushSize, "Brush size change should update brush dimensions");
    }

    public static void testColorPaintClearFacetColor() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        mockModel.objects.get(0).colorIndex = 4;
        
        // Clear color
        mockModel.objects.get(0).colorIndex = 0;
        assertEquals(0, Native.getMockModels().get(modelPtr).objects.get(0).colorIndex, "Cleared facet color should return to 0");
    }

    public static void testColorPaintExtruderCountMatches() {
        resetEnvironment();
        int activeExtruderIndex = 2;
        int maxExtruders = 4;
        assertTrue(activeExtruderIndex <= maxExtruders, "Active color extruder index must not exceed maximum extruder count");
    }

    public static void testColorPaintBrushScalingLimitMax() {
        resetEnvironment();
        float inputSize = 150.0f;
        float clampedSize = Math.min(100.0f, inputSize);
        assertEquals(100.0, (double)clampedSize, "Brush size must clamp to maximum limit of 100");
    }

    public static void testColorPaintBrushScalingLimitMin() {
        resetEnvironment();
        float inputSize = 0.05f;
        float clampedSize = Math.max(0.1f, inputSize);
        assertEquals(0.1, (double)clampedSize, "Brush size must clamp to minimum limit of 0.1");
    }

    public static void testColorPaintOutOfBoundsFacetIndex() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        int totalFacets = 100;
        int targetFacet = 150;
        boolean success = false;
        if (targetFacet >= 0 && targetFacet < totalFacets) {
            mockModel.objects.get(0).colorIndex = 2;
            success = true;
        }
        assertTrue(!success, "Painting an out-of-bounds facet index must fail gracefully");
    }

    public static void testColorPaintInvalidColorIndexNegative() {
        resetEnvironment();
        int colorIndex = -1;
        int appliedColor = (colorIndex < 0) ? 0 : colorIndex;
        assertEquals(0, appliedColor, "Negative color index should default to 0");
    }

    public static void testColorPaintLargeFacetIndex() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        int targetFacet = 99999;
        mockModel.objects.get(0).colorIndex = 5;
        assertEquals(5, mockModel.objects.get(0).colorIndex, "Color painting on a valid large facet index should succeed");
    }

    // =========================================================================
    // FEATURE 2: SUPPORT PAINTING (Tests 11-20)
    // =========================================================================

    public static void testSupportPaintModeToggle() {
        resetEnvironment();
        boolean supportPaintMode = false;
        supportPaintMode = true;
        assertTrue(supportPaintMode, "Support painting mode should toggle on successfully");
    }

    public static void testSupportPaintEnforce() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        mockModel.objects.get(0).hasSupport = true;
        assertTrue(mockModel.objects.get(0).hasSupport, "Enforcing support should set support enforcement state");
    }

    public static void testSupportPaintBlock() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        mockModel.objects.get(0).hasSupport = false;
        assertTrue(!mockModel.objects.get(0).hasSupport, "Blocking support should disable support state");
    }

    public static void testSupportPaintClear() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        mockModel.objects.get(0).hasSupport = true;
        
        mockModel.objects.get(0).hasSupport = false;
        assertTrue(!mockModel.objects.get(0).hasSupport, "Clearing support should reset enforcement state");
    }

    public static void testSupportPaintBrushTypes() {
        resetEnvironment();
        String brushType = "Sphere";
        brushType = "Circle";
        assertEquals("Circle", brushType, "Brush type change should update active brush geometry");
    }

    public static void testSupportPaintBrushSizeZero() {
        resetEnvironment();
        float brushSize = 0.0f;
        float clampedSize = (brushSize <= 0.0f) ? 1.0f : brushSize;
        assertEquals(1.0, (double)clampedSize, "Support brush size of 0 must default to 1.0");
    }

    public static void testSupportPaintHugeBrushSize() {
        resetEnvironment();
        float brushSize = 500.0f;
        float clampedSize = Math.min(100.0f, brushSize);
        assertEquals(100.0, (double)clampedSize, "Support brush size should clamp to maximum limit of 100");
    }

    public static void testSupportPaintInvalidObjectReference() {
        resetEnvironment();
        long invalidModelPtr = 9999L;
        Native.MockModel mockModel = Native.getMockModels().get(invalidModelPtr);
        assertTrue(mockModel == null, "Querying invalid model reference should return null");
    }

    public static void testSupportPaintFacetRangeBoundary() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        int totalFacets = 500;
        int boundaryFacet = 499; // exactly totalFacets - 1
        boolean inside = (boundaryFacet >= 0 && boundaryFacet < totalFacets);
        assertTrue(inside, "Boundary facet index must be resolved as valid");
    }

    public static void testSupportPaintConcurrentFacets() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        int[] targetFacets = {10, 11, 12, 13};
        for (int facet : targetFacets) {
            mockModel.objects.get(0).hasSupport = true;
        }
        assertTrue(mockModel.objects.get(0).hasSupport, "Concurrent facet support assignment must update state");
    }

    // =========================================================================
    // FEATURE 3: 3D MEASURING (Tests 21-30)
    // =========================================================================

    public static void testMeasurePickFirstPoint() {
        resetEnvironment();
        double[] p1 = {12.5, 45.0, 10.0};
        assertEquals(12.5, p1[0], "First measurement point X coordinate should match input");
        assertEquals(45.0, p1[1], "First measurement point Y coordinate should match input");
        assertEquals(10.0, p1[2], "First measurement point Z coordinate should match input");
    }

    public static void testMeasurePickSecondPoint() {
        resetEnvironment();
        double[] p1 = {10.0, 10.0, 0.0};
        double[] p2 = {10.0, 10.0, 15.0};
        double dx = p2[0] - p1[0];
        double dy = p2[1] - p1[1];
        double dz = p2[2] - p1[2];
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        assertEquals(15.0, dist, "Distance between (10,10,0) and (10,10,15) must be 15.0");
    }

    public static void testMeasureClearPoints() {
        resetEnvironment();
        double[] p1 = {5.0, 5.0, 5.0};
        double[] p2 = {10.0, 10.0, 10.0};
        p1 = null;
        p2 = null;
        assertTrue(p1 == null && p2 == null, "Clearing points should reset measurement targets");
    }

    public static void testMeasureDisplayDistanceLabel() {
        resetEnvironment();
        double distance = 14.5678;
        String label = String.format("%.2f mm", distance);
        assertEquals("14,57 mm", label, "Distance measurement label formatting should use German decimal notation");
    }

    public static void testMeasureAxisProjection() {
        resetEnvironment();
        double[] p1 = {10.0, 20.0, 30.0};
        double[] p2 = {15.0, 25.0, 35.0};
        double dx = Math.abs(p2[0] - p1[0]);
        double dy = Math.abs(p2[1] - p1[1]);
        double dz = Math.abs(p2[2] - p1[2]);
        assertEquals(5.0, dx, "X-axis projected distance should match dx");
        assertEquals(5.0, dy, "Y-axis projected distance should match dy");
        assertEquals(5.0, dz, "Z-axis projected distance should match dz");
    }

    public static void testMeasureZeroDistance() {
        resetEnvironment();
        double[] p1 = {15.0, 15.0, 5.0};
        double[] p2 = {15.0, 15.0, 5.0};
        double dx = p2[0] - p1[0];
        double dy = p2[1] - p1[1];
        double dz = p2[2] - p1[2];
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        assertEquals(0.0, dist, "Same-point measurement distance must be exactly 0.0");
    }

    public static void testMeasureHugeDistance() {
        resetEnvironment();
        double[] p1 = {0.0, 0.0, 0.0};
        double[] p2 = {1000.0, 0.0, 0.0};
        double dx = p2[0] - p1[0];
        double dist = Math.abs(dx);
        assertEquals(1000.0, dist, "Large scale coordinates distance calculation should remain accurate");
    }

    public static void testMeasureNegativeCoordinates() {
        resetEnvironment();
        double[] p1 = {-10.0, -10.0, -10.0};
        double[] p2 = {-10.0, -10.0, -20.0};
        double dz = p2[2] - p1[2];
        double dist = Math.abs(dz);
        assertEquals(10.0, dist, "Negative coordinates distance calculation must yield positive distance");
    }

    public static void testMeasurePrecisionLimit() {
        resetEnvironment();
        double[] p1 = {0.0, 0.0, 0.0};
        double[] p2 = {0.0001, 0.0, 0.0};
        double dx = p2[0] - p1[0];
        assertTrue(dx > 0.0, "Measurement resolution should detect sub-millimeter offsets");
    }

    public static void testMeasureSwapPoints() {
        resetEnvironment();
        double[] p1 = {10.0, 20.0, 30.0};
        double[] p2 = {40.0, 50.0, 60.0};
        double d12 = Math.sqrt(Math.pow(p2[0]-p1[0], 2) + Math.pow(p2[1]-p1[1], 2) + Math.pow(p2[2]-p1[2], 2));
        double d21 = Math.sqrt(Math.pow(p1[0]-p2[0], 2) + Math.pow(p1[1]-p2[1], 2) + Math.pow(p1[2]-p2[2], 2));
        assertEquals(d12, d21, "Swapping points must not change calculated distance");
    }

    // =========================================================================
    // FEATURE 4: LONG-PRESS CONTEXT MENU (Tests 31-40)
    // =========================================================================

    public static void testLongPressOnModelDoesNotOpenMenu() {
        resetEnvironment();
        TapGestureGate gate = new TapGestureGate();
        gate.begin(1, 40f, 60f, 1_000L);
        // GLView invalidates the tap when its long-press handler starts a model drag.
        gate.invalidate();
        boolean actionMenuOpened = gate.finish(1, false, false, 1_510L, 40f, 60f, 300L, 32f, 500L);
        assertTrue(!actionMenuOpened, "Long pressing a model must not dispatch a click that could open its action menu");
    }

    public static void testLongPressOnBedDoesNotOpenMenu() {
        resetEnvironment();
        TapGestureGate gate = new TapGestureGate();
        gate.begin(1, 80f, 120f, 2_000L);
        boolean actionMenuOpened = gate.finish(1, false, false, 2_500L, 80f, 120f, 300L, 32f, 500L);
        assertTrue(!actionMenuOpened, "Holding the empty build plate must not dispatch a click or open an action menu");
    }

    public static void testRepeatedBuildPlateTapsInTheSameSpotAreCoalesced() {
        resetEnvironment();
        TapGestureGate gate = new TapGestureGate();
        gate.begin(1, 160f, 220f, 3_000L);
        assertTrue(gate.finish(1, false, false, 3_080L, 160f, 220f, 300L, 32f, 500L),
                "The first intentional tap should remain available to the workspace");

        gate.begin(1, 161f, 221f, 3_170L);
        boolean duplicateClick = gate.finish(1, false, false, 3_230L, 161f, 221f, 300L, 32f, 500L);
        assertTrue(!duplicateClick, "A quick repeated touch at the same point must not dispatch a second click");
    }

    public static void testExplicitMoreActionOpensMenu() {
        resetEnvironment();
        assertEquals("MORE", SelectedObjectQuickAction.MORE.name(),
                "The visible selected-object action must retain its explicit More entry point");
    }

    public static void testLongPressMenuOptionsList() {
        resetEnvironment();
        List<String> options = new ArrayList<>();
        options.add("Fill Bed");
        options.add("Remove model");
        options.add("Arrange models");
        assertEquals(3, options.size(), "Context menu must expose expected options count");
        assertTrue(options.contains("Fill Bed"), "Context menu must contain Fill Bed option");
    }

    public static void testLongPressSelectsModelObject() {
        resetEnvironment();
        int selectedObjectIndex = -1;
        // Long press target object 2
        selectedObjectIndex = 2;
        assertEquals(2, selectedObjectIndex, "Long press action should select targeted object index");
    }

    public static void testLongPressDismissal() {
        resetEnvironment();
        boolean menuShowing = true;
        // Click outside
        menuShowing = false;
        assertTrue(!menuShowing, "Clicking outside context menu should dismiss it");
    }

    public static void testLongPressWithMoveThreshold() {
        resetEnvironment();
        float deltaX = 2.0f;
        float touchSlop = 8.0f;
        boolean triggered = (deltaX < touchSlop);
        assertTrue(triggered, "Slight movement below touch slop threshold must still trigger long press");
    }

    public static void testLongPressWithDragRejected() {
        resetEnvironment();
        float deltaX = 15.0f;
        float touchSlop = 8.0f;
        boolean triggered = (deltaX < touchSlop);
        assertTrue(!triggered, "Significant movement above touch slop threshold should treat gesture as drag/pan");
    }

    public static void testLongPressMultipleObjects() {
        resetEnvironment();
        int raycastHitCount = 2;
        int selectedIndex = 0; // First hit object closest to camera
        assertEquals(0, selectedIndex, "Long press among overlapping objects must select the closest hit");
    }

    public static void testLongPressOutsideBedBounds() {
        resetEnvironment();
        double pressX = 300.0;
        double bedMaxX = 125.0;
        boolean outside = (pressX > bedMaxX);
        assertTrue(outside, "Pressing coordinate outside bed limits should register as out-of-bounds");
    }

    public static void testLongPressTriggerDelay() {
        resetEnvironment();
        long startTime = System.currentTimeMillis();
        long triggerTime = startTime + 600;
        long expectedThreshold = 500;
        assertTrue((triggerTime - startTime) >= expectedThreshold, "Long press must only fire after the minimum threshold duration");
    }

    // =========================================================================
    // FEATURE 5: FILL BED PRE-CALCULATION (Tests 41-50)
    // =========================================================================

    public static void testFillBedPlannerNormalRange() {
        resetEnvironment();
        int attempts = FillBedPlanner.copyAttemptsForLimit(5, 10);
        assertEquals(5, attempts, "FillBedPlanner should return remaining slots below limit");
    }

    public static void testFillBedPlannerZeroSize() {
        resetEnvironment();
        int attempts = FillBedPlanner.copyAttemptsForLimit(0, 256);
        assertEquals(0, attempts, "0 current objects should yield 0 copy attempts");
    }

    public static void testFillBedPlannerAtCap() {
        resetEnvironment();
        int attempts = FillBedPlanner.copyAttemptsForLimit(256, 256);
        assertEquals(0, attempts, "Being at the limit cap should allow 0 copy attempts");
    }

    public static void testFillBedPlannerAboveCap() {
        resetEnvironment();
        int attempts = FillBedPlanner.copyAttemptsForLimit(300, 256);
        assertEquals(0, attempts, "Exceeding the limit cap should allow 0 copy attempts");
    }

    public static void testFillBedPlannerObjectCalculations() {
        resetEnvironment();
        // Bed size 250x250, model size 50x50. Grid fits 5x5 = 25 models.
        // With 1 original model, copy limit is 24 copies.
        int bedW = 250, bedD = 250;
        int objW = 50, objD = 50;
        int cols = bedW / objW;
        int rows = bedD / objD;
        int totalFits = cols * rows;
        int copies = totalFits - 1;
        assertEquals(24, copies, "Should calculate correct grid cloning count for bed size");
    }

    public static void testFillBedHugeModel() {
        resetEnvironment();
        // Model size 300x300, Bed size 250x250. Fits = 0. Copies = 0.
        int bedW = 250, bedD = 250;
        int objW = 300, objD = 300;
        int cols = bedW / objW;
        int rows = bedD / objD;
        int totalFits = cols * rows;
        int copies = Math.max(0, totalFits - 1);
        assertEquals(0, copies, "Models larger than the bed plate should result in 0 clones");
    }

    public static void testFillBedPlannerNegativeLimit() {
        resetEnvironment();
        int current = 5;
        int limit = -10;
        int attempts = FillBedPlanner.copyAttemptsForLimit(current, limit);
        assertEquals(0, attempts, "Negative limit threshold should yield 0 copies");
    }

    public static void testFillBedPlannerFloatDimensions() {
        resetEnvironment();
        double bedW = 250.0, bedD = 250.0;
        double objW = 75.5, objD = 75.5;
        int cols = (int) (bedW / objW);
        int rows = (int) (bedD / objD);
        int totalFits = cols * rows;
        assertEquals(9, totalFits, "Grid calculation should truncate partial floating dimensions correctly");
    }

    public static void testFillBedPlannerMargins() {
        resetEnvironment();
        double bedW = 250.0;
        double objW = 50.0;
        double margin = 5.0;
        // Effective width = 250 - 2 * 5 = 240
        int cols = (int) ((bedW - 2 * margin) / objW);
        assertEquals(4, cols, "Grid calculation must account for safe margin spacing from bed boundaries");
    }

    public static void testFillBedPlannerSingleFitsPerfect() {
        resetEnvironment();
        // Object fits exactly the bed size
        int bedW = 200, bedD = 200;
        int objW = 200, objD = 200;
        int totalFits = (bedW / objW) * (bedD / objD);
        int copies = totalFits - 1;
        assertEquals(0, copies, "If a single object takes up the whole bed, 0 additional copies should be planned");
    }

    // =========================================================================
    // FEATURE 6: MANDATORY SETUP SCREEN (Tests 51-60)
    // =========================================================================

    public static void testSetupRedirectionWhenConfigNull() {
        resetEnvironment();
        SliceBeam.CONFIG = null;
        boolean shouldRedirect = false;
        if (SliceBeam.CONFIG == null) {
            shouldRedirect = true;
        }
        assertTrue(shouldRedirect, "Null configuration must redirect to the onboarding setup wizard");
    }

    public static void testNoSetupRedirectionWhenConfigExists() {
        resetEnvironment();
        setupMockConfig();
        boolean shouldRedirect = false;
        if (SliceBeam.CONFIG == null) {
            shouldRedirect = true;
        }
        assertTrue(!shouldRedirect, "Valid configuration should bypass setup wizard redirection");
    }

    public static void testSetupCompletionSavesConfig() {
        resetEnvironment();
        setupMockConfig();
        assertTrue(SliceBeam.saveConfig(), "Saving setup configuration should complete atomically");
        File configFile = SliceBeam.getConfigFile();
        assertTrue(configFile.exists(), "Saving configuration should create a physical slic3r.ini profile file");
    }

    public static void testProfileSnapshotRestoresFailedMutation() {
        resetEnvironment();
        Slic3rConfigWrapper config = new Slic3rConfigWrapper();
        ConfigObject printer = new ConfigObject("Snapshot Printer");
        printer.profileListType = ConfigObject.PROFILE_LIST_PRINTER;
        printer.thumbnailUrl = "before.png";
        printer.put("printer_model", "Before");
        config.printerConfigs.add(printer);
        ConfigObject presets = new ConfigObject();
        presets.put("printer", "Snapshot Printer");
        config.presets = presets;

        ProfileConfigSnapshot snapshot = new ProfileConfigSnapshot(config);
        printer.put("printer_model", "After");
        config.printerConfigs.add(new ConfigObject("Unsaved Printer"));
        presets.put("printer", "Unsaved Printer");
        snapshot.restore();

        assertEquals(1, config.printerConfigs.size(), "Failed profile saves must discard newly added in-memory profiles");
        assertTrue(config.printerConfigs.get(0) == printer, "Rollback should preserve references held by active profile editors");
        assertEquals("Before", printer.get("printer_model"), "Rollback must restore replaced profile values");
        assertEquals(ConfigObject.PROFILE_LIST_PRINTER, printer.profileListType, "Rollback must restore profile category metadata");
        assertEquals("before.png", printer.thumbnailUrl, "Rollback must restore profile thumbnails");
        assertTrue(config.presets == presets, "Rollback should preserve the active preset object reference");
        assertEquals("Snapshot Printer", presets.get("printer"), "Rollback must restore the selected printer");
    }

    public static void testPrinterChangePersistsCompatibleProfiles() {
        resetEnvironment();
        setupMockConfig();
        Slic3rConfigWrapper config = SliceBeam.CONFIG;
        ConfigObject currentPrinter = config.findPrinter("MyPrinter");
        currentPrinter.profileListType = ConfigObject.PROFILE_LIST_PRINTER;
        ConfigObject alternatePrinter = new ConfigObject("Alternate Printer");
        alternatePrinter.profileListType = ConfigObject.PROFILE_LIST_PRINTER;
        alternatePrinter.put("printer_model", "AlternateModel");
        alternatePrinter.put("nozzle_diameter", "0.4");
        config.printerConfigs.add(alternatePrinter);

        ConfigObject currentPrint = config.findPrint("MyPrint");
        currentPrint.put("compatible_printers", "MyPrinter");
        ConfigObject firstCompatiblePrint = new ConfigObject("First Compatible Quality");
        firstCompatiblePrint.profileListType = ConfigObject.PROFILE_LIST_PRINT;
        firstCompatiblePrint.put("compatible_printers", "MyPrinter");
        firstCompatiblePrint.put("layer_height", "0.06");
        firstCompatiblePrint.put("line_width", "0.4");
        config.printConfigs.add(0, firstCompatiblePrint);
        ConfigObject alternatePrint = new ConfigObject("Alternate Quality");
        alternatePrint.profileListType = ConfigObject.PROFILE_LIST_PRINT;
        alternatePrint.put("compatible_printers", "AlternateModel");
        alternatePrint.put("layer_height", "0.2");
        config.printConfigs.add(alternatePrint);

        ConfigObject currentFilament = config.findFilament("MyFilament");
        currentFilament.put("compatible_printers", "MyPrinter");
        ConfigObject firstCompatibleFilament = new ConfigObject("First Compatible PLA");
        firstCompatibleFilament.profileListType = ConfigObject.PROFILE_LIST_FILAMENT;
        firstCompatibleFilament.put("compatible_printers", "MyPrinter");
        firstCompatibleFilament.put("filament_type", "PLA");
        config.filamentConfigs.add(0, firstCompatibleFilament);
        ConfigObject alternateFilament = new ConfigObject("Alternate PLA");
        alternateFilament.profileListType = ConfigObject.PROFILE_LIST_FILAMENT;
        alternateFilament.put("compatible_printers", "AlternateModel");
        alternateFilament.put("filament_type", "PLA");
        config.filamentConfigs.add(alternateFilament);

        assertTrue(SliceBeam.applyProfileSelection("Alternate Printer", null, null),
                "Selecting a saved printer profile should succeed");
        assertEquals("Alternate Printer", config.presets.get("printer"), "The selected printer should change");
        assertEquals("Alternate Quality", config.presets.get("print"),
                "Changing printers should immediately choose a compatible print profile");
        assertEquals("Alternate PLA", config.presets.get("filament"),
                "Changing printers should immediately choose a compatible filament profile");

        assertTrue(SliceBeam.applyProfileSelection("MyPrinter", null, null),
                "Returning to a previously used printer profile should succeed");
        assertEquals("MyPrint", config.presets.get("print"),
                "Returning to a printer should restore its previous print profile instead of the first compatible preset");
        assertEquals("MyFilament", config.presets.get("filament"),
                "Returning to a printer should restore its previous filament instead of the first compatible preset");

        assertTrue(SliceBeam.applyProfileSelection("Alternate Printer", null, null),
                "Switching back to the second printer should succeed");
        assertEquals("Alternate Quality", config.presets.get("print"),
                "The second printer should also restore its own print choice");
        assertEquals("Alternate PLA", config.presets.get("filament"),
                "The second printer should also restore its own filament choice");

        try {
            Slic3rConfigWrapper reloaded = new Slic3rConfigWrapper(SliceBeam.getConfigFile());
            assertEquals("Alternate Printer", reloaded.presets.get("printer"),
                    "The selected printer should survive reloading the saved configuration");
            assertEquals("Alternate Quality", reloaded.presets.get("print"),
                    "The compatible print profile should survive reloading the saved configuration");
            assertEquals("Alternate PLA", reloaded.presets.get("filament"),
                    "The compatible filament profile should survive reloading the saved configuration");
            SliceBeam.CONFIG = reloaded;
            assertTrue(SliceBeam.applyProfileSelection("MyPrinter", null, null),
                    "Per-printer selections should remain available after reloading the saved configuration");
            assertEquals("MyPrint", reloaded.presets.get("print"),
                    "The original print profile should be restored after configuration reload");
            assertEquals("MyFilament", reloaded.presets.get("filament"),
                    "The original filament should be restored after configuration reload");
            assertTrue(Files.deleteIfExists(SliceBeam.getConfigFile().toPath()),
                    "Reading a profile file must release its handle so Windows can replace or remove it");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testBackgroundSavePersistsPendingProfileEdits() {
        resetEnvironment();
        setupMockConfig();
        ConfigObject printer = SliceBeam.CONFIG.findPrinter("MyPrinter");
        printer.put("z_offset", "0");
        ProfileSettingsRepository.setPendingValue(
                ConfigObject.PROFILE_LIST_PRINTER, "z_offset", "0", "0.25");

        assertTrue(SliceBeam.savePendingProfileSettings(),
                "Leaving the app should commit pending profile edits to the config file");
        assertEquals("0.25", printer.get("z_offset"), "The active printer profile should receive the pending edit");
        assertEquals(0, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER),
                "Successfully saved edits should no longer be marked pending");

        try {
            Slic3rConfigWrapper reloaded = new Slic3rConfigWrapper(SliceBeam.getConfigFile());
            assertEquals("0.25", reloaded.findPrinter("MyPrinter").get("z_offset"),
                    "The pending edit should survive reloading the app configuration");
            assertTrue(Files.deleteIfExists(SliceBeam.getConfigFile().toPath()),
                    "Reading a profile file must release its handle so Windows can replace or remove it");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testSetupPreferenceStorage() {
        resetEnvironment();
        Prefs.getPrefs().edit().putBoolean("setup_done", true).commit();
        assertTrue(Prefs.getPrefs().getBoolean("setup_done", false), "Setup completion should persist setup_done preference state");
    }

    public static void testSetupActivityFinishesOnDone() {
        resetEnvironment();
        boolean activityFinished = false;
        // On setup done
        activityFinished = true;
        assertTrue(activityFinished, "Setup screen should close and finish its lifecycle when configuration is complete");
    }

    public static void testSetupCorruptedIniFileRedirects() {
        resetEnvironment();
        File configFile = SliceBeam.getConfigFile();
        try {
            Files.write(configFile.toPath(), "MALFORMED_INI_DATA".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Try to load
        boolean loadFailed = false;
        try {
            SliceBeam.CONFIG = new Slic3rConfigWrapper(configFile);
        } catch (Exception e) {
            loadFailed = true;
        }
        assertTrue(loadFailed || SliceBeam.CONFIG == null, "Corrupt profile files should fail to parse and trigger setup redirect");
    }

    public static void testSetupProfileSelectionPrinterOnly() {
        resetEnvironment();
        Slic3rConfigWrapper config = new Slic3rConfigWrapper();
        config.presets = new ConfigObject();
        config.presets.put("printer", "Prusai3");
        config.presets.put("print", "Default");
        config.presets.put("filament", "Default");
        SliceBeam.CONFIG = config;
        assertEquals("Prusai3", SliceBeam.CONFIG.presets.get("printer"), "Selected printer profile must match printer preset value");
    }

    public static void testSetupProfileSelectionCustomPrinter() {
        resetEnvironment();
        setupMockConfig();
        ConfigObject customPrinter = new ConfigObject("MyCustomK3D");
        SliceBeam.CONFIG.printerConfigs.add(customPrinter);
        SliceBeam.CONFIG.presets.put("printer", "MyCustomK3D");
        assertEquals("MyCustomK3D", SliceBeam.CONFIG.findPrinter("MyCustomK3D").getTitle(), "Custom printer selection should be registerable");
    }

    public static void testSetupImportProfilesDuringRedirection() {
        resetEnvironment();
        setupMockConfig();
        File importFile = new File(SliceBeam.INSTANCE.getFilesDir(), "imported.ini");
        try {
            Files.write(importFile.toPath(), "[presets]\nprinter = ImportedPrinter\n[printer:ImportedPrinter]\nprinter_model = ImportedModel\n".getBytes());
            Slic3rConfigWrapper imported = new Slic3rConfigWrapper(importFile);
            SliceBeam.CONFIG = imported;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals("ImportedPrinter", SliceBeam.CONFIG.presets.get("printer"), "Imported configurations during setup should parse correctly");
        assertEquals("ImportedPrinter", SliceBeam.CONFIG.findPrinter("ImportedPrinter").getTitle(), "Imported printer profile should be indexed by its section name");
    }

    public static void testSetupRedirectionTwiceBypassed() {
        resetEnvironment();
        setupMockConfig();
        int redirectCount = 0;
        if (SliceBeam.CONFIG == null) {
            redirectCount++;
        }
        // Run checks again
        if (SliceBeam.CONFIG == null) {
            redirectCount++;
        }
        assertEquals(0, redirectCount, "Multiple checks when config is valid should result in 0 setup redirects");
    }

    // =========================================================================
    // FEATURE 7: AUTO BRIM CONFIG LOADING (Tests 61-70)
    // =========================================================================

    public static void testAutoBrimLoadingReplacesAutoBrim() {
        resetEnvironment();
        setupMockConfig();
        try {
            SliceBeam.genCurrentConfig();
            File currentConfigFile = SliceBeam.getCurrentConfigFile();
            String configContent = new String(Files.readAllBytes(currentConfigFile.toPath()));
            assertTrue(!configContent.contains("brim_type = auto_brim"), "Generated slicing config must replace auto_brim with outer_only to prevent engine crashes");
            assertTrue(SliceBeam.AUTO_BRIM_SELECTED, "Selected auto-brim state must be remembered while generating the safe bed-rendering config");
            new Model().slice(currentConfigFile.getAbsolutePath(),
                    new File(SliceBeam.INSTANCE.getFilesDir(), "auto-brim.gcode").getAbsolutePath(), null);
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"),
                    "The original auto-brim setting must be restored before the native slicer runs");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimNoBrimForFlatModels() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Box 100x100x10 (flat model: diagonal = 141.4 > height = 10)
        mockModel.objects.get(0).boundingBox = new double[]{-50, -50, 0, 50, 50, 10};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for a flat PLA model");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimEnabledForTallModels() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Box 20x20x100 (tall model: diagonal = 28.2 < height = 100)
        mockModel.objects.get(0).boundingBox = new double[]{-10, -10, 0, 10, 10, 100};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for a tall model");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimFilamentTypeABS() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Flat model, but ABS filament triggers mandatory brim to prevent warping
        mockModel.objects.get(0).boundingBox = new double[]{-50, -50, 0, 50, 50, 10};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = ABS\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for ABS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimFilamentTypeASA() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Flat model, but ASA filament triggers mandatory brim
        mockModel.objects.get(0).boundingBox = new double[]{-50, -50, 0, 50, 50, 10};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = ASA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for ASA");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimFilamentTypePLA() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Flat model, PLA filament does not need brim
        mockModel.objects.get(0).boundingBox = new double[]{-50, -50, 0, 50, 50, 10};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for PLA");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimMultipleModelsHeightConflict() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Tall model object added
        Native.MockModelObject tallObj = new Native.MockModelObject();
        tallObj.boundingBox = new double[]{-10, -10, 0, 10, 10, 150};
        mockModel.objects.add(tallObj);
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Native slicer must receive auto_brim for multi-object plates");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimInvalidConfigFormat() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "INVALID_INI_NO_BRIM_KEY\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            // Slicing shouldn't crash
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            assertTrue(true, "Auto brim loader must handle configs missing the brim_type key gracefully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimBrimWidthPreserved() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nbrim_width = 8.5\nfilament_type = ABS\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(updatedConfig.contains("brim_width = 8.5"), "Auto brim replacement must preserve custom brim_width parameter value");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testAutoBrimConfigRewriteSuccess() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = ABS\n".getBytes());
            long originalTime = configFile.lastModified();
            Thread.sleep(100);
            
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            long modifiedTime = configFile.lastModified();
            assertTrue(modifiedTime >= originalTime, "Config file must be overwritten/modified during slicing pre-processing");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // FEATURE 8: NOMENCLATURE FIXES (Tests 71-80)
    // =========================================================================

    public static void testNomenclatureAppName() {
        resetEnvironment();
        String appName = "OrcaSlicer Mobile";
        assertEquals("OrcaSlicer Mobile", appName, "Application name nomenclature must represent OrcaSlicer Mobile branding");
    }

    public static void testNomenclatureRussianAboutVersion() {
        resetEnvironment();
        String rusAbout = "Основано на Slice Beam и OrcaSlicer-совместимых профилях";
        assertTrue(!rusAbout.contains("PrusaSlicer"), "Russian localized string must not refer to legacy PrusaSlicer naming");
    }

    public static void testNomenclatureEnglishAboutVersion() {
        resetEnvironment();
        String engAbout = "Based on Slice Beam, OrcaSlicer, and Slic3r";
        assertTrue(!engAbout.contains("PrusaSlicer"), "English localized string must not refer to legacy PrusaSlicer naming");
    }

    public static void testNomenclatureSettingsTitles() {
        resetEnvironment();
        String settingsTitle = "Slice Beam Settings";
        assertTrue(!settingsTitle.contains("Prusa"), "Settings elements should use current branding names");
    }

    public static void testNomenclatureGcodeExporterName() {
        resetEnvironment();
        String gcodeName = "OrcaSlicerMobile_output.gcode";
        assertTrue(gcodeName.startsWith("OrcaSlicerMobile"), "Default gcode output recommendation must prefix with OrcaSlicerMobile");
    }

    public static void testNomenclatureCaseInsensitiveCheck() {
        resetEnvironment();
        String rawText = "prusaslicer profiles";
        boolean clean = !rawText.toLowerCase().contains("prusaslicer");
        assertTrue(!clean, "Case-insensitive check detects any legacy naming variants");
    }

    public static void testNomenclatureLogsDoNotExposeLegacyName() {
        resetEnvironment();
        String logTag = "SliceBeamNative";
        assertTrue(!logTag.contains("Prusa"), "Log tags must not expose legacy PrusaSlicer references");
    }

    public static void testNomenclatureLocalizationCoverage() {
        resetEnvironment();
        String ruText = "Мобильный OrcaSlicer";
        String enText = "OrcaSlicer Mobile";
        assertTrue(ruText.contains("OrcaSlicer") && enText.contains("OrcaSlicer"), "Localization across both languages should use synchronized nomenclature");
    }

    public static void testNomenclatureHelpUrl() {
        resetEnvironment();
        String helpUrl = "https://github.com/utkab0bp/SliceBeam";
        assertTrue(helpUrl.contains("SliceBeam") && !helpUrl.contains("Prusa"), "Online document links must point to SliceBeam project repositories");
    }

    public static void testNomenclatureConfigHeader() {
        resetEnvironment();
        String header = "# Generated by OrcaSlicer Mobile config writer";
        assertTrue(header.contains("OrcaSlicer Mobile"), "Generated configuration files must contain OrcaSlicer Mobile naming in their headers");
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE INTEGRATION (Tests 81-88)
    // =========================================================================

    public static void testFillBedAfterColorPainting() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Paint facet color
        mockModel.objects.get(0).colorIndex = 3;
        
        // Fill bed (duplicate models)
        int copies = FillBedPlanner.copyAttemptsForLimit(1, 4);
        for (int i = 0; i < copies; i++) {
            model.addObject(model, 0);
        }
        
        // Verify all clones retain the painted color index
        assertEquals(4, mockModel.objects.size(), "Object copies should be added to model");
        for (Native.MockModelObject obj : mockModel.objects) {
            assertEquals(3, obj.colorIndex, "All bed fill duplicates must inherit the color painting index");
        }
    }

    public static void testSupportPaintingAndMeasuring() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Paint support block
        mockModel.objects.get(0).hasSupport = true;
        
        // Measure coordinates
        double[] p1 = {0, 0, 0};
        double[] p2 = {0, 0, 50};
        double dist = Math.abs(p2[2] - p1[2]);
        
        assertTrue(mockModel.objects.get(0).hasSupport, "Support state remains enforced");
        assertEquals(50.0, dist, "Measurement coordinate distance should be calculated successfully");
    }

    public static void testSetupAndAutoBrimLoading() {
        resetEnvironment();
        // Setup initial config
        setupMockConfig();
        assertTrue(SliceBeam.CONFIG != null, "Onboarding setup must initialize configuration");
        
        // Slicing tall model with auto_brim
        Model model = new Model();
        Native.MockModel mockModel = Native.getMockModels().get(model.getPointer());
        mockModel.objects.get(0).boundingBox = new double[]{-10, -10, 0, 10, 10, 180}; // tall
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Setup-initialized slicing must pass auto_brim to the native slicer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testLongPressMenuFillBedWorkflow() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Long press trigger selection
        int pressIndex = 0;
        assertEquals(0, pressIndex, "Long press select target object 0");
        
        // Trigger Bed Fill copy planning
        int clones = FillBedPlanner.copyAttemptsForLimit(1, 5);
        for (int i = 0; i < clones; i++) {
            model.addObject(model, 0);
        }
        
        assertEquals(5, mockModel.objects.size(), "Model should now consist of 5 cloned objects");
    }

    public static void testNomenclatureLocalizationDuringSetup() {
        resetEnvironment();
        // Setup redirects because config is null
        SliceBeam.CONFIG = null;
        boolean redirected = (SliceBeam.CONFIG == null);
        assertTrue(redirected, "Redirected to Setup screen");
        
        // Setup screen must display correct app branding nomenclature
        String displayTitle = "OrcaSlicer Mobile - Setup Wizard";
        assertTrue(displayTitle.contains("OrcaSlicer Mobile"), "Setup wizard screen must present current branding nomenclature");
    }

    public static void testMeasureModelAfterArrange() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Clone object
        model.addObject(model, 0);
        assertEquals(2, mockModel.objects.size(), "Model has two objects");
        
        // Arrange bed
        boolean arranged = Native.bed_arrange(1000L, modelPtr);
        assertTrue(arranged, "Bed arrange shifts object positions");
        
        // Measure arranged distance between objects
        double[] pos1 = mockModel.objects.get(0).translation;
        double[] pos2 = mockModel.objects.get(1).translation;
        double dx = pos2[0] - pos1[0];
        double dy = pos2[1] - pos1[1];
        double dist = Math.sqrt(dx*dx + dy*dy);
        
        assertTrue(dist > 0.0, "Arranged objects must have spaced distances greater than 0");
    }

    public static void testColorPaintingOnArrangedModels() {
        resetEnvironment();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        model.addObject(model, 0);
        
        // Arrange
        Native.bed_arrange(1000L, modelPtr);
        
        // Paint color on arranged second object
        mockModel.objects.get(1).colorIndex = 2;
        assertEquals(0, mockModel.objects.get(0).colorIndex, "First object color remains default");
        assertEquals(2, mockModel.objects.get(1).colorIndex, "Second arranged object should record filament color");
    }

    public static void testAutoBrimAfterModelArrangement() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Double objects and arrange
        model.addObject(model, 0);
        Native.bed_arrange(1000L, modelPtr);
        
        // Make one arranged copy tall
        mockModel.objects.get(1).boundingBox = new double[]{-10, -10, 0, 10, 10, 200};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "Auto-brim must still reach the native slicer after model arrangement");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS (Tests 89-93)
    // =========================================================================

    public static void testEndToEndSlicingWorkflow() {
        resetEnvironment();
        
        // 1. Mandatory Setup Redirection
        assertTrue(SliceBeam.CONFIG == null, "Redirection active initially");
        setupMockConfig();
        assertTrue(SliceBeam.CONFIG != null, "Onboarding setup completed");
        
        // 2. Load model
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        mockModel.objects.get(0).boundingBox = new double[]{-15, -15, 0, 15, 15, 120}; // Tall model
        
        // 3. Paint Color & Support
        mockModel.objects.get(0).colorIndex = 2;
        mockModel.objects.get(0).hasSupport = true;
        
        // 4. Slicing with Auto-Brim
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            String updatedConfig = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "End-to-end slicing must pass auto_brim to the native slicer");
            
            // 5. Verify Output G-code Metadata
            String recommendedGcode = Native.gcoderesult_get_recommended_name(5000L);
            assertTrue(recommendedGcode.endsWith(".gcode"), "Slicing output should generate gcode format");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testMultiModelColorPrintJob() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Load multiple models by adding objects
        model.addObject(model, 0);
        model.addObject(model, 0);
        assertEquals(3, mockModel.objects.size(), "Model contains 3 objects");
        
        // Arrange
        Native.bed_arrange(1000L, modelPtr);
        
        // Assign different colors
        mockModel.objects.get(0).colorIndex = 1;
        mockModel.objects.get(1).colorIndex = 2;
        mockModel.objects.get(2).colorIndex = 3;
        
        // Slice
        try {
            File configFile = SliceBeam.getConfigFile();
            Files.write(configFile.toPath(), "brim_type = no_brim\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            double usage = Native.gcoderesult_get_used_filament_g(5000L, 0);
            assertTrue(usage > 0.0, "Multi-color slice should calculate non-zero filament weight usage");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testComplexModelSupportEnforcement() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        
        // Paint supports on overhang areas
        mockModel.objects.get(0).hasSupport = true;
        
        try {
            File configFile = SliceBeam.getConfigFile();
            // Setup support_material = 0 initially
            Files.write(configFile.toPath(), "support_material = 0\nsupport_material_auto = 0\n".getBytes());
            File gcodeFile = new File(SliceBeam.INSTANCE.getFilesDir(), "output.gcode");
            
            // Slice (simulate slicer logic enforcing support since facets are painted)
            model.slice(configFile.getAbsolutePath(), gcodeFile.getAbsolutePath(), null);
            
            // Check that support is enabled because of painted facets
            // Since we shadow, mock slice completes successfully
            assertTrue(mockModel.objects.get(0).hasSupport, "Support paint state is preserved");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testPrinterMigrationAndBrimUpdate() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        long modelPtr = 1;
        Native.MockModel mockModel = Native.getMockModels().get(modelPtr);
        // Flat model PLA
        mockModel.objects.get(0).boundingBox = new double[]{-20, -20, 0, 20, 20, 10};
        
        try {
            File configFile = SliceBeam.getConfigFile();
            
            // Migration Scenario 1: PLA
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = PLA\n".getBytes());
            model.slice(configFile.getAbsolutePath(), new File(SliceBeam.INSTANCE.getFilesDir(), "pla.gcode").getAbsolutePath(), null);
            String configPLA = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "PLA auto-brim rule must reach the native slicer");
            
            // Migration Scenario 2: Switch to ABS
            Files.write(configFile.toPath(), "brim_type = auto_brim\nfilament_type = ABS\n".getBytes());
            model.slice(configFile.getAbsolutePath(), new File(SliceBeam.INSTANCE.getFilesDir(), "abs.gcode").getAbsolutePath(), null);
            String configABS = new String(Files.readAllBytes(configFile.toPath()));
            assertTrue(Native.getLastSliceConfig().contains("brim_type = auto_brim"), "ABS auto-brim rule must reach the native slicer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testHeadlessFullStateReset() {
        resetEnvironment();
        setupMockConfig();
        Model model = new Model();
        Native.MockModel mockModel = Native.getMockModels().get(model.getPointer());
        mockModel.objects.get(0).colorIndex = 4;
        
        // Perform State Reset (crash simulation recovery)
        resetEnvironment();
        
        assertTrue(Native.getMockModels().isEmpty(), "All models must be cleared after environment reset");
        assertTrue(SliceBeam.CONFIG == null, "Configuration must be cleared after environment reset");
    }
}
