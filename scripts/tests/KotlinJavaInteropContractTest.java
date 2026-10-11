package scripts.tests;

import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.animation.TimeInterpolator;
import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import ru.ytkab0bp.slicebeam.components.UnfoldMenu;
import ru.ytkab0bp.slicebeam.components.WebViewMenu;
import ru.ytkab0bp.slicebeam.components.SvgImportGuard;
import ru.ytkab0bp.slicebeam.components.CableClipGenerator;
import ru.ytkab0bp.slicebeam.components.CableGrommetGenerator;
import ru.ytkab0bp.slicebeam.components.PhoneStandGenerator;
import ru.ytkab0bp.slicebeam.components.PlanterPotGenerator;
import ru.ytkab0bp.slicebeam.components.PlantLabelSetGenerator;
import ru.ytkab0bp.slicebeam.components.TextTagGenerator;
import ru.ytkab0bp.slicebeam.components.UtilityBoxGenerator;
import ru.ytkab0bp.slicebeam.components.PowerStripTrayGenerator;
import ru.ytkab0bp.slicebeam.components.WallToolHolderGenerator;
import ru.ytkab0bp.slicebeam.components.FastenerGenerator;
import ru.ytkab0bp.slicebeam.components.LitoGenGenerator;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rRuntimeError;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper;
import ru.ytkab0bp.slicebeam.config.ConfigObject;
import ru.ytkab0bp.slicebeam.BeamServerData;
import ru.ytkab0bp.slicebeam.cloud.CloudAPI;
import ru.ytkab0bp.slicebeam.cloud.CloudController;
import ru.ytkab0bp.sapil.APICallback;
import ru.ytkab0bp.sapil.APIEnclosure;
import ru.ytkab0bp.sapil.APILibrary;
import ru.ytkab0bp.sapil.APILibraryConfig;
import ru.ytkab0bp.sapil.APIRunner;
import ru.ytkab0bp.sapil.APIRequestHandle;
import ru.ytkab0bp.sapil.RequestType;
import ru.ytkab0bp.slicebeam.SliceBeam;
import ru.ytkab0bp.slicebeam.render.Camera;
import ru.ytkab0bp.slicebeam.render.CoordAxes;
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult;
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer;
import ru.ytkab0bp.slicebeam.slic3r.Bed3D;
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef;
import ru.ytkab0bp.slicebeam.slic3r.GLModel;
import ru.ytkab0bp.slicebeam.slic3r.GLShaderProgram;
import ru.ytkab0bp.slicebeam.slic3r.GLShadersManager;
import ru.ytkab0bp.slicebeam.slic3r.Model;
import ru.ytkab0bp.slicebeam.slic3r.Native;
import ru.ytkab0bp.slicebeam.slic3r.PaintSession;
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef;
import ru.ytkab0bp.slicebeam.slic3r.SliceListener;
import ru.ytkab0bp.slicebeam.slic3r.NativeEnumValueSanitizer;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rUtils;
import ru.ytkab0bp.slicebeam.boot.AppBoot;
import ru.ytkab0bp.slicebeam.boot.BootTask;
import ru.ytkab0bp.slicebeam.boot.BeamServerDataTask;
import ru.ytkab0bp.slicebeam.boot.CheckUpdateJsonTask;
import ru.ytkab0bp.slicebeam.boot.CloudCachedInitTask;
import ru.ytkab0bp.slicebeam.boot.CloudInitTask;
import ru.ytkab0bp.slicebeam.boot.ClearModelCacheTask;
import ru.ytkab0bp.slicebeam.boot.EventBusTask;
import ru.ytkab0bp.slicebeam.boot.LoadSlic3rConfigTask;
import ru.ytkab0bp.slicebeam.boot.TrueTimeTask;
import ru.ytkab0bp.slicebeam.boot.PrefsTask;
import ru.ytkab0bp.slicebeam.boot.PrintConfigWarmupTask;
import ru.ytkab0bp.slicebeam.boot.VibrationUtilsTask;
import ru.ytkab0bp.slicebeam.events.BeamServerDataUpdatedEvent;
import ru.ytkab0bp.slicebeam.events.CloudUserInfoUpdatedEvent;
import ru.ytkab0bp.slicebeam.events.CloudFeaturesUpdatedEvent;
import ru.ytkab0bp.slicebeam.events.CloudLoginStateUpdatedEvent;
import ru.ytkab0bp.slicebeam.events.CloudModelsRemainingCountUpdatedEvent;
import ru.ytkab0bp.slicebeam.events.CloudSyncFinishedEvent;
import ru.ytkab0bp.slicebeam.events.FlattenModeResetEvent;
import ru.ytkab0bp.slicebeam.events.LongClickTranslationEvent;
import ru.ytkab0bp.slicebeam.events.NeedDismissAIGeneratorMenu;
import ru.ytkab0bp.slicebeam.events.NeedDismissCalibrationsMenu;
import ru.ytkab0bp.slicebeam.events.NeedDismissSnackbarEvent;
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent;
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent;
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent;
import ru.ytkab0bp.slicebeam.smartslice.GeometryAnalysis;
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceEngine;
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceIntent;
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceResult;
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout;
import ru.ytkab0bp.slicebeam.utils.FillBedPlanner;
import ru.ytkab0bp.slicebeam.utils.DebugUtils;
import ru.ytkab0bp.slicebeam.utils.DoubleMatrix;
import ru.ytkab0bp.slicebeam.utils.FilamentPaletteSyncPolicy;
import ru.ytkab0bp.slicebeam.utils.FilamentSlot;
import ru.ytkab0bp.slicebeam.utils.FilamentTypes;
import ru.ytkab0bp.slicebeam.utils.FastenerStandards;
import ru.ytkab0bp.slicebeam.utils.FastenerStlWriter;
import ru.ytkab0bp.slicebeam.utils.GCodeIntegrityValidator;
import ru.ytkab0bp.slicebeam.utils.GCodeToolMapper;
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync;
import ru.ytkab0bp.slicebeam.utils.CssColors;
import ru.ytkab0bp.slicebeam.utils.ConverterStorage;
import ru.ytkab0bp.slicebeam.utils.LithophaneInput;
import ru.ytkab0bp.slicebeam.utils.LithophaneToneMapper;
import ru.ytkab0bp.slicebeam.utils.PrinterWebOriginPolicy;
import ru.ytkab0bp.slicebeam.utils.Prefs;
import ru.ytkab0bp.slicebeam.utils.RandomUtils;
import ru.ytkab0bp.slicebeam.utils.ThreadLocalDoubleArray;
import ru.ytkab0bp.slicebeam.utils.UploadFileValidator;
import ru.ytkab0bp.slicebeam.utils.IOUtils;
import ru.ytkab0bp.slicebeam.utils.ViewUtils;
import ru.ytkab0bp.slicebeam.utils.VibrationUtils;
import ru.ytkab0bp.slicebeam.utils.Vec3d;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.io.IOException;
import org.json.JSONObject;
import com.instacart.truetime.time.TrueTimeImpl;
import ru.ytkab0bp.eventbus.EventBus;

public final class KotlinJavaInteropContractTest {
    public static void main(String[] args) throws ReflectiveOperationException, IOException {
        assertExtensibleMarkerEvents(
                BeamServerDataUpdatedEvent.class,
                CloudUserInfoUpdatedEvent.class,
                CloudFeaturesUpdatedEvent.class,
                CloudLoginStateUpdatedEvent.class,
                CloudModelsRemainingCountUpdatedEvent.class,
                CloudSyncFinishedEvent.class,
                FlattenModeResetEvent.class,
                NeedDismissAIGeneratorMenu.class,
                NeedDismissCalibrationsMenu.class,
                ObjectsListChangedEvent.class,
                SelectedObjectChangedEvent.class);
        assertEventPayloadContracts();
        assertSmartSliceContracts();
        assertUtilityAndJniContracts();
        assertViewAndWebMenuContracts();
        assertConfigMetadataContracts();
        assertPrefsJavaContract();
        assertProfileUtilityContracts();
        assertNativeResourceWrapperContracts();
        assertModelJavaContract();
        assertBedAndGlModelContracts();
        assertCameraAndCoordAxesContracts();
        assertShaderContracts();
        assertLocalizationContracts();
        assertConverterStorageContracts();
        assertSvgImportGuardContracts();
        assertSmallMakerGeneratorContracts();
        assertFastenerContracts();
        assertMmuFilamentSyncContracts();
        assertBeamServerDataContracts();
        assertSliceBeamJavaContract();
        assertSlic3rConfigWrapperContract();
        assertCloudAPIJavaContract();
        assertSapilKotlinContracts();
        assertCloudControllerJavaContract();
        assertGCodeViewerContracts();
        assertIOUtilsContracts();
        assertRemainingUiAndRenderInteropContracts();
        if (FillBedPlanner.copyAttemptsForLimit(3, 8) != 5) {
            throw new AssertionError("Java callers must retain the static FillBedPlanner method");
        }
        if (UploadFileValidator.isReadyForUpload(null)) {
            throw new AssertionError("The Kotlin upload validator must reject null Java inputs");
        }
        if (RandomUtils.RANDOM == null || RandomUtils.randomf(2f, 3f) < 2f || RandomUtils.randomf(2f, 3f) >= 3f) {
            throw new AssertionError("Java callers must retain the RandomUtils static field and methods");
        }
        if (RandomUtils.randoml(10L, 11L) != 10L) {
            throw new AssertionError("RandomUtils.randoml must preserve its one-value interval behavior");
        }
        if (FilamentTypes.ALL.length < 70 || !"PLA".equals(FilamentTypes.ALL[0])
                || !Arrays.asList(FilamentTypes.ALL).contains("PEEK")) {
            throw new AssertionError("FilamentTypes.ALL must remain a public Java static array with the full material catalog");
        }
        if (!"T1 ; tool change".equals(GCodeToolMapper.remapToolLine("T2 ; tool change", new int[]{0, 1, 1}))) {
            throw new AssertionError("GCodeToolMapper Java static routing API changed");
        }
        List<FilamentSlot> syncedSlots = FilamentPaletteSyncPolicy.updateExisting(
                Arrays.asList(new FilamentSlot(0xFF111111, "PETG")),
                Arrays.asList(new FilamentSlot(0xFF222222, "PLA")));
        if (syncedSlots.size() != 1 || !"PLA".equals(syncedSlots.get(0).type)) {
            throw new AssertionError("FilamentPaletteSyncPolicy Java static API changed");
        }
        GCodeIntegrityValidator.Result invalidGcode = GCodeIntegrityValidator.validate(null);
        if (invalidGcode.failure != GCodeIntegrityValidator.Failure.INVALID_FILE
                || invalidGcode.expectedLayers != 0 || invalidGcode.actualLayers != 0 || invalidGcode.isValid()) {
            throw new AssertionError("GCodeIntegrityValidator nested Java result fields or methods changed");
        }
        if (!"http://printer.local/".equals(PrinterWebOriginPolicy.dashboardUrl("printer.local:7125"))
                || !PrinterWebOriginPolicy.allows("http://printer.local", "http://printer.local/status")) {
            throw new AssertionError("PrinterWebOriginPolicy Java static API changed");
        }
        if (Math.abs(LithophaneInput.parseClamped("1,25", 2f, .4f, 10f) - 1.25f) > 0.0001f
                || LithophaneToneMapper.gray(0f, 0f, 0f, 1f, 0f, 1f, 0, true) != 1f) {
            throw new AssertionError("Lithophane input and tone-mapping static APIs changed");
        }
        if (FastenerStandards.count() != 9 || FastenerStandards.coarsePitch(0) != .4f
                || !"12p5".equals(FastenerStandards.fileNumber(12.5f))) {
            throw new AssertionError("FastenerStandards Java static API changed");
        }
        if (CssColors.ALL.size() != 141 || CssColors.ALL.get("red") != 0xFFFF0000
                || !"black".equals(new ArrayList<>(CssColors.ALL.keySet()).get(0))
                || !"rebeccapurple".equals(new ArrayList<>(CssColors.ALL.keySet()).get(140))
                || CssColors.COMMON.length != 18) {
            throw new AssertionError("CssColors must keep its ordered public Java map and swatch array");
        }
        Vec3d vector = new Vec3d(1, 2, 3);
        Vec3d copy = new Vec3d(vector);
        Vec3d center = new Vec3d(0, 2, 4).center(new Vec3d(2, 4, 6));
        Vec3d normalized = new Vec3d(3, 4, 0).normalize();
        Vec3d cross = new Vec3d(1, 0, 0).crossProduct(new Vec3d(0, 1, 0));
        Vec3d divided = new Vec3d(8, 12, 20).divide(new Vec3d(2, 3, 4));
        Vec3d defaultVector = new Vec3d();
        if (defaultVector.x != 0 || defaultVector.y != 0 || defaultVector.z != 0) {
            throw new AssertionError("Vec3d default constructor must initialize all public fields to zero");
        }
        if (copy.x != 1 || copy.y != 2 || copy.z != 3 || vector.clone() == vector
                || center.x != 1 || center.y != 3 || center.z != 5
                || Math.abs(vector.distance(new Vec3d(4, 6, 3)) - 5) > 0.0001
                || Math.abs(normalized.magnitude() - 1) > 0.0001
                || Math.abs(normalized.x - 0.6) > 0.0001 || Math.abs(normalized.y - 0.8) > 0.0001
                || cross.x != 0 || cross.y != 0 || cross.z != 1
                || divided.x != 4 || divided.y != 4 || divided.z != 5) {
            throw new AssertionError("Vec3d constructors, public fields or geometry methods changed");
        }
        if (vector.add(1, 1, 1) != vector || vector.multiply(2) != vector
                || !Arrays.equals(vector.asArray(), new float[]{4, 6, 8, 1})
                || !Arrays.equals(vector.asDoubleArray(), new double[]{4, 6, 8, 1})) {
            throw new AssertionError("Vec3d Java mutation or array APIs changed");
        }
        Vec3d mutation = new Vec3d().set(copy).add(new Vec3d(1, 2, 3)).multiply(2, 3, 4).negate();
        if (mutation.x != -4 || mutation.y != -12 || mutation.z != -24
                || new Vec3d(3, 0, 4).magnitude() != 5) {
            throw new AssertionError("Vec3d set, vector add, component multiplication, negate or magnitude changed");
        }

        ThreadLocalDoubleArray values = new ThreadLocalDoubleArray(4);
        if (values.get().length != 4 || values.get() != values.get()) {
            throw new AssertionError("ThreadLocalDoubleArray must keep its public constructor and per-thread value");
        }

        Throwable cause = new IllegalStateException("native failure");
        Slic3rRuntimeError error = new Slic3rRuntimeError("slice failed", cause);
        if (!"slice failed".equals(error.getMessage()) || error.getCause() != cause) {
            throw new AssertionError("Slic3rRuntimeError Java constructor overloads changed");
        }
        new Slic3rRuntimeError();
        new Slic3rRuntimeError("message");
        new Slic3rRuntimeError(cause);

        assertBootTask(new PrefsTask(), false, false, "preferences initialization");
        assertBootTask(new PrintConfigWarmupTask(), true, false, "print config warmup");
        assertBootTask(new VibrationUtilsTask(), true, false, "vibration initialization");
        assertBootTask(new ClearModelCacheTask(), true, true, "model cache cleanup");
        assertBootTask(new CheckUpdateJsonTask(), true, false, "update metadata check");
        CloudCachedInitTask cloudCached = new CloudCachedInitTask();
        CloudInitTask cloudInit = new CloudInitTask();
        EventBusTask eventBus = new EventBusTask();
        BeamServerDataTask serverData = new BeamServerDataTask();
        LoadSlic3rConfigTask config = new LoadSlic3rConfigTask();
        assertBootTask(cloudCached, true, false, "cached cloud initialization");
        assertBootTask(cloudInit, true, true, "cloud initialization");
        assertBootTask(eventBus, true, false, "event bus registration");
        assertBootTask(serverData, true, false, "printer server data");
        assertBootTask(config, true, false, "slicer config loading");
        assertTaskDependencies(cloudCached, PrefsTask.class);
        assertTaskDependencies(cloudInit, PrefsTask.class, TrueTimeTask.class,
                LoadSlic3rConfigTask.class, CloudCachedInitTask.class);
        assertTaskDependencies(eventBus);
        assertTaskDependencies(serverData);
        assertTaskDependencies(config);

        Runnable startup = () -> {};
        List<Class<?>> startupDependencies = Arrays.asList(PrefsTask.class, CloudInitTask.class);
        BootTask baseTask = new BootTask(startup);
        BootTask dependentTask = new BootTask(startupDependencies, startup);
        if (baseTask.run != startup || !baseTask.dependencies.equals(Collections.emptyList())
                || dependentTask.run != startup || dependentTask.dependencies != startupDependencies
                || baseTask.workerThread || baseTask.nonCritical || baseTask.priority != 0
                || baseTask.onWorker() != baseTask || !baseTask.workerThread || baseTask.priority != -20
                || baseTask.onWorker(7) != baseTask || baseTask.priority != 7) {
            throw new AssertionError("BootTask constructors, public fields or fluent worker policy changed");
        }
        TrueTimeTask trueTime = new TrueTimeTask();
        assertBootTask(trueTime, true, true, "time synchronization");
        assertTaskDependencies(trueTime);
        assertAppBootScheduler();
        System.out.println("KotlinJavaInteropContractTest passed: Java APIs, JNI callbacks, profile compatibility, event payloads, smart-slice DTOs, and boot scheduling contracts are preserved.");
    }

    private static void assertRemainingUiAndRenderInteropContracts() throws ReflectiveOperationException {
        Class<?> mainActivity = Class.forName("ru.ytkab0bp.slicebeam.MainActivity");
        if (!"ru.ytkab0bp.slicebeam.navigation.NavigationDelegate"
                .equals(mainActivity.getMethod("getNavigationDelegate").getReturnType().getName())) {
            throw new AssertionError("MainActivity must preserve its Java navigationDelegate getter");
        }

        Class<?> activity = Class.forName("android.app.Activity");
        Class<?> converter = Class.forName("ru.ytkab0bp.slicebeam.components.ImageToStlConverter");
        if (!Modifier.isStatic(converter.getMethod("pickImage", activity).getModifiers())) {
            throw new AssertionError("ImageToStlConverter.pickImage must remain Java-static");
        }
        Class<?> bed = Class.forName("ru.ytkab0bp.slicebeam.fragment.BedFragment");
        Class<?> loadCallback = Class.forName("ru.ytkab0bp.slicebeam.fragment.BedFragment$ModelLoadCallback");
        if (!loadCallback.isInterface()
                || !Modifier.isPublic(bed.getMethod("loadModel", Class.forName("java.io.File"),
                        boolean.class, int.class, loadCallback).getModifiers())) {
            throw new AssertionError("BedFragment must retain its public multi-plate model-import callback");
        }

        Class<?> lithophaneWriter = Class.forName("ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter");
        if (!Modifier.isStatic(lithophaneWriter.getMethod("writePanel", Class.forName("java.io.File"),
                float[].class, int.class, int.class, float.class, float.class).getModifiers())) {
            throw new AssertionError("LithophaneStlWriter.writePanel must remain Java-static");
        }

        Class<?> glView = Class.forName("ru.ytkab0bp.slicebeam.view.GLView");
        Class<?> renderer = Class.forName("ru.ytkab0bp.slicebeam.render.GLRenderer");
        glView.getMethod("getRenderer");
        renderer.getMethod("onDrawFrame", Class.forName("javax.microedition.khronos.opengles.GL10"));
        Class.forName("ru.ytkab0bp.slicebeam.SetupActivity").getDeclaredConstructor();
    }

    private static void assertProfileUtilityContracts() {
        if (!"0.4".equals(Slic3rUtils.extractNozzle("Anycubic Kobra 3 V2 0.4 nozzle"))
                || !"Anycubic Kobra 3 V2".equals(Slic3rUtils.stripNozzleSuffix("Anycubic Kobra 3 V2 0.4 nozzle"))
                || !"0.4".equals(Slic3rUtils.firstNozzleDiameter("0.4;0.6"))
                || !Slic3rUtils.layerHeightFitsNozzle("0.28", "0.4")
                || Slic3rUtils.layerHeightFitsNozzle("0.56", "0.4")
                || !Slic3rUtils.lineWidthFitsNozzle("0.5", "0.4")
                || Slic3rUtils.lineWidthFitsNozzle("0.7", "0.4")) {
            throw new AssertionError("Slic3rUtils nozzle extraction or profile filtering changed");
        }

        List<String> parsed = Slic3rUtils.parseStringList("  A ; \"B;C\";\"D\\\"E\"  ");
        if (!parsed.equals(Arrays.asList("A", " B;C", "D\"E  "))) {
            throw new AssertionError("Slic3rUtils serialized string-list quoting or escaping changed: " + parsed);
        }
        if (!Slic3rUtils.isPrinterCompatible("Process 0.4 nozzle", "Anycubic Kobra 3 V2 0.4 nozzle",
                        "", "Anycubic Kobra 3 V2", "Anycubic Kobra 3 V2", "0.4", null)
                || Slic3rUtils.isPrinterCompatible("Process 0.6 nozzle", "Anycubic Kobra 3 V2",
                        "", "Anycubic Kobra 3 V2", "Anycubic Kobra 3 V2", "0.4", null)
                || !Slic3rUtils.isPrinterCompatible("Generic process", "", "", "Printer", "Printer", null, null)
                || !Slic3rUtils.isCompatible("Generic PLA;Generic PETG", "", "Generic PLA", null)
                || Slic3rUtils.isCompatible("Generic PLA;Generic PETG", "", "Other", null)) {
            throw new AssertionError("Slic3rUtils explicit preset/model/nozzle compatibility changed");
        }

        if (!"top".equals(NativeEnumValueSanitizer.normalize(
                        "unsupported", new String[]{"top", "solid"}, "top"))
                || !"unsupported".equals(NativeEnumValueSanitizer.normalize(
                        "unsupported", new String[0], "top"))) {
            throw new AssertionError("NativeEnumValueSanitizer normalization changed");
        }

        try {
            Slic3rUtils.class.getConstructor();
            Slic3rUtils.ConfigChecker.class.getConstructor(String.class);
            Slic3rUtils.ConfigChecker.class.getMethod("checkCompatibility", String.class);
            if (!Arrays.asList(Slic3rUtils.ConfigChecker.class.getMethod("eval", String.class).getExceptionTypes())
                    .contains(Slic3rRuntimeError.class)) {
                throw new AssertionError("Slic3rUtils.ConfigChecker.eval must keep its declared checked exception");
            }
            if (!Modifier.isStatic(NativeEnumValueSanitizer.class.getMethod(
                    "normalize", String.class, String[].class, String.class).getModifiers())
                    || NativeEnumValueSanitizer.class.getConstructors().length != 0) {
                throw new AssertionError("NativeEnumValueSanitizer must keep its static API and hidden constructor");
            }
            Slic3rUtils.class.getMethod("calcViewNormalMatrix", double[].class, double[].class, double[].class);
            Slic3rUtils.class.getMethod("unproject", double[].class, double[].class,
                    int.class, int.class, double.class, double.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Slic3rUtils or native profile-checker Java signatures changed", e);
        }
    }

    private static void assertNativeResourceWrapperContracts() {
        Native.resetMockState();
        GCodeProcessorResult result = new GCodeProcessorResult(new java.io.File("sample.gcode"));
        if (result.pointer != 5000L || !"sample.gcode".equals(Native.getLastGCodeName())
                || !new java.io.File("sample.gcode").getAbsolutePath().equals(Native.getLastGCodePath())
                || !"mock_slice.gcode".equals(result.getRecommendedName())
                || result.getUsedFilamentMM(2) != 1500.0
                || result.getUsedFilamentG(3) != 4.5) {
            throw new AssertionError("GCodeProcessorResult must forward file metadata, role queries and native values");
        }
        if (Native.getLastGCodeMMRole() != 2 || Native.getLastGCodeGRole() != 3) {
            throw new AssertionError("GCodeProcessorResult changed the requested extrusion role");
        }
        result.release();
        result.release();
        if (result.pointer != 0L || Native.getGCodeReleaseCalls() != 1 || Native.getLastGCodeReleasedPtr() != 5000L) {
            throw new AssertionError("GCodeProcessorResult must release its native pointer exactly once");
        }

        try {
            GCodeProcessorResult.class.getConstructor(java.io.File.class);
            GCodeProcessorResult.class.getDeclaredConstructor(long.class);
            if (!Modifier.isProtected(GCodeProcessorResult.class.getDeclaredMethod("finalize").getModifiers())
                    || !Arrays.asList(GCodeProcessorResult.class.getDeclaredMethod("finalize").getExceptionTypes())
                    .contains(Throwable.class)) {
                throw new AssertionError("GCodeProcessorResult must preserve its finalizer contract");
            }
            PaintSession.class.getConstructor(Model.class, int.class, int.class);
            PaintSession.class.getMethod("raycast", double[].class, double[].class);
            PaintSession.class.getMethod("brush", double[].class, int.class, double.class, int.class,
                    double[].class, boolean.class);
            PaintSession.class.getMethod("bucket", double[].class, int.class, int.class, boolean.class, double.class);
            PaintSession.class.getMethod("height", double.class, double.class, int.class);
            PaintSession.class.getMethod("buildOverlay", GLModel.class, int.class);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Native Slicer resource wrappers lost a Java-callable constructor or method", error);
        }

        Native.resetMockState();
        Model model = new Model();
        PaintSession session = new PaintSession(model, 3, 2);
        if (!session.isValid() || Native.getLastPaintModelPtr() != 1L
                || Native.getLastPaintObjectIndex() != 3 || Native.getLastPaintMode() != 2) {
            throw new AssertionError("PaintSession must bind the native session to the selected model object and mode");
        }
        double[] origin = {1.0, 2.0, 3.0};
        double[] direction = {0.0, 0.0, -1.0};
        if (!Arrays.equals(session.raycast(origin, direction), new double[]{4.0, 5.0, 6.0, 7.0})) {
            throw new AssertionError("PaintSession raycast must return native mesh hit data");
        }
        double[] hit = {8.0, 9.0, 10.0, 11.0};
        double[] camera = {12.0, 13.0, 14.0};
        session.brush(hit, 6, 2.5, 4, camera, true);
        if (Native.getLastPaintFacetStart() != 6 || Native.getLastPaintRadius() != 2.5
                || Native.getLastPaintFilament() != 4 || !Native.getLastPaintSphere()) {
            throw new AssertionError("PaintSession brush arguments must reach the native selector unchanged");
        }
        session.bucket(hit, 7, 3, true, 42.0);
        if (Native.getLastPaintFacetStart() != 7 || Native.getLastPaintFilament() != 3
                || !Native.getLastPaintPropagate() || Native.getLastPaintAngle() != 42.0) {
            throw new AssertionError("PaintSession bucket arguments must reach the native selector unchanged");
        }
        session.height(1.25, 6.5, 5);
        if (Native.getLastPaintZMin() != 1.25 || Native.getLastPaintZMax() != 6.5
                || Native.getLastPaintFilament() != 5) {
            throw new AssertionError("PaintSession height-paint arguments must reach the native selector unchanged");
        }
        session.clear();
        session.commit();
        if (session.buildOverlay(new GLModel(), 6) != 29 || Native.getLastPaintFilament() != 6) {
            throw new AssertionError("PaintSession overlay must use the native painting mesh and return its triangle count");
        }
        session.end();
        session.end();
        if (session.isValid() || Native.getPaintEndCalls() != 1
                || Native.getLastPaintEndedPtr() != 9000L
                || !Native.getNativeLifecycleEvents().equals(Arrays.asList(
                        "paint_begin", "paint_raycast", "paint_brush", "paint_bucket", "paint_height",
                        "paint_clear", "paint_commit", "glmodel_init_from_paint", "paint_end"))) {
            throw new AssertionError("PaintSession must preserve native call order and release its session exactly once");
        }
        model.release();

        Native.resetMockState();
        Native.setNextPaintSessionPtr(0L);
        Model invalidModel = new Model();
        PaintSession invalidSession = new PaintSession(invalidModel, 0, 0);
        invalidSession.end();
        if (invalidSession.isValid() || Native.getPaintEndCalls() != 0) {
            throw new AssertionError("An invalid PaintSession must not release a null native pointer");
        }
        invalidModel.release();
    }

    private static void assertBedAndGlModelContracts() {
        Native.resetMockState();
        GLModel glModel = new GLModel();
        Model source = new Model();
        if (glModel.pointer != 1L || glModel.hoverProgress != 0f || glModel.isHovering
                || !glModel.isInitialized() || glModel.isEmpty()) {
            throw new AssertionError("GLModel must preserve its native handle, public state fields and query methods");
        }
        glModel.initFrom(source);
        glModel.initFrom(source, 0);
        glModel.initBoundingBox(source, 0);
        glModel.initBox(10f, 20f, 30f);
        glModel.initBackgroundTriangles();
        glModel.stilizedArrow(1f, 2f, 3f, 4f);
        if (glModel.initFromPaint(71L, 2) != 29) {
            throw new AssertionError("GLModel painted-overlay initialization must return the native triangle count");
        }
        GLModel.MeshRaycaster originalRaycaster = glModel.getRaycaster();
        if (originalRaycaster != glModel.getRaycaster()) {
            throw new AssertionError("GLModel must lazily cache its mesh raycaster");
        }
        glModel.reset();
        if (originalRaycaster == glModel.getRaycaster()) {
            throw new AssertionError("GLModel.reset must invalidate its cached raycaster");
        }
        GLModel.HitResult hit = new GLModel.HitResult(new Vec3d(1, 2, 3), new Vec3d(0, 0, 1));
        if (hit.position.x != 1 || hit.position.y != 2 || hit.position.z != 3
                || hit.normal.x != 0 || hit.normal.y != 0 || hit.normal.z != 1) {
            throw new AssertionError("GLModel.HitResult must retain its public position and normal values");
        }
        glModel.release();
        glModel.release();
        source.release();
        if (glModel.pointer != 0L || Native.getGlModelReleaseCalls() != 1
                || Native.getLastGlModelReleasedPtr() != 1L) {
            throw new AssertionError("GLModel.release must clear and release its native pointer exactly once");
        }

        Native.resetMockState();
        Bed3D bed = new Bed3D();
        bed.configure(new java.io.File("printer.json"));
        Vec3d minimum = bed.getVolumeMin();
        Vec3d maximum = bed.getVolumeMax();
        if (!bed.isValid() || minimum == null || maximum == null
                || minimum.x != -125 || minimum.y != -125 || minimum.z != 0
                || maximum.x != 125 || maximum.y != 125 || maximum.z != 200
                || Native.getMockBeds().size() != 1
                || !new java.io.File("printer.json").getAbsolutePath().equals(
                        Native.getMockBeds().values().iterator().next().configPath)) {
            throw new AssertionError("Bed3D must configure its native bed and cache the six volume bounds");
        }
        Model arrangeModel = new Model();
        if (!bed.arrange(arrangeModel) || bed.getRaycaster() == null) {
            throw new AssertionError("Bed3D must preserve native arrangement and triangle raycaster access");
        }
        bed.release();
        bed.release();
        if (Native.getBedReleaseCalls() != 1 || Native.getLastBedReleasedPtr() != 2L
                || Native.getGlModelReleaseCalls() != 1 || Native.getLastGlModelReleasedPtr() != 1L) {
            throw new AssertionError("Bed3D.release must dispose its native bed and axes once, retaining native mesh ownership");
        }
        arrangeModel.release();

        try {
            GLModel.class.getDeclaredConstructor(long.class);
            GLModel.class.getConstructor();
            GLModel.class.getConstructor(Model.class);
            GLModel.class.getMethod("initFrom", Model.class);
            GLModel.class.getMethod("initFrom", Model.class, int.class);
            GLModel.class.getMethod("initFromPaint", long.class, int.class);
            GLModel.class.getMethod("initBox", float.class, float.class, float.class);
            GLModel.class.getMethod("getRaycaster");
            GLModel.MeshRaycaster.class.getMethod("raycast", ru.ytkab0bp.slicebeam.render.GLRenderer.class,
                    java.util.ArrayList.class, float.class, float.class);
            GLModel.HitResult.class.getConstructor(Vec3d.class, Vec3d.class);
            Bed3D.class.getConstructor();
            Bed3D.class.getMethod("configure", java.io.File.class);
            Bed3D.class.getMethod("arrange", Model.class);
            Bed3D.class.getMethod("render", GLShadersManager.class, boolean.class, double[].class,
                    double[].class, float.class);
            Bed3D.class.getMethod("render", GLShadersManager.class, boolean.class, double[].class,
                    double[].class, float.class, boolean.class);
            if (Modifier.isFinal(GLModel.class.getModifiers()) || Modifier.isFinal(Bed3D.class.getModifiers())
                    || Modifier.isFinal(GLModel.class.getMethod("release").getModifiers())
                    || Modifier.isFinal(Bed3D.class.getMethod("release").getModifiers())
                    || !Modifier.isProtected(Bed3D.class.getDeclaredMethod("finalize").getModifiers())
                    || !Arrays.asList(Bed3D.class.getDeclaredMethod("finalize").getExceptionTypes())
                            .contains(Throwable.class)) {
                throw new AssertionError("GLModel/Bed3D Java extensibility or finalizer contract changed");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("GLModel/Bed3D Java constructors, fields or render signatures changed", error);
        }
    }

    private static void assertModelJavaContract() {
        try {
            Model.class.getConstructor();
            Model.class.getConstructor(java.io.File.class);
            Model.class.getConstructor(java.io.File.class, int.class);
            Model.class.getConstructor(String.class);
            Model.class.getConstructor(String.class, int.class);
            Model.class.getMethod("getBoundingBoxExact", int.class, Vec3d.class, Vec3d.class);
            Model.class.getMethod("getBoundingBoxApprox", int.class, Vec3d.class, Vec3d.class);
            Model.class.getMethod("getBoundingBoxExactMin");
            Model.class.getMethod("getBoundingBoxExactMax");
            Model.class.getMethod("getBoundingBoxApproxMin");
            Model.class.getMethod("getBoundingBoxApproxMax");
            Model.class.getMethod("getGeometryRevision");
            Model.class.getMethod("getObjectsCount");
            Model.class.getMethod("getObjectName", int.class);
            Model.class.getMethod("getTriangleCount", int.class);
            Model.class.getMethod("getRequiredFilamentCount");
            Model.class.getMethod("cut", int.class, double.class, double.class, double.class,
                    boolean.class, boolean.class);
            Model.class.getMethod("addConnector", int.class, Vec3d.class, float.class, float.class,
                    int.class, double.class, double.class);
            Model.class.getMethod("addConnector", int.class, Vec3d.class, float.class, float.class, int.class);
            Model.class.getMethod("slice", String.class, String.class, SliceListener.class);
            Model.class.getMethod("slice", String.class, String.class, SliceListener.class,
                    int.class, double.class, double.class, double.class);
            Model.class.getMethod("export3mf", String.class, String.class);
            java.lang.reflect.Method merge = Model.class.getMethod("merge", Model[].class);
            java.lang.reflect.Method release = Model.class.getMethod("release");
            java.lang.reflect.Method finalize = Model.class.getDeclaredMethod("finalize");
            java.lang.reflect.Field key = Model.class.getField("key");
            java.lang.reflect.Field pointer = Model.class.getField("pointer");
            if (Modifier.isFinal(Model.class.getModifiers()) || Modifier.isFinal(release.getModifiers())
                    || Modifier.isFinal(finalize.getModifiers()) || !Modifier.isStatic(merge.getModifiers())
                    || key.getType() != String.class || !Modifier.isFinal(key.getModifiers())
                    || pointer.getType() != long.class
                    || !Arrays.asList(finalize.getExceptionTypes()).contains(Throwable.class)) {
                throw new AssertionError("Model must preserve its Java fields, extensibility, static merge and finalizer contract");
            }
            for (java.lang.reflect.Constructor<?> constructor : new java.lang.reflect.Constructor<?>[]{
                    Model.class.getConstructor(java.io.File.class),
                    Model.class.getConstructor(java.io.File.class, int.class),
                    Model.class.getConstructor(String.class),
                    Model.class.getConstructor(String.class, int.class),
            }) {
                if (!Arrays.asList(constructor.getExceptionTypes()).contains(Slic3rRuntimeError.class)) {
                    throw new AssertionError("Model import constructors must declare Slic3rRuntimeError");
                }
            }
            if (!Arrays.asList(Model.class.getMethod("slice", String.class, String.class, SliceListener.class)
                            .getExceptionTypes()).contains(Slic3rRuntimeError.class)
                    || !Arrays.asList(Model.class.getMethod("export3mf", String.class, String.class)
                            .getExceptionTypes()).contains(Slic3rRuntimeError.class)) {
                throw new AssertionError("Model slice/export must retain the checked native error contract");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Kotlin Model wrapper lost a Java/JNI call signature", error);
        }

        Native.resetMockState();
        Model model = new Model();
        if (model.pointer == 0L || model.pointer != model.getPointer() || model.key == null || model.key.isEmpty()
                || model.getGeometryRevision() != 0L || model.getObjectsCount() != 1) {
            throw new AssertionError("Model must initialize its native handle, stable key and cached geometry state");
        }
        Vec3d initialMinimum = model.getBoundingBoxExactMin();
        if (initialMinimum.x != -50.0 || initialMinimum.y != -50.0 || initialMinimum.z != 0.0) {
            throw new AssertionError("Model exact bounds must be read from the native object");
        }
        model.translate(0, 5.0, 0.0, 0.0);
        Vec3d translatedMinimum = model.getBoundingBoxExactMin();
        if (model.getGeometryRevision() != 1L || translatedMinimum.x != -45.0) {
            throw new AssertionError("Geometry edits must invalidate native bounding-box caches and advance revision");
        }
        model.setExtruder(0, 2);
        if (model.getRequiredFilamentCount() != 2) {
            throw new AssertionError("Required filament count must include each model object's base extruder");
        }
        Model second = new Model();
        Model merged = Model.merge(model, second);
        if (merged.getObjectsCount() != 2) {
            throw new AssertionError("Static model merge must combine the native model objects");
        }
        merged.release();
        second.release();
        model.release();
        model.release();
        if (model.pointer != 0L) {
            throw new AssertionError("Model release must clear the native handle and tolerate repeated cleanup");
        }
    }

    private static void assertCameraAndCoordAxesContracts() {
        Camera camera = new Camera();
        if (camera.position == null || camera.origin == null || camera.up == null
                || camera.position.x != 0.0 || camera.position.y != 0.0 || camera.position.z != 0.0
                || camera.origin.x != 0.0 || camera.origin.y != 0.0 || camera.origin.z != 0.0
                || camera.up.x != 0.0 || camera.up.y != 0.0 || camera.up.z != 1.0) {
            throw new AssertionError("Camera public mutable vectors or their defaults changed");
        }

        if (camera.getZoom() != 1f) {
            throw new AssertionError("Camera zoom must start at 1");
        }
        camera.zoom(25f);
        if (camera.getZoom() != 2f) {
            throw new AssertionError("Camera gesture zoom scaling changed");
        }
        camera.zoom(Float.MAX_VALUE);
        if (camera.getZoom() != 10f) {
            throw new AssertionError("Camera gesture zoom upper bound changed");
        }
        camera.zoom(-Float.MAX_VALUE);
        if (camera.getZoom() != 1f) {
            throw new AssertionError("Camera gesture zoom lower bound changed");
        }
        camera.setZoom(12f);
        if (camera.getZoom() != 12f) {
            throw new AssertionError("Camera direct setZoom must retain its unclamped behavior");
        }

        camera.setZoom(1f);
        camera.position.set(0.0, -100.0, 100.0);
        camera.origin.set(0.0, 0.0, 0.0);
        double rootHalf = Math.sqrt(0.5);
        Vec3d forward = camera.getDirForward();
        if (Math.abs(forward.x) > 1e-9 || Math.abs(forward.y - rootHalf) > 1e-9
                || Math.abs(forward.z + rootHalf) > 1e-9
                || camera.getDirForward() != forward) {
            throw new AssertionError("Camera forward direction or reusable scratch-vector behavior changed");
        }

        double[] matrix = camera.getViewModelMatrix();
        double[] initialMatrix = matrix.clone();
        if (matrix.length != 16 || camera.getViewModelMatrix() != matrix) {
            throw new AssertionError("Camera view matrix must remain a cached 4x4 array");
        }
        camera.move(25f, 0f);
        if (camera.position.x != 25.0 || camera.origin.x != 25.0
                || camera.getViewModelMatrix() != matrix || Arrays.equals(initialMatrix, matrix)) {
            throw new AssertionError("Camera screen-space panning or dirty-matrix refresh changed");
        }

        Vec3d screenMove = camera.calcScreenMovement(10f, 5f);
        if (screenMove.magnitude() <= 0.0) {
            throw new AssertionError("Camera screen movement should produce a world-space vector");
        }
        double orbitRadius = camera.position.distance(camera.origin);
        camera.rotateAround(25.0, 10.0);
        if (Math.abs(camera.position.distance(camera.origin) - orbitRadius) > 1e-6) {
            throw new AssertionError("Camera orbit must preserve its distance from the target");
        }

        try {
            if (Camera.class.getField("position").getType() != Vec3d.class
                    || Camera.class.getField("origin").getType() != Vec3d.class
                    || Camera.class.getField("up").getType() != Vec3d.class) {
                throw new AssertionError("Camera public vector field descriptors changed");
            }
            CoordAxes.class.getConstructor();
            if (CoordAxes.class.getField("origin").getType() != Vec3d.class) {
                throw new AssertionError("CoordAxes.origin must remain a public Vec3d field");
            }
            CoordAxes.class.getMethod("setStemLength", float.class);
            CoordAxes.class.getMethod("render", GLShadersManager.class, double[].class,
                    double[].class, float.class, float.class);
            CoordAxes.class.getMethod("release");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Camera or coordinate-axis Java entry points changed during migration", error);
        }
    }

    private static void assertShaderContracts() {
        try {
            if (GLShadersManager.class.getConstructor() == null
                    || GLShaderProgram.class.getConstructor(String.class) == null) {
                throw new AssertionError("Shader manager/program constructors changed");
            }
            if (!"background".equals(GLShadersManager.SHADER_BACKGROUND)
                    || !"gouraud_light".equals(GLShadersManager.SHADER_GOURAUD_LIGHT)
                    || !"beam_intro".equals(GLShadersManager.SHADER_BEAM_INTRO)) {
                throw new AssertionError("Shader IDs must remain Java-visible compile-time constants");
            }

            java.lang.reflect.Field pointer = GLShaderProgram.class.getField("pointer");
            if (pointer.getType() != long.class || !Modifier.isPublic(pointer.getModifiers())) {
                throw new AssertionError("GLShaderProgram.pointer must remain directly accessible to the manager");
            }

            GLShaderProgram.class.getMethod("startUsing");
            GLShaderProgram.class.getMethod("stopUsing");
            GLShaderProgram.class.getMethod("getUniformLocation", String.class);
            GLShaderProgram.class.getMethod("getAttribLocation", String.class);
            GLShaderProgram.class.getMethod("setUniform", String.class, boolean.class);
            GLShaderProgram.class.getMethod("setUniform", String.class, float.class);
            GLShaderProgram.class.getMethod("setUniformMatrix3fv", String.class, double[].class);
            GLShaderProgram.class.getMethod("setUniformMatrix3fv", String.class, float[].class);
            GLShaderProgram.class.getMethod("setUniformMatrix4fv", String.class, double[].class);
            GLShaderProgram.class.getMethod("setUniformMatrix4fv", String.class, float[].class);
            GLShaderProgram.class.getMethod("setUniformColor", String.class, int.class);
            GLShaderProgram.class.getMethod("setUniform4f", String.class, float[].class);
            GLShaderProgram.class.getMethod("setUniform3f", String.class, float[].class);
            GLShaderProgram.class.getMethod("setUniform2f", String.class, float[].class);
            GLShaderProgram.class.getMethod("getId");
            GLShaderProgram.class.getMethod("release");
            if (!GLShaderProgram.class.getMethod("setUniform4f", String.class, float[].class).isVarArgs()
                    || !GLShaderProgram.class.getMethod("setUniform3f", String.class, float[].class).isVarArgs()
                    || !GLShaderProgram.class.getMethod("setUniform2f", String.class, float[].class).isVarArgs()) {
                throw new AssertionError("Shader uniform vector setters must retain Java varargs signatures");
            }

            java.lang.reflect.Method nativePointer = GLShadersManager.class.getDeclaredMethod("getCurrentShaderPointer");
            if (!Modifier.isPrivate(nativePointer.getModifiers()) || !Modifier.isStatic(nativePointer.getModifiers())
                    || nativePointer.getReturnType() != long.class) {
                throw new AssertionError("JNI shader-current-pointer method must remain private, static and long-valued");
            }
            if (Modifier.isFinal(GLShaderProgram.class.getModifiers())
                    || Modifier.isFinal(GLShadersManager.class.getModifiers())
                    || Modifier.isFinal(GLShaderProgram.class.getMethod("release").getModifiers())
                    || Modifier.isFinal(GLShadersManager.class.getMethod("get", String.class).getModifiers())) {
                throw new AssertionError("Shader classes and public methods must remain extensible to Java subclasses");
            }
            java.lang.reflect.Method finalizer = GLShaderProgram.class.getDeclaredMethod("finalize");
            if (!Modifier.isProtected(finalizer.getModifiers()) || finalizer.getReturnType() != void.class) {
                throw new AssertionError("GLShaderProgram must retain its protected native-resource finalizer");
            }
            GLShadersManager.class.getMethod("clearShaders");
            GLShadersManager.class.getMethod("get", String.class);
            GLShadersManager.class.getMethod("getCurrentShader");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Shader Java/JNI entry points changed during Kotlin migration", error);
        }
    }

    private static void assertLocalizationContracts() {
        try {
            if (Modifier.isFinal(Slic3rLocalization.class.getModifiers())
                    || Modifier.isFinal(Slic3rLocalization.class.getMethod("get", String.class).getModifiers())
                    || !Modifier.isStatic(Slic3rLocalization.class.getMethod("getString", String.class).getModifiers())
                    || !Modifier.isStatic(Slic3rLocalization.class.getMethod("getInstance", String.class).getModifiers())
                    || !Modifier.isStatic(Slic3rLocalization.class.getMethod("getInstance").getModifiers())) {
                throw new AssertionError("Slic3rLocalization must preserve its extensible Java class and static entry points");
            }
            Class<?>[] exceptions = Slic3rLocalization.class.getConstructor(String.class).getExceptionTypes();
            if (!Arrays.asList(exceptions).contains(java.io.IOException.class)) {
                throw new AssertionError("Slic3rLocalization(String) must retain its declared IOException");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Slic3rLocalization Java API changed during Kotlin migration", error);
        }
    }

    private static void assertUtilityAndJniContracts() {
        try {
            DoubleMatrix.class.getConstructor();
            if (Modifier.isFinal(DoubleMatrix.class.getModifiers())
                    || !Modifier.isStatic(DoubleMatrix.class.getMethod("multiplyMM", double[].class, int.class,
                    double[].class, int.class, double[].class, int.class).getModifiers())) {
                throw new AssertionError("DoubleMatrix must retain its extensible class and Java-static method ABI");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("DoubleMatrix Java constructor or static method signature changed", error);
        }
        double[] identity = new double[16];
        DoubleMatrix.setIdentityM(identity, 0);
        double[] expectedIdentity = new double[16];
        expectedIdentity[0] = expectedIdentity[5] = expectedIdentity[10] = expectedIdentity[15] = 1.0;
        if (!Arrays.equals(identity, expectedIdentity)) {
            throw new AssertionError("DoubleMatrix must expose its Java-static identity initializer after Kotlin migration");
        }
        double[] translated = identity.clone();
        translated[12] = 3.0;
        translated[13] = -2.0;
        translated[14] = 5.0;
        double[] inverse = new double[16];
        if (!DoubleMatrix.invertM(inverse, 0, translated, 0)
                || inverse[12] != -3.0 || inverse[13] != 2.0 || inverse[14] != -5.0) {
            throw new AssertionError("DoubleMatrix inversion must preserve translation math");
        }
        double[] transformed = new double[16];
        DoubleMatrix.setIdentityM(transformed, 0);
        DoubleMatrix.rotateM(transformed, 0, 31.0, 0.0, 0.0, 1.0);
        DoubleMatrix.scaleM(transformed, 0, 1.75, 0.8, 2.1);
        DoubleMatrix.translateM(transformed, 0, 4.0, -3.0, 2.0);
        double[] transformedInverse = new double[16];
        double[] roundTrip = new double[16];
        if (!DoubleMatrix.invertM(transformedInverse, 0, transformed, 0)) {
            throw new AssertionError("DoubleMatrix must invert a translated, rotated, and scaled matrix");
        }
        DoubleMatrix.multiplyMM(roundTrip, 0, transformed, 0, transformedInverse, 0);
        for (int i = 0; i < roundTrip.length; i++) {
            if (Math.abs(roundTrip[i] - expectedIdentity[i]) > 1e-9) {
                throw new AssertionError("DoubleMatrix inverse must round-trip a composite transform at index " + i
                        + ": " + roundTrip[i]);
            }
        }
        double[] aliased = translated.clone();
        DoubleMatrix.multiplyMM(aliased, 0, aliased, 0, identity, 0);
        if (!Arrays.equals(aliased, translated) || DoubleMatrix.invertM(new double[16], 0, new double[16], 0)) {
            throw new AssertionError("DoubleMatrix must retain overlap-safe multiplication and singular-matrix handling");
        }
        try {
            DoubleMatrix.multiplyMM(null, 0, identity, 0, identity, 0);
            throw new AssertionError("DoubleMatrix must retain its explicit null-argument validation");
        } catch (IllegalArgumentException expected) {
            if (!"result == null".equals(expected.getMessage())) throw expected;
        }

        DebugUtils.assertTrue(true);
        DebugUtils.assertFalse(false);
        try {
            DebugUtils.assertTrue(false);
            throw new AssertionError("DebugUtils.assertTrue must throw in the debug test variant");
        } catch (AssertionError expected) {
            if (!"Assert failed".equals(expected.getMessage())) throw expected;
        }
        try {
            DebugUtils.assertFalse(true);
            throw new AssertionError("DebugUtils.assertFalse must throw in the debug test variant");
        } catch (AssertionError expected) {
            if (!"Assert failed".equals(expected.getMessage())) throw expected;
        }

        final int[] callbackValues = {-1};
        final String[] callbackText = {"unset"};
        SliceListener listener = (progress, text) -> {
            callbackValues[0] = progress;
            callbackText[0] = text;
        };
        listener.onProgress(64, "layer 64");
        if (callbackValues[0] != 64 || !"layer 64".equals(callbackText[0])) {
            throw new AssertionError("SliceListener Java SAM callback changed the native progress contract");
        }
        listener.onProgress(100, null);
        if (callbackValues[0] != 100 || callbackText[0] != null) {
            throw new AssertionError("SliceListener must preserve nullable native progress text");
        }

        try {
            DebugUtils.class.getConstructor();
            VibrationUtils.class.getConstructor();
            if (!Modifier.isStatic(DebugUtils.class.getMethod("assertTrue", boolean.class).getModifiers())
                    || !Modifier.isStatic(DebugUtils.class.getMethod("assertFalse", boolean.class).getModifiers())
                    || !Modifier.isStatic(VibrationUtils.class.getMethod("init", android.content.Context.class).getModifiers())
                    || !Modifier.isStatic(VibrationUtils.class.getMethod("hasAmplitudeControl").getModifiers())) {
                throw new AssertionError("Debug and vibration helper methods must remain static for Java callers");
            }
            SliceListener.class.getMethod("onProgress", int.class, String.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Debug, vibration or JNI listener Java ABI changed", e);
        }
    }

    private static void assertEventPayloadContracts() {
        LongClickTranslationEvent click = new LongClickTranslationEvent(12.5, -4.25, true);
        if (click.x != 12.5 || click.y != -4.25 || !click.visual) {
            throw new AssertionError("LongClickTranslationEvent Java payload fields changed");
        }
        NeedDismissSnackbarEvent dismiss = new NeedDismissSnackbarEvent("upload");
        if (!"upload".equals(dismiss.tag)) {
            throw new AssertionError("NeedDismissSnackbarEvent Java tag field changed");
        }

        NeedSnackbarEvent done = new NeedSnackbarEvent((CharSequence) "finished");
        NeedSnackbarEvent loading = new NeedSnackbarEvent(SnackbarsLayout.Type.LOADING, (CharSequence) "sending");
        if (done.type != SnackbarsLayout.Type.DONE || !"finished".contentEquals(done.title)
                || loading.type != SnackbarsLayout.Type.LOADING || !"sending".contentEquals(loading.title)) {
            throw new AssertionError("NeedSnackbarEvent title and type constructors changed");
        }
        NeedSnackbarEvent returned = loading.tag("upload");
        if (returned != loading || !"upload".equals(loading.tag)) {
            throw new AssertionError("NeedSnackbarEvent.tag must mutate and return this for Java chaining");
        }
        try {
            NeedSnackbarEvent.class.getConstructor(int.class, Object[].class);
            NeedSnackbarEvent.class.getConstructor(SnackbarsLayout.Type.class, int.class, Object[].class);
            NeedSnackbarEvent.class.getMethod("button", int.class, android.view.View.OnClickListener.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("NeedSnackbarEvent resource constructors or button action changed", e);
        }
    }

    private static void assertConfigMetadataContracts() {
        try {
            ConfigOptionDef.class.getDeclaredConstructor();
            PrintConfigDef.class.getDeclaredConstructor();
            if (ConfigOptionDef.class.getField("key").getType() != String.class
                    || ConfigOptionDef.class.getField("type").getType() != ConfigOptionDef.ConfigOptionType.class
                    || ConfigOptionDef.class.getField("enumValues").getType() != String[].class
                    || ConfigOptionDef.ConfigOptionType.FLOATS.list == false
                    || ConfigOptionDef.ConfigOptionType.FLOAT.list) {
                throw new AssertionError("ConfigOptionDef lost native field descriptors or list flags");
            }

            java.lang.reflect.Method addOption = PrintConfigDef.class.getDeclaredMethod(
                    "addOption", String.class, ConfigOptionDef.class);
            java.lang.reflect.Method resolveEnum = PrintConfigDef.class.getDeclaredMethod(
                    "resolveEnum", String.class, String.class);
            if (addOption.getReturnType() != Void.TYPE || resolveEnum.getReturnType() != Object.class
                    || !Modifier.isStatic(resolveEnum.getModifiers())
                    || PrintConfigDef.class.getField("SKIP_DEFAULT_OPTIONS").getType() != List.class) {
                throw new AssertionError("PrintConfigDef no longer matches the native JNI method descriptors");
            }

            ConfigOptionDef option = new ConfigOptionDef();
            option.key = "layer_height";
            PrintConfigDef definition = new PrintConfigDef();
            definition.addOption(option.key, option);
            if (definition.options.get(option.key) != option
                    || PrintConfigDef.resolveEnum(
                    "ru/ytkab0bp/slicebeam/slic3r/ConfigOptionDef$ConfigOptionType", "FLOATS")
                    != ConfigOptionDef.ConfigOptionType.FLOATS) {
                throw new AssertionError("PrintConfigDef lost its Java/JNI option insertion or enum lookup behavior");
            }

            assertConfigObjectJavaContract();
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Config metadata Java/JNI ABI changed during Kotlin migration", error);
        }
    }

    private static void assertConfigObjectJavaContract() throws ReflectiveOperationException {
        if (Modifier.isFinal(ConfigObject.class.getModifiers())
                || ConfigObject.PROFILE_LIST_PRINT != 0
                || ConfigObject.PROFILE_LIST_FILAMENT != 1
                || ConfigObject.PROFILE_LIST_PRINTER != 2
                || ConfigObject.class.getConstructors().length != 3
                || ConfigObject.class.getField("values").getType() != java.util.Map.class
                || ConfigObject.class.getField("thumbnailUrl").getType() != String.class
                || ConfigObject.class.getField("profileListType").getType() != int.class
                || ConfigObject.class.getField("KEY_MIGRATION").getType() != java.util.Map.class) {
            throw new AssertionError("ConfigObject fields, constructors, profile IDs or open Java type changed");
        }
        assertStaticMethod(ConfigObject.class, "migrateKey", String.class, String.class);
        assertStaticMethod(ConfigObject.class, "legacyKey", String.class, String.class);
        assertStaticMethod(ConfigObject.class, "printConfigKeyAliases", List.class);
        assertStaticMethod(ConfigObject.class, "normalizeSerializedValue", String.class, String.class, String.class);
        assertStaticMethod(ConfigObject.class, "createCustomPrinterProfile", ConfigObject.class);
        assertStaticMethod(ConfigObject.class, "createCustomFilamentProfile", ConfigObject.class);
        ConfigObject.class.getMethod("getTitle");
        ConfigObject.class.getMethod("setTitle", String.class);
        ConfigObject.class.getMethod("getExtruderCount");
        ConfigObject.class.getMethod("has", String.class);
        ConfigObject.class.getMethod("get", String.class);
        ConfigObject.class.getMethod("remove", String.class);
        ConfigObject.class.getMethod("put", String.class, String.class);
        ConfigObject.class.getMethod("serialize");

        ConfigObject profile = new ConfigObject("Contract profile");
        profile.put("bed_shape", "0x0,200x0,200x200,0x200");
        profile.put("top_one_wall_type", "none");
        profile.put("nozzle_diameter", "0.4;0.6");
        if (!"printable_area".equals(ConfigObject.migrateKey("bed_shape"))
                || !"0x0,200x0,200x200,0x200".equals(profile.get("printable_area"))
                || !"0".equals(profile.get("only_one_wall_top"))
                || profile.getExtruderCount() != 2
                || !"perimeters".equals(ConfigObject.legacyKey("wall_loops"))) {
            throw new AssertionError("ConfigObject key/value migration or multi-nozzle counting changed");
        }
        String cleanedGcode = ConfigObject.normalizeSerializedValue(
                "before_layer_change_gcode", "G92 E0\nM117 keep\n");
        if (!"M117 keep\n".equals(cleanedGcode)) {
            throw new AssertionError("ConfigObject layer G-code cleanup must preserve remaining lines and trailing newline");
        }
        ConfigObject copy = new ConfigObject(profile);
        copy.put("z_offset", "0.42");
        if (profile.get("z_offset") != null || !copy.serialize().startsWith("# generated by Slice Beam ")) {
            throw new AssertionError("ConfigObject copy and profile serialization behavior changed");
        }
        try {
            ConfigObject.printConfigKeyAliases().add("should_not_be_mutable");
            throw new AssertionError("ConfigObject process-key alias list must remain immutable");
        } catch (UnsupportedOperationException expected) {
            // Preserve the immutable import filter contract.
        }
    }

    private static void assertPrefsJavaContract() throws ReflectiveOperationException {
        if (Prefs.class.getConstructors().length != 1
                || Prefs.class.getField("CAMERA_CONTROL_MODE_ROTATE_MOVE").getInt(null) != 0
                || Prefs.class.getField("CAMERA_CONTROL_MODE_MOVE_ROTATE").getInt(null) != 1
                || Prefs.class.getField("CAMERA_CONTROL_MODE_MOVE_ONLY").getInt(null) != 2
                || Prefs.class.getField("MAX_FILAMENT_COLORS").getInt(null) != 16
                || Prefs.class.getMethod("getPrefs").getReturnType() != android.content.SharedPreferences.class) {
            throw new AssertionError("Prefs constructor, constants or nullable SharedPreferences API changed");
        }
        assertStaticMethod(Prefs.class, "init", Void.TYPE, android.app.Application.class);
        for (String[] method : new String[][]{
                {"getLastCommit", "0"}, {"setLastCommit", "0"}, {"getFilamentSlots", "0"},
                {"setFilamentSlots", "1"}, {"getFilamentPalette", "0"}, {"getThemeMode", "0"},
                {"setThemeMode", "1"}, {"getCloudAPIToken", "0"}, {"setCloudAPIToken", "1"},
                {"getRenderScale", "0"}, {"setRenderScale", "1"}, {"getCameraControlMode", "0"},
                {"setCameraControlMode", "1"}, {"isScaleLinked", "0"}, {"setScaleLinked", "1"},
        }) {
            boolean found = false;
            for (java.lang.reflect.Method candidate : Prefs.class.getMethods()) {
                if (candidate.getName().equals(method[0])
                        && candidate.getParameterCount() == Integer.parseInt(method[1])) {
                    found = Modifier.isStatic(candidate.getModifiers());
                    break;
                }
            }
            if (!found) throw new AssertionError("Prefs method must remain Java-static: " + method[0]);
        }
        java.lang.reflect.Field themeTitle = Prefs.ThemeMode.class.getField("title");
        if (themeTitle.getType() != int.class
                || Prefs.ThemeMode.values().length != 3
                || Prefs.ThemeMode.values()[0] != Prefs.ThemeMode.SYSTEM
                || Prefs.ThemeMode.values()[1] != Prefs.ThemeMode.LIGHT
                || Prefs.ThemeMode.values()[2] != Prefs.ThemeMode.DARK
                || Prefs.getPrefs() != null) {
            throw new AssertionError("Prefs theme enum or pre-bootstrap null behavior changed");
        }
    }

    private static void assertViewAndWebMenuContracts() {
        try {
            ViewUtils.class.getConstructor();
            if (ViewUtils.class.getField("CUBIC_INTERPOLATOR").getType() != TimeInterpolator.class
                    || ViewUtils.class.getField("ROBOTO_MEDIUM").getType() != String.class) {
                throw new AssertionError("ViewUtils public constants lost their Java field types");
            }
            assertStaticViewMethod("getUiHandler", Handler.class);
            assertStaticViewMethod("postOnMainThread", Void.TYPE, Runnable.class);
            assertStaticViewMethod("postOnMainThread", Void.TYPE, Runnable.class, long.class);
            assertStaticViewMethod("removeCallbacks", Void.TYPE, Runnable.class);
            assertStaticViewMethod("getTypeface", Typeface.class, String.class);
            assertStaticViewMethod("lerp", float.class, float.class, float.class, float.class);
            assertStaticViewMethod("lerpd", double.class, double.class, double.class, double.class, float.class);
            assertStaticViewMethod("lerpd", double.class, double.class, double.class, float.class);
            assertStaticViewMethod("createRipple", RippleDrawable.class, int.class, float.class);
            assertStaticViewMethod("createRipple", RippleDrawable.class, int.class, int.class, float.class);
            assertStaticViewMethod("dp", int.class, float.class);
            assertStaticViewMethod("dp", int.class, int.class);

            if (!UnfoldMenu.class.isAssignableFrom(WebViewMenu.class)
                    || Modifier.isFinal(WebViewMenu.class.getModifiers())) {
                throw new AssertionError("WebViewMenu must remain an extensible UnfoldMenu subtype");
            }
            WebViewMenu.class.getConstructor(Uri.class);
            WebViewMenu.class.getConstructor(Uri.class, String.class);
            if (WebViewMenu.class.getMethod("setFragment", ru.ytkab0bp.slicebeam.fragment.BedFragment.class)
                    .getReturnType() != WebViewMenu.class
                    || WebViewMenu.class.getMethod("getRequestedSize", FrameLayout.class, boolean.class)
                    .getReturnType() != int.class) {
                throw new AssertionError("WebViewMenu constructors or fluent Java methods changed");
            }

            Class<?> bridge = Class.forName(
                    "ru.ytkab0bp.slicebeam.components.WebViewMenu$Bridge",
                    false,
                    WebViewMenu.class.getClassLoader());
            assertJavascriptBridgeMethod(bridge, "beginDownload", String.class);
            assertJavascriptBridgeMethod(bridge, "writeData", String.class);
            assertJavascriptBridgeMethod(bridge, "finishDownload");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("View/WebView Java and JavascriptInterface contracts changed", error);
        }
    }

    private static void assertConverterStorageContracts() {
        try {
            if (ConverterStorage.class.getConstructors().length != 0) {
                throw new AssertionError("ConverterStorage must keep its private utility-class constructor");
            }
            assertStaticMethod(ConverterStorage.class, "getRoot", Uri.class);
            assertStaticMethod(ConverterStorage.class, "hasRoot", boolean.class);
            assertStaticMethod(ConverterStorage.class, "markAndShouldPromptFirstLaunch", boolean.class);
            assertStaticMethod(ConverterStorage.class, "createFolderPickerIntent", android.content.Intent.class);
            assertStaticMethod(ConverterStorage.class, "persistSelectedRoot", Void.TYPE,
                    android.app.Activity.class, android.content.Intent.class);
            assertStaticMethod(ConverterStorage.class, "getRootDisplayName", String.class,
                    android.content.Context.class);
            assertStaticMethod(ConverterStorage.class, "saveGeneratedFile", String.class,
                    android.content.Context.class, Uri.class, java.io.File.class);
            if (!Arrays.asList(ConverterStorage.class.getMethod("saveGeneratedFile",
                    android.content.Context.class, Uri.class, java.io.File.class).getExceptionTypes())
                    .contains(java.io.IOException.class)
                    || !Arrays.asList(ConverterStorage.class.getMethod("persistSelectedRoot",
                    android.app.Activity.class, android.content.Intent.class).getExceptionTypes())
                    .contains(SecurityException.class)) {
                throw new AssertionError("ConverterStorage checked-exception declarations changed");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("ConverterStorage Java API changed during Kotlin migration", error);
        }
    }

    private static void assertSvgImportGuardContracts() {
        try {
            if (SvgImportGuard.class.getConstructors().length != 0) {
                throw new AssertionError("SvgImportGuard must keep its private utility-class constructor");
            }
            assertStaticMethod(SvgImportGuard.class, "validate", String.class, java.io.File.class);
            assertStaticMethod(SvgImportGuard.class, "canSimplify", boolean.class, java.io.File.class);
            assertStaticMethod(SvgImportGuard.class, "simplify", int.class, java.io.File.class, java.io.File.class);
            if (!Arrays.asList(SvgImportGuard.class.getMethod("validate", java.io.File.class).getExceptionTypes())
                    .contains(java.io.IOException.class)
                    || !Arrays.asList(SvgImportGuard.class.getMethod("simplify",
                    java.io.File.class, java.io.File.class).getExceptionTypes()).contains(java.io.IOException.class)) {
                throw new AssertionError("SvgImportGuard checked-exception declarations changed");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("SvgImportGuard Java API changed during Kotlin migration", error);
        }
    }

    private static void assertSmallMakerGeneratorContracts() {
        try {
            for (Class<?> generator : Arrays.asList(
                    CableClipGenerator.class, CableGrommetGenerator.class, PhoneStandGenerator.class,
                    PlanterPotGenerator.class, PlantLabelSetGenerator.class, TextTagGenerator.class,
                    UtilityBoxGenerator.class, PowerStripTrayGenerator.class, WallToolHolderGenerator.class,
                    FastenerGenerator.class)) {
                if (generator.getConstructors().length != 0) {
                    throw new AssertionError(generator.getSimpleName() + " must keep its private utility constructor");
                }
                assertStaticMethod(generator, "show", Void.TYPE,
                        android.app.Activity.class, ru.ytkab0bp.slicebeam.fragment.BedFragment.class);
            }
            assertStaticMethod(TextTagGenerator.class, "rasterize", float[].class,
                    String.class, boolean.class, float.class, float.class, float.class, float.class,
                    int.class, int.class, float.class, float.class);
            if (LitoGenGenerator.class.getConstructors().length != 0) {
                throw new AssertionError("LitoGenGenerator must remain a non-instantiable utility entry point");
            }
            assertStaticMethod(LitoGenGenerator.class, "pick", Void.TYPE, android.app.Activity.class);
            assertStaticMethod(LitoGenGenerator.class, "isAwaitingImages", boolean.class);
            assertStaticMethod(LitoGenGenerator.class, "cancel", Void.TYPE);
            assertStaticMethod(LitoGenGenerator.class, "acceptPickedImages", boolean.class,
                    android.app.Activity.class, List.class, ru.ytkab0bp.slicebeam.fragment.BedFragment.class);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Maker generator Java launch contracts changed during Kotlin migration", error);
        }
    }

    private static void assertFastenerContracts() {
        try {
            if (FastenerStlWriter.class.getConstructors().length != 0) {
                throw new AssertionError("FastenerStlWriter must remain a non-instantiable Java utility");
            }
            assertStaticMethod(FastenerStlWriter.class, "writeBolt", int.class,
                    java.io.File.class, float.class, float.class, float.class, float.class, float.class);
            assertStaticMethod(FastenerStlWriter.class, "writeNut", int.class,
                    java.io.File.class, float.class, float.class, float.class);
            assertStaticMethod(FastenerStlWriter.class, "writeThreadedNut", int.class,
                    java.io.File.class, float.class, float.class, float.class, float.class, float.class);
            assertStaticMethod(FastenerStlWriter.class, "writeThreadFitCoupon", int.class,
                    java.io.File.class, float.class, float.class, float.class, float.class, float.class, float.class);
            assertStaticMethod(FastenerStlWriter.class, "writeWasher", int.class,
                    java.io.File.class, float.class, float.class, float.class);
            assertIOExceptionDeclaration(FastenerStlWriter.class, "writeBolt", java.io.File.class,
                    float.class, float.class, float.class, float.class, float.class);
            assertIOExceptionDeclaration(FastenerStlWriter.class, "writeNut", java.io.File.class,
                    float.class, float.class, float.class);
            assertIOExceptionDeclaration(FastenerStlWriter.class, "writeThreadedNut", java.io.File.class,
                    float.class, float.class, float.class, float.class, float.class);
            assertIOExceptionDeclaration(FastenerStlWriter.class, "writeThreadFitCoupon", java.io.File.class,
                    float.class, float.class, float.class, float.class, float.class, float.class);
            assertIOExceptionDeclaration(FastenerStlWriter.class, "writeWasher", java.io.File.class,
                    float.class, float.class, float.class);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Fastener generator or STL writer Java API changed during Kotlin migration", error);
        }
    }

    private static void assertMmuFilamentSyncContracts() {
        try {
            if (MmuFilamentSync.class.getConstructors().length != 0) {
                throw new AssertionError("MmuFilamentSync must keep its private utility constructor");
            }
            assertStaticMethod(MmuFilamentSync.class, "synchronize", Void.TYPE,
                    ConfigObject.class, MmuFilamentSync.Callback.class);
            assertStaticMethod(MmuFilamentSync.class, "getSnapshot", JSONObject.class, String.class);
            assertStaticMethod(MmuFilamentSync.class, "getSelectedGates", int[].class, String.class, int.class);
            assertStaticMethod(MmuFilamentSync.class, "setSelectedGates", Void.TYPE, String.class, int[].class);
            assertStaticMethod(MmuFilamentSync.class, "loadedGates", int[].class, String.class);
            assertStaticMethod(MmuFilamentSync.class, "defaultGateForTool", int.class, String.class, int.class);
            assertStaticMethod(MmuFilamentSync.class, "toolForGate", int.class, String.class, int.class);
            assertStaticMethod(MmuFilamentSync.class, "gateLabel", String.class, String.class, int.class);
            assertStaticMethod(MmuFilamentSync.class, "fromMoonraker", List.class, JSONObject.class);
            MmuFilamentSync.Callback.class.getMethod("onResult", int.class, String.class);
            if (!Arrays.asList(MmuFilamentSync.class.getMethod("fromMoonraker", JSONObject.class)
                    .getExceptionTypes()).contains(org.json.JSONException.class)) {
                throw new AssertionError("Moonraker parser must retain its checked JSONException declaration");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("MmuFilamentSync Java API changed during Kotlin migration", error);
        }
    }

    private static void assertBeamServerDataContracts() {
        try {
            if (Modifier.isFinal(BeamServerData.class.getModifiers())) {
                throw new AssertionError("BeamServerData must remain subclassable for Java compatibility");
            }
            if (BeamServerData.class.getField("boostySubscribers").getType() != List.class) {
                throw new AssertionError("BeamServerData.boostySubscribers must remain a public Java List field");
            }
            BeamServerData.class.getConstructor(JSONObject.class);
            assertStaticMethod(BeamServerData.class, "isBoostyAvailable", boolean.class);
            assertStaticMethod(BeamServerData.class, "isCloudAvailable", boolean.class);
            assertStaticMethod(BeamServerData.class, "load", Void.TYPE);
            if (BeamServerData.isBoostyAvailable() || BeamServerData.isCloudAvailable()) {
                throw new AssertionError("Offline server bootstrap must keep cloud-only features disabled");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("BeamServerData Java API changed during Kotlin migration", error);
        }
    }

    private static void assertSliceBeamJavaContract() {
        try {
            if (SliceBeam.class.getSuperclass() != Application.class
                    || Modifier.isFinal(SliceBeam.class.getModifiers())) {
                throw new AssertionError("SliceBeam must remain a subclassable Android Application");
            }
            SliceBeam.class.getConstructor();
            assertStaticField(SliceBeam.class, "INSTANCE", SliceBeam.class);
            assertStaticField(SliceBeam.class, "EVENT_BUS", EventBus.class);
            assertStaticField(SliceBeam.class, "TRUE_TIME", TrueTimeImpl.class);
            assertStaticField(SliceBeam.class, "CONFIG", Slic3rConfigWrapper.class);
            assertStaticField(SliceBeam.class, "CONFIG_UID", int.class);
            assertStaticField(SliceBeam.class, "AUTO_BRIM_SELECTED", boolean.class);
            assertStaticField(SliceBeam.class, "PENDING_CALIB_MODE", int.class);
            assertStaticField(SliceBeam.class, "PENDING_CALIB_START", double.class);
            assertStaticField(SliceBeam.class, "PENDING_CALIB_END", double.class);
            assertStaticField(SliceBeam.class, "PENDING_CALIB_STEP", double.class);
            assertStaticField(SliceBeam.class, "SERVER_DATA", BeamServerData.class);
            assertStaticField(SliceBeam.class, "hasUpdateInfo", boolean.class);

            assertStaticMethod(SliceBeam.class, "clearLiveDiffs", Void.TYPE);
            assertStaticMethod(SliceBeam.class, "requireInstance", SliceBeam.class);
            assertStaticMethod(SliceBeam.class, "requireConfig", Slic3rConfigWrapper.class);
            assertStaticMethod(SliceBeam.class, "saveConfig", boolean.class);
            assertStaticMethod(SliceBeam.class, "getModelCacheDir", java.io.File.class);
            assertStaticMethod(SliceBeam.class, "getConfigFile", java.io.File.class);
            assertStaticMethod(SliceBeam.class, "ensureCompatibleSelection", Void.TYPE);
            assertStaticMethod(SliceBeam.class, "buildCurrentConfigObject", ConfigObject.class);
            assertStaticMethod(SliceBeam.class, "buildCurrentConfigObject", ConfigObject.class, int.class);
            assertStaticMethod(SliceBeam.class, "genCurrentConfig", Void.TYPE);
            assertStaticMethod(SliceBeam.class, "genCurrentConfig", Void.TYPE, Model.class);
            assertIOExceptionDeclaration(SliceBeam.class, "genCurrentConfig");
            assertIOExceptionDeclaration(SliceBeam.class, "genCurrentConfig", Model.class);
            assertStaticMethod(SliceBeam.class, "getCurrentConfigFile", java.io.File.class);

        } catch (ReflectiveOperationException error) {
            throw new AssertionError("SliceBeam Java/Application contract changed during Kotlin migration", error);
        }
    }

    private static void assertStaticField(Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        java.lang.reflect.Field field = owner.getField(name);
        if (field.getType() != type || !Modifier.isStatic(field.getModifiers())
                || !Modifier.isPublic(field.getModifiers())) {
            throw new AssertionError(owner.getSimpleName() + "." + name
                    + " must remain a public static field of type " + type.getSimpleName());
        }
    }

    private static void assertSlic3rConfigWrapperContract() throws IOException {
        try {
            if (Modifier.isFinal(Slic3rConfigWrapper.class.getModifiers())) {
                throw new AssertionError("Slic3rConfigWrapper must remain subclassable for Java callers");
            }
            Slic3rConfigWrapper.class.getConstructor();
            assertStaticField(Slic3rConfigWrapper.class, "BLACKLISTED_SYMBOLS", String.class);
            for (String keyList : Arrays.asList("PRINT_CONFIG_KEYS", "FILAMENT_CONFIG_KEYS",
                    "PRINTER_CONFIG_KEYS", "PHYSICAL_PRINTER_CONFIG_KEYS")) {
                assertStaticField(Slic3rConfigWrapper.class, keyList, List.class);
            }
            for (String field : Arrays.asList("printConfigs", "printerConfigs", "filamentConfigs",
                    "physicalPrintersConfigs", "printerModels")) {
                java.lang.reflect.Field reflected = Slic3rConfigWrapper.class.getField(field);
                if (reflected.getType() != List.class || Modifier.isStatic(reflected.getModifiers())) {
                    throw new AssertionError("Slic3rConfigWrapper." + field + " must remain a public instance List");
                }
            }
            for (String field : Arrays.asList("presets", "vendor")) {
                java.lang.reflect.Field reflected = Slic3rConfigWrapper.class.getField(field);
                if (reflected.getType() != ConfigObject.class || Modifier.isStatic(reflected.getModifiers())) {
                    throw new AssertionError("Slic3rConfigWrapper." + field + " must remain a public instance ConfigObject");
                }
            }

            Slic3rConfigWrapper.class.getConstructor(java.io.File.class);
            Slic3rConfigWrapper.class.getConstructor(java.io.InputStream.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "importPrint", Void.TYPE, ConfigObject.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "importPrinter", Void.TYPE, ConfigObject.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "importFilament", Void.TYPE, ConfigObject.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "importInto", Void.TYPE, List.class, ConfigObject.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "findFilament", ConfigObject.class, String.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "findPrinterVariant", ConfigObject.class, String.class, String.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "findPrint", ConfigObject.class, String.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "findPrinter", ConfigObject.class, String.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "serialize", String.class);
            assertInstanceMethod(Slic3rConfigWrapper.class, "readFromStream", Void.TYPE, java.io.InputStream.class);
            for (java.lang.reflect.Constructor<?> constructor : Arrays.asList(
                    Slic3rConfigWrapper.class.getConstructor(java.io.File.class),
                    Slic3rConfigWrapper.class.getConstructor(java.io.InputStream.class))) {
                if (!Arrays.asList(constructor.getExceptionTypes()).contains(IOException.class)) {
                    throw new AssertionError("Slic3rConfigWrapper constructors must retain their IOException declaration");
                }
            }
            if (!Arrays.asList(Slic3rConfigWrapper.class.getMethod("readFromStream", java.io.InputStream.class)
                    .getExceptionTypes()).contains(IOException.class)) {
                throw new AssertionError("Slic3rConfigWrapper.readFromStream must retain its IOException declaration");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Slic3rConfigWrapper Java API changed during Kotlin migration", error);
        }

        String explicitIni = "[print:Base]\nparent_marker = parent\n"
                + "[print:Child]\ninherits = Base\nprofile_marker = child\nlayer_height = 0.28\n"
                + "[printer_model:Anycubic Kobra 3 V2]\nprinter_model = Anycubic Kobra 3 V2\n"
                + "[printer:Kobra 0.4]\nprinter_model = Anycubic Kobra 3 V2\nprinter_variant = 0.4\n"
                + "thumbnails = 16x16, 32x32/COLPIC\narc_fitting = 1\n"
                + "[filament:PLA]\ntemperature = 220\nfilament_type = PLA\n"
                + "[physical_printer:Kobra]\nprint_host = printer.local\n"
                + "[vendor]\nname = Orca\n"
                + "[presets]\nprint = Child\nprinter = Kobra 0.4\nfilament = PLA\nphysical_printer = Kobra\n";
        Slic3rConfigWrapper explicit = readConfig(explicitIni);
        ConfigObject child = explicit.findPrint("Child");
        if (child == null || !"parent".equals(child.get("parent_marker"))
                || !"child".equals(child.get("profile_marker"))
                || !"0.28".equals(child.get("layer_height")) || child.get("inherits") != null
                || child.profileListType != ConfigObject.PROFILE_LIST_PRINT
                || explicit.printerModels.size() != 1
                || explicit.findPrinterVariant("Anycubic Kobra 3 V2", "0.4") == null
                || !"16x16".equals(explicit.findPrinter("Kobra 0.4").get("thumbnails"))
                || explicit.findPrinter("Kobra 0.4").get("arc_fitting") != null
                || !"nozzle_temperature".equals(ConfigObject.migrateKey("temperature"))
                || explicit.findFilament("PLA").profileListType != ConfigObject.PROFILE_LIST_FILAMENT
                || !"Orca".equals(explicit.vendor.get("name"))
                || !"Child".equals(explicit.presets.get("print"))
                || !"Kobra".equals(explicit.presets.get("physical_printer"))) {
            throw new AssertionError("Slic3rConfigWrapper explicit profiles, inheritance, orphans, presets, or sanitizers changed");
        }

        ConfigObject replacement = new ConfigObject("Child");
        replacement.put("replacement_marker", "updated");
        explicit.importPrint(replacement);
        if (explicit.printConfigs.size() != 2 || explicit.findPrint("Child") != child
                || !"updated".equals(child.get("replacement_marker")) || child.get("profile_marker") != null) {
            throw new AssertionError("Slic3rConfigWrapper importPrint must replace values in-place for duplicate profile names");
        }

        String serialized = explicit.serialize();
        if (!serialized.startsWith("# generated by Slice Beam ")
                || !serialized.contains("[print:Child]")
                || serialized.contains("[physical_printer:")
                || serialized.contains("[printer_model:")
                || serialized.contains("name = Orca")) {
            throw new AssertionError("Slic3rConfigWrapper serialization categories or header changed");
        }

        Slic3rConfigWrapper combined = readConfig("print_settings_id = Legacy Quality\nlayer_height = 0.2\n"
                + "printer_settings_id = Legacy Kobra\nz_offset = 0.3\n"
                + "filament_settings_id = Legacy PLA\ntemperature = 215\nbed_temperature = 60\n");
        if (!"Legacy Quality".equals(combined.presets.get("print"))
                || !"Legacy Kobra".equals(combined.findPrinter("Legacy Kobra").getTitle())
                || !"0.3".equals(combined.findPrinter("Legacy Kobra").get("z_offset"))
                || !"215".equals(combined.findFilament("Legacy PLA").get("nozzle_temperature"))
                || !"60".equals(combined.findFilament("Legacy PLA").get("hot_plate_temp"))) {
            throw new AssertionError("Slic3rConfigWrapper legacy combined profiles or Orca key migrations changed");
        }

        for (String invalid : Arrays.asList("MALFORMED_INI_DATA", "[unsupported]\nvalue = 1\n")) {
            try {
                readConfig(invalid);
                throw new AssertionError("Slic3rConfigWrapper must reject unrecognized configuration text");
            } catch (java.io.UnsupportedEncodingException expected) {
                // Preserve the current profile-import failure contract.
            }
        }
        try {
            readConfig("layer_height = 0.2\n[print:Explicit]\nlayer_height = 0.3\n");
            throw new AssertionError("Slic3rConfigWrapper must reject explicit sections mixed with a legacy bundle");
        } catch (java.io.UnsupportedEncodingException expected) {
            // Explicit preset bundles and combined legacy profiles are mutually exclusive.
        }
        Slic3rConfigWrapper obsoleteOnly = readConfig("[obsolete_presets]\nlegacy_value = 1\n");
        if (obsoleteOnly.serialize().contains("legacy_value")) {
            throw new AssertionError("Obsolete preset payload must be accepted but remain excluded from saved profiles");
        }
    }

    private static Slic3rConfigWrapper readConfig(String source) throws IOException {
        return new Slic3rConfigWrapper(new java.io.ByteArrayInputStream(
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static void assertCloudAPIJavaContract() throws ReflectiveOperationException {
        java.lang.reflect.Field instance = CloudAPI.class.getField("INSTANCE");
        if (instance.getType() != CloudAPI.class
                || !Modifier.isPublic(instance.getModifiers())
                || !Modifier.isStatic(instance.getModifiers())
                || !Modifier.isFinal(instance.getModifiers())
                || !java.lang.reflect.Proxy.isProxyClass(CloudAPI.INSTANCE.getClass())) {
            throw new AssertionError("CloudAPI.INSTANCE must remain a public static SAPIL runner proxy");
        }

        java.lang.reflect.Method loginBegin = CloudAPI.class.getMethod(
                "loginBegin", ru.ytkab0bp.sapil.APICallback.class);
        ru.ytkab0bp.sapil.Method loginRoute = loginBegin.getAnnotation(ru.ytkab0bp.sapil.Method.class);
        java.lang.reflect.ParameterizedType loginCallback = (java.lang.reflect.ParameterizedType)
                loginBegin.getGenericParameterTypes()[0];
        if (loginRoute == null || !"login/begin".equals(loginRoute.value())
                || loginBegin.getReturnType() != ru.ytkab0bp.sapil.APIRequestHandle.class
                || loginCallback.getActualTypeArguments()[0] != CloudAPI.LoginData.class) {
            throw new AssertionError("CloudAPI login route, callback payload, or request-handle return changed");
        }

        java.lang.reflect.Method upload = CloudAPI.class.getMethod(
                "syncUpload", String.class, String.class, ru.ytkab0bp.sapil.APICallback.class);
        ru.ytkab0bp.sapil.Method uploadRoute = upload.getAnnotation(ru.ytkab0bp.sapil.Method.class);
        java.lang.annotation.Annotation[][] uploadAnnotations = upload.getParameterAnnotations();
        if (uploadRoute == null || !"sync/upload".equals(uploadRoute.value())
                || uploadRoute.requestType() != ru.ytkab0bp.sapil.RequestType.POST
                || uploadAnnotations[0].length != 1
                || !"".equals(((ru.ytkab0bp.sapil.Arg) uploadAnnotations[0][0]).value())
                || uploadAnnotations[1].length != 1
                || !"Content-Type".equals(((ru.ytkab0bp.sapil.Header) uploadAnnotations[1][0]).value())) {
            throw new AssertionError("CloudAPI upload annotations must remain runtime-visible to SAPIL");
        }

        java.lang.reflect.Method imageGeneration = CloudAPI.class.getMethod(
                "modelsGenerate", String.class, String.class, ru.ytkab0bp.sapil.APICallback.class);
        ru.ytkab0bp.sapil.Method generationRoute = imageGeneration.getAnnotation(ru.ytkab0bp.sapil.Method.class);
        java.lang.reflect.ParameterizedType imageCallback = (java.lang.reflect.ParameterizedType)
                imageGeneration.getGenericParameterTypes()[2];
        if (generationRoute == null || !"models/generate".equals(generationRoute.value())
                || generationRoute.requestType() != ru.ytkab0bp.sapil.RequestType.POST
                || imageCallback.getActualTypeArguments()[0] != java.io.InputStream.class) {
            throw new AssertionError("CloudAPI model-generation route or streaming response type changed");
        }

        java.lang.reflect.Field levels = CloudAPI.UserFeatures.class.getField("levels");
        java.lang.reflect.ParameterizedType levelList = (java.lang.reflect.ParameterizedType) levels.getGenericType();
        if (levels.getType() != List.class
                || levelList.getActualTypeArguments()[0] != CloudAPI.SubscriptionLevel.class
                || !new CloudAPI.UserFeatures().levels.isEmpty()
                || new CloudAPI.SyncState().lastUpdatedDate != 0L) {
            throw new AssertionError("CloudAPI DTO field descriptors, list type, or defaults changed");
        }
        for (java.lang.reflect.Field field : new java.lang.reflect.Field[]{
                CloudAPI.LoginData.class.getField("url"),
                CloudAPI.LoginData.class.getField("sessionId"),
                CloudAPI.LoginState.class.getField("bearer"),
                CloudAPI.UserInfo.class.getField("avatarUrl"),
                CloudAPI.SubscriptionLevel.class.getField("manageUrl")}) {
            if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                throw new AssertionError("CloudAPI DTO field is no longer public instance state: " + field.getName());
            }
        }
    }

    private static void assertSapilKotlinContracts() throws ReflectiveOperationException {
        java.lang.reflect.Field libraryInstance = APILibrary.class.getField("INSTANCE");
        if (!Modifier.isStatic(libraryInstance.getModifiers())
                || libraryInstance.getType() != APILibrary.class) {
            throw new AssertionError("SAPIL must preserve its Java-visible singleton field");
        }
        java.lang.reflect.Method setConfig = APILibrary.class.getMethod("setConfig", APILibraryConfig.class);
        java.lang.reflect.Method newRunner = APILibrary.class.getMethod(
                "newRunner", Class.class, APIRunner.RunnerConfig.class);
        if (!Modifier.isStatic(setConfig.getModifiers())
                || !Modifier.isStatic(newRunner.getModifiers())
                || newRunner.getReturnType() != APIRunner.class) {
            throw new AssertionError("SAPIL static configuration or runner factory signatures changed");
        }

        if (!APICallback.class.getMethod("onException", Exception.class).isDefault()
                || !APIRunner.RunnerConfig.class.getMethod("getBaseURL").isDefault()
                || !APIRunner.RunnerConfig.class.getMethod("getDefaultHeaders").isDefault()) {
            throw new AssertionError("SAPIL Java default interface callbacks or runner defaults were lost");
        }
        if (APIRunner.class.getField("DEFAULT_USER_AGENT").getType() != String.class
                || !Modifier.isStatic(APIRunner.class.getField("DEFAULT_USER_AGENT").getModifiers())
                || !Modifier.isFinal(APIRunner.class.getField("DEFAULT_USER_AGENT").getModifiers())) {
            throw new AssertionError("SAPIL default user-agent constant is no longer Java-static");
        }

        java.lang.reflect.Field first = ru.ytkab0bp.sapil.util.Pair.class.getField("first");
        java.lang.reflect.Field second = ru.ytkab0bp.sapil.util.Pair.class.getField("second");
        if (!Modifier.isPublic(first.getModifiers()) || !Modifier.isFinal(first.getModifiers())
                || !Modifier.isPublic(second.getModifiers()) || !Modifier.isFinal(second.getModifiers())
                || RequestType.values().length != 6 || RequestType.values()[0] != RequestType._DEFAULT) {
            throw new AssertionError("SAPIL Pair fields or request-type enum order changed");
        }

        if (APILibraryConfig.class.getConstructor() == null
                || APILibraryConfig.class.getMethod("setDefaultRequestType", RequestType.class).getReturnType()
                        != APILibraryConfig.class
                || APILibraryConfig.class.getMethod("setGson", com.google.gson.Gson.class).getReturnType()
                        != APILibraryConfig.class
                || APIRequestHandle.class.getMethod("isRunning").getReturnType() != boolean.class
                || APIEnclosure.LifecycleDestroyListener.class.getMethod("onDestroy").getReturnType() != void.class) {
            throw new AssertionError("SAPIL config builders, request handles, or lifecycle listener API changed");
        }
    }

    private static void assertCloudControllerJavaContract() throws ReflectiveOperationException {
        if (Modifier.isFinal(CloudController.class.getModifiers())
                || CloudController.class.getConstructor() == null) {
            throw new AssertionError("CloudController must retain its extensible type and public no-arg constructor");
        }
        for (String methodName : Arrays.asList(
                "initCached", "init", "isLoggingIn", "beginLogin", "cancelLogin", "logout",
                "checkGeneratorRemaining", "checkUserFeatures", "getUserInfo", "getUserFeatures",
                "hasAccountFeatures", "isSyncAvailable", "needShowAIGenerator", "getGeneratedModels",
                "getMaxGeneratedModels", "notifyDataChanged")) {
            java.lang.reflect.Method method = Arrays.stream(CloudController.class.getMethods())
                    .filter(candidate -> candidate.getName().equals(methodName)
                            && Modifier.isStatic(candidate.getModifiers()))
                    .findFirst().orElse(null);
            if (method == null) {
                throw new AssertionError("CloudController lost its public Java static method: " + methodName);
            }
        }
        java.lang.reflect.Field aiTag = CloudController.class.getField("USER_INFO_AI_GEN_TAG");
        java.lang.reflect.Field syncTag = CloudController.class.getField("CLOUD_SYNC_TAG");
        if (aiTag.getType() != String.class || syncTag.getType() != String.class
                || !Modifier.isStatic(aiTag.getModifiers()) || !Modifier.isFinal(aiTag.getModifiers())
                || !"ai_gen_user_info".equals(aiTag.get(null))
                || !"cloud_sync".equals(syncTag.get(null))) {
            throw new AssertionError("CloudController public static tag constants changed");
        }
        if (CloudController.class.getMethod("getUserInfo").getReturnType() != CloudAPI.UserInfo.class
                || CloudController.class.getMethod("getUserFeatures").getReturnType() != CloudAPI.UserFeatures.class
                || CloudController.class.getMethod("isLoggingIn").getReturnType() != boolean.class
                || CloudController.class.getMethod("getGeneratedModels").getReturnType() != int.class) {
            throw new AssertionError("CloudController Java return types changed during Kotlin migration");
        }

        CloudController.initCached();
        if (CloudController.getUserInfo() != null || CloudController.getUserFeatures() != null
                || CloudController.isLoggingIn() || CloudController.getGeneratedModels() != 0
                || CloudController.getMaxGeneratedModels() != 0) {
            throw new AssertionError("CloudController offline bootstrap must start without cached cloud state");
        }
    }

    private static void assertGCodeViewerContracts() {
        try {
            if (Modifier.isFinal(GCodeViewer.class.getModifiers()) || GCodeViewer.class.getConstructors().length != 1) {
                throw new AssertionError("GCodeViewer must remain an extensible type with a public no-arg constructor");
            }
            if (GCodeViewer.EXTRUSION_ROLES_COUNT != 15
                    || GCodeViewer.EXTRUSION_ROLE_NONE != 0 || GCodeViewer.EXTRUSION_ROLE_CUSTOM != 14
                    || GCodeViewer.VIEW_TYPE_FEATURE != 0 || GCodeViewer.VIEW_TYPE_TOOL != 11
                    || GCodeViewer.OPTION_TYPE_SEAMS != 4) {
                throw new AssertionError("GCodeViewer native role or option constants changed");
            }
            assertInstanceMethod(GCodeViewer.class, "isInitialized", boolean.class);
            assertInstanceMethod(GCodeViewer.class, "initGL", Void.TYPE);
            assertInstanceMethod(GCodeViewer.class, "load", Void.TYPE, GCodeProcessorResult.class);
            assertInstanceMethod(GCodeViewer.class, "setLayersViewRange", Void.TYPE, long.class, long.class);
            assertInstanceMethod(GCodeViewer.class, "setInfillVisibilityDepth", Void.TYPE, int.class);
            assertInstanceMethod(GCodeViewer.class, "isOptionVisible", boolean.class, int.class);
            assertInstanceMethod(GCodeViewer.class, "toggleOptionVisibility", Void.TYPE, int.class);
            assertInstanceMethod(GCodeViewer.class, "setViewType", Void.TYPE, int.class);
            assertInstanceMethod(GCodeViewer.class, "getViewType", int.class);
            assertInstanceMethod(GCodeViewer.class, "setToolColors", Void.TYPE, int[].class);
            assertInstanceMethod(GCodeViewer.class, "setFastMode", Void.TYPE, boolean.class);
            assertInstanceMethod(GCodeViewer.class, "setSelectedObjectId", Void.TYPE, int.class);
            assertInstanceMethod(GCodeViewer.class, "getLayersViewRange", androidx.core.util.Pair.class);
            assertInstanceMethod(GCodeViewer.class, "getLayersCount", long.class);
            assertInstanceMethod(GCodeViewer.class, "getEstimatedTime", float.class);
            assertInstanceMethod(GCodeViewer.class, "getEstimatedTime", float.class, int.class);
            assertInstanceMethod(GCodeViewer.class, "isExtrusionRoleVisible", boolean.class, int.class);
            assertInstanceMethod(GCodeViewer.class, "toggleExtrusionRoleVisible", Void.TYPE, int.class);
            assertInstanceMethod(GCodeViewer.class, "render", Void.TYPE, double[].class, double[].class);
            assertInstanceMethod(GCodeViewer.class, "setThemeColors", Void.TYPE);
            assertInstanceMethod(GCodeViewer.class, "setColors", Void.TYPE,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            assertInstanceMethod(GCodeViewer.class, "reset", Void.TYPE);
            assertInstanceMethod(GCodeViewer.class, "release", Void.TYPE);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("GCodeViewer Java/JNI API changed during Kotlin migration", error);
        }
    }

    private static void assertIOUtilsContracts() {
        try {
            if (IOUtils.class.getConstructors().length != 1) {
                throw new AssertionError("IOUtils must retain its public no-argument constructor");
            }
            IOUtils.class.getConstructor();
            if (IOUtils.class.getField("IO_POOL").getType() != java.util.concurrent.ExecutorService.class
                    || !Modifier.isStatic(IOUtils.class.getField("IO_POOL").getModifiers())) {
                throw new AssertionError("IOUtils.IO_POOL must remain a public static ExecutorService field");
            }
            assertStaticMethod(IOUtils.class, "getDisplayName", String.class, Uri.class);
            assertStaticMethod(IOUtils.class, "readString", String.class, java.io.InputStream.class);
            assertStaticMethod(IOUtils.class, "readString", String.class, java.io.InputStream.class, boolean.class);
            assertStaticMethod(IOUtils.class, "configJsonToIni", ConfigObject.class,
                    JSONObject.class, String.class, List.class, List.class);
            for (Class<?> exception : Arrays.asList(IOException.class, org.json.JSONException.class,
                    IOUtils.MissingProfileException.class)) {
                if (!Arrays.asList(IOUtils.class.getMethod("configJsonToIni", JSONObject.class, String.class,
                        List.class, List.class).getExceptionTypes()).contains(exception)) {
                    throw new AssertionError("configJsonToIni lost its checked exception declaration: " + exception.getName());
                }
            }
            if (IOUtils.MissingProfileException.class.getField("profile").getType() != String.class) {
                throw new AssertionError("MissingProfileException.profile must remain a public String field");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("IOUtils Java API changed during Kotlin migration", error);
        }

        try {
            JSONObject processJson = new JSONObject()
                    .put("print_settings_id", "[\"Quality Profile\"]")
                    .put("layer_height", 0.2)
                    .put("chamber_temperatures", new org.json.JSONArray().put(35).put(40))
                    .put("start_gcode", "M104 S[nozzle_temperature_initial_layer] M140 S[bed_temperature_initial_layer_single]")
                    .put("unsupported_option", "must be ignored");
            ConfigObject process = IOUtils.configJsonToIni(processJson, "process",
                    Arrays.asList("layer_height", "chamber_temperature", "start_gcode"), Collections.emptyList());
            if (process.profileListType != ConfigObject.PROFILE_LIST_PRINT
                    || !"Quality Profile".equals(process.getTitle())
                    || !"0.2".equals(process.get("layer_height"))
                    || !"35,40".equals(process.get("chamber_temperature"))
                    || process.get("unsupported_option") != null
                    || !process.get("machine_start_gcode").contains("first_layer_temperature")
                    || !process.get("machine_start_gcode").contains("first_layer_bed_temperature")) {
                throw new AssertionError("Orca JSON profile conversion changed key mapping, filtering, title, or G-code tokens");
            }

            ConfigObject filament = IOUtils.configJsonToIni(new JSONObject().put("filament_settings_id", "PLA"),
                    "filament", Collections.emptyList(), Collections.emptyList());
            ConfigObject printer = IOUtils.configJsonToIni(new JSONObject().put("printer_settings_id", "Machine"),
                    "machine", Collections.emptyList(), Collections.emptyList());
            if (filament.profileListType != ConfigObject.PROFILE_LIST_FILAMENT
                    || printer.profileListType != ConfigObject.PROFILE_LIST_PRINTER) {
                throw new AssertionError("Imported filament or printer profiles lost their selector category");
            }

            try {
                IOUtils.configJsonToIni(new JSONObject().put("inherits", "MissingParent"), "process",
                        Collections.emptyList(), Collections.emptyList());
                throw new AssertionError("An unavailable unbundled parent profile must still be reported");
            } catch (IOUtils.MissingProfileException expected) {
                if (!"MissingParent".equals(expected.profile)
                        || !"MissingProfileException{profile='MissingParent'}".equals(expected.toString())) {
                    throw new AssertionError("Missing profile exception payload changed");
                }
            }
        } catch (org.json.JSONException | IOException | IOUtils.MissingProfileException error) {
            throw new AssertionError("IOUtils profile import regression failed", error);
        }

        try {
            String decoded = IOUtils.readString(new java.io.ByteArrayInputStream("Orca ✓".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (!"Orca ✓".equals(decoded)) throw new AssertionError("IOUtils must decode profile text as UTF-8");
        } catch (IOException error) {
            throw new AssertionError("IOUtils UTF-8 stream reading failed", error);
        }
    }

    private static void assertInstanceMethod(Class<?> owner, String name, Class<?> returnType,
                                             Class<?>... parameters) throws ReflectiveOperationException {
        java.lang.reflect.Method method = owner.getMethod(name, parameters);
        if (method.getReturnType() != returnType || Modifier.isStatic(method.getModifiers())) {
            throw new AssertionError(owner.getSimpleName() + "." + name + " must retain its instance Java signature");
        }
    }

    private static void assertIOExceptionDeclaration(Class<?> owner, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        if (!Arrays.asList(owner.getMethod(name, parameters).getExceptionTypes()).contains(IOException.class)) {
            throw new AssertionError(owner.getSimpleName() + "." + name + " must retain its checked IOException declaration");
        }
    }

    private static void assertStaticMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters)
            throws ReflectiveOperationException {
        java.lang.reflect.Method method = owner.getMethod(name, parameters);
        if (method.getReturnType() != returnType || !Modifier.isStatic(method.getModifiers())) {
            throw new AssertionError(owner.getSimpleName() + "." + name + " must retain its static Java signature");
        }
    }

    private static void assertStaticViewMethod(String name, Class<?> returnType, Class<?>... parameters)
            throws ReflectiveOperationException {
        java.lang.reflect.Method method = ViewUtils.class.getMethod(name, parameters);
        if (method.getReturnType() != returnType || !Modifier.isStatic(method.getModifiers())) {
            throw new AssertionError("ViewUtils." + name + " must retain its static Java signature");
        }
    }

    private static void assertJavascriptBridgeMethod(Class<?> bridge, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        java.lang.reflect.Method method = bridge.getDeclaredMethod(name, parameters);
        if (method.getReturnType() != Void.TYPE || !Modifier.isPublic(method.getModifiers())
                || !method.isAnnotationPresent(JavascriptInterface.class)) {
            throw new AssertionError("WebView JavascriptInterface method is not JavaScript-callable: " + name);
        }
    }

    private static void assertSmartSliceContracts() {
        SmartSliceIntent intent = new SmartSliceIntent(
                SmartSliceIntent.Purpose.FUNCTIONAL,
                SmartSliceIntent.Priority.STRENGTH,
                1.2,
                0.24);
        if (intent.purpose != SmartSliceIntent.Purpose.FUNCTIONAL
                || intent.priority != SmartSliceIntent.Priority.STRENGTH
                || intent.requestedWallMm == null || intent.requestedWallMm != 1.2
                || intent.requestedLayerMm == null || intent.requestedLayerMm != 0.24) {
            throw new AssertionError("SmartSliceIntent Java constructor or public fields changed");
        }

        SmartSliceResult result = new SmartSliceResult(GeometryAnalysis.analyze(null));
        result.nozzleMm = 0.4;
        result.layerMm = 0.28;
        result.walls = 3;
        result.infillPercent = 22;
        result.infillPattern = "gyroid";
        result.support = "nur bei Bedarf";
        result.seam = "Rückseite";
        result.reasons.add("Testgrund");
        result.warnings.add("Testhinweis");
        result.changes.put("layer_height", "0.28");
        if (result.geometry.objectCount != 0 || result.reasons.size() != 1 || result.warnings.size() != 1
                || !result.summary().contains("Modell: 0,0 × 0,0 × 0,0 mm · 0 Objekt(e)")
                || !result.summary().contains("Wände: 3 · Infill: 22 % gyroid")
                || !result.summary().contains("Hinweise")) {
            throw new AssertionError("SmartSliceResult public data or localized summary changed");
        }
        String changes = result.changeSummary(Collections.singletonMap("layer_height", "0.20"));
        if (!changes.contains("Schichthöhe: 0,20 mm → 0,28 mm")) {
            throw new AssertionError("SmartSliceResult change summary lost readable German units or decimal formatting: " + changes);
        }

        try {
            GeometryAnalysis.class.getMethod("analyze", Model.class);
            boolean exposesCallableGeometryConstructor = Arrays.stream(GeometryAnalysis.class.getConstructors())
                    .anyMatch(constructor -> !constructor.isSynthetic());
            if (!Modifier.isStatic(SmartSliceEngine.class.getMethod(
                    "analyze", GeometryAnalysis.class, SmartSliceIntent.class, ConfigObject.class).getModifiers())
                    || !Modifier.isStatic(SmartSliceEngine.class.getMethod(
                    "apply", SmartSliceResult.class).getModifiers())
                    || exposesCallableGeometryConstructor
                    || SmartSliceEngine.class.getConstructors().length != 0) {
                throw new AssertionError("Smart Slice geometry and engine Java entry points changed");
            }
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Smart Slice public Java methods changed during Kotlin migration", error);
        }

        Native.resetMockState();
        Model model = new Model();
        GeometryAnalysis initialGeometry = GeometryAnalysis.analyze(model);
        if (initialGeometry.objectCount != 1 || initialGeometry.widthMm != 100.0
                || initialGeometry.depthMm != 100.0 || initialGeometry.heightMm != 100.0
                || initialGeometry.tall || initialGeometry.slender || initialGeometry.wide
                || GeometryAnalysis.analyze(model) != initialGeometry) {
            throw new AssertionError("GeometryAnalysis measurements, shape flags or revision cache changed");
        }
        model.translate(0, 10.0, 0.0, 0.0);
        GeometryAnalysis movedGeometry = GeometryAnalysis.analyze(model);
        if (movedGeometry == initialGeometry || movedGeometry.modelRevision != 1L || movedGeometry.widthMm != 100.0
                || movedGeometry.depthMm != 100.0
                || GeometryAnalysis.analyze(model) != movedGeometry) {
            throw new AssertionError("GeometryAnalysis cache must invalidate when native model geometry changes: "
                    + "same=" + (movedGeometry == initialGeometry) + ", revision=" + movedGeometry.modelRevision
                    + ", bounds=" + movedGeometry.widthMm + "x" + movedGeometry.depthMm);
        }
        model.release();
        GeometryAnalysis emptyGeometry = GeometryAnalysis.analyze(null);
        if (emptyGeometry.objectCount != 0 || emptyGeometry.modelRevision != -1L
                || emptyGeometry.widthMm != 0.0 || emptyGeometry.volumeEnvelopeMm3 != 0.0) {
            throw new AssertionError("GeometryAnalysis must safely report an absent model");
        }

        try {
            java.lang.reflect.Field instance = PrintConfigDef.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not isolate the Smart Slice option-schema test", error);
        }
        ConfigObject config = new ConfigObject();
        config.put("nozzle_diameter", "0.4");
        config.put("line_width", "0.42");
        config.put("filament_type", "PETG");
        config.put("top_shell_layers", "4");
        config.put("bottom_shell_layers", "4");
        config.put("spiral_mode", "0");
        config.put("bridge_flow", "1.0");
        config.put("bridge_speed", "30");
        SmartSliceIntent vaseIntent = new SmartSliceIntent(
                SmartSliceIntent.Purpose.VASE, SmartSliceIntent.Priority.BALANCED, null, 0.1);
        SmartSliceResult vase = SmartSliceEngine.analyze(emptyGeometry, vaseIntent, config);
        if (vase.layerMm != 0.1 || vase.walls != 1 || vase.infillPercent != 0
                || !"line".equals(vase.infillPattern)
                || !"0".equals(vase.changes.get("top_shell_layers"))
                || !"8".equals(vase.changes.get("bottom_shell_layers"))
                || !"1".equals(vase.changes.get("spiral_mode"))
                || !"0.9".equals(vase.changes.get("bridge_flow"))
                || !"25".equals(vase.changes.get("bridge_speed"))) {
            throw new AssertionError("Smart Slice vase mode must retain zero top shells and apply PETG bridge settings");
        }
    }

    private static void assertAppBootScheduler() {
        try {
            AppBoot.class.getConstructor();
            if (!Modifier.isStatic(AppBoot.class.getMethod("run", List.class).getModifiers())) {
                throw new AssertionError("AppBoot.run(List) must remain a Java static method");
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("AppBoot public Java entry points changed", e);
        }

        List<String> executionOrder = new ArrayList<>();
        SchedulerWorkerTask worker = new SchedulerWorkerTask(() -> executionOrder.add("worker"));
        SchedulerMainTask dependent = new SchedulerMainTask(() -> executionOrder.add("dependent"));
        AppBoot.run(Arrays.asList(worker, dependent));
        if (!executionOrder.equals(Arrays.asList("worker", "dependent"))) {
            throw new AssertionError("AppBoot did not wait for the worker dependency before running its main-thread task");
        }
    }

    private static void assertExtensibleMarkerEvents(Class<?>... eventTypes) {
        for (Class<?> eventType : eventTypes) {
            if (Modifier.isFinal(eventType.getModifiers())) {
                throw new AssertionError(eventType.getSimpleName() + " must remain extensible for Java callers");
            }
            try {
                eventType.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new AssertionError(eventType.getSimpleName() + " must retain its public no-argument constructor", e);
            }
        }
    }

    private static void assertBootTask(BootTask task, boolean workerThread, boolean nonCritical, String name) {
        if (task.workerThread != workerThread || task.nonCritical != nonCritical || task.priority != (workerThread ? -20 : 0)) {
            throw new AssertionError("Boot scheduling contract changed for " + name);
        }
        if (task.run == null || task.dependencies == null) {
            throw new AssertionError("Boot task executable or dependency list is missing for " + name);
        }
    }

    private static void assertTaskDependencies(BootTask task, Class<?>... expected) {
        if (!task.dependencies.equals(Arrays.asList(expected))) {
            throw new AssertionError("Boot task dependency order changed: " + task.getClass().getSimpleName());
        }
    }

    private static final class SchedulerWorkerTask extends BootTask {
        SchedulerWorkerTask(Runnable run) {
            super(run);
            onWorker();
        }
    }

    private static final class SchedulerMainTask extends BootTask {
        SchedulerMainTask(Runnable run) {
            super(Collections.singletonList(SchedulerWorkerTask.class), run);
        }
    }
}
