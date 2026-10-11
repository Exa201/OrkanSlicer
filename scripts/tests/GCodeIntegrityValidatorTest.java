package scripts.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import ru.ytkab0bp.slicebeam.utils.GCodeIntegrityValidator;

public final class GCodeIntegrityValidatorTest {
    public static void main(String[] args) throws Exception {
        testCompleteFilePasses();
        testMissingFinalLayerIsRejected();
        testLayerNumberingGapIsRejected();
        testEarlyCompletionIsRejected();
        testFinalLayerWithoutExtrusionIsRejected();
        testMissingEndMarkerIsRejected();
        testHeightMismatchIsRejected();
        testHeaderRecognitionIsLocaleIndependent();
        if (args.length > 0) testRealSlicedFiles(args);
        System.out.println("GCodeIntegrityValidatorTest passed: layer completeness, final extrusion, height and end order.");
    }

    private static void testCompleteFilePasses() throws Exception {
        GCodeIntegrityValidator.Result result = validate(completeFile());
        check(result.isValid(), "complete G-code should pass: " + result.details);
        equal(3, result.expectedLayers, "expected layers");
        equal(3, result.actualLayers, "actual layers");
    }

    private static void testMissingFinalLayerIsRejected() throws Exception {
        String gcode = completeFile().replace("; total layers count = 3", "; total layers count = 4");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.LAYER_COUNT_MISMATCH);
    }

    private static void testEarlyCompletionIsRejected() throws Exception {
        String gcode = completeFile().replace(";LAYER_CHANGE\n;Z:0.6", "M73 P100\n;LAYER_CHANGE\n;Z:0.6");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.END_BEFORE_LAST_LAYER);
    }

    private static void testLayerNumberingGapIsRejected() throws Exception {
        String gcode = completeFile().replace("BEFORE_LAYER_CHANGE 1 @ 0.4mm", "BEFORE_LAYER_CHANGE 4 @ 0.4mm");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.NUMBERING_GAP);
    }

    private static void testFinalLayerWithoutExtrusionIsRejected() throws Exception {
        String gcode = completeFile().replace("G1 X3 E1", "G1 X3 F3000");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.LAST_LAYER_HAS_NO_EXTRUSION);
    }

    private static void testMissingEndMarkerIsRejected() throws Exception {
        String gcode = completeFile().replace("M73 P100 R0\n", "").replace("; EXECUTABLE_BLOCK_END\n", "");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.MISSING_COMPLETION_MARKER);
    }

    private static void testHeightMismatchIsRejected() throws Exception {
        String gcode = completeFile().replace("; max_z_height: 0.6", "; max_z_height: 0.7");
        expectFailure(gcode, GCodeIntegrityValidator.Failure.HEIGHT_MISMATCH);
    }

    private static void testHeaderRecognitionIsLocaleIndependent() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            GCodeIntegrityValidator.Result result = validate(completeFile()
                    .replace("; total layer number: 3", "; TOTAL LAYER NUMBER: 3")
                    .replace("; max_z_height: 0.6", "; MAX_Z_HEIGHT: 0.6"));
            check(result.isValid(), "header markers should be case-insensitive in every locale: " + result.details);
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static void testRealSlicedFile(String path) {
        GCodeIntegrityValidator.Result result = GCodeIntegrityValidator.validate(new File(path));
        check(result.isValid(), "real sliced file should pass: " + result.failure + " " + result.details);
        check(result.expectedLayers > 0, "real file should declare a positive layer count");
        equal(result.expectedLayers, result.actualLayers, "real file expected/actual layers");
    }

    private static void testRealSlicedFiles(String[] paths) {
        StringBuilder failures = new StringBuilder();
        for (String path : paths) {
            try {
                testRealSlicedFile(path);
                System.out.println("G-code complete: " + path);
            } catch (AssertionError error) {
                if (failures.length() > 0) failures.append("\n");
                failures.append(path).append(": ").append(error.getMessage());
                System.err.println("G-code failed validation: " + path + " — " + error.getMessage());
            }
        }
        if (failures.length() > 0) throw new AssertionError(failures.toString());
    }

    private static void expectFailure(String gcode, GCodeIntegrityValidator.Failure expected) throws Exception {
        GCodeIntegrityValidator.Result result = validate(gcode);
        check(result.failure == expected, "expected " + expected + " but got " + result.failure + ": " + result.details);
    }

    private static GCodeIntegrityValidator.Result validate(String gcode) throws Exception {
        File file = Files.createTempFile("integrity-test", ".gcode").toFile();
        try {
            Files.write(file.toPath(), gcode.getBytes(StandardCharsets.UTF_8));
            return GCodeIntegrityValidator.validate(file);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static String completeFile() {
        return "; HEADER_BLOCK_START\n" +
                "; total layer number: 3\n" +
                "; max_z_height: 0.6\n" +
                "; HEADER_BLOCK_END\n" +
                ";LAYER_CHANGE\n;Z:0.2\n; BEFORE_LAYER_CHANGE 0 @ 0.2mm\nG1 X1 E1\n" +
                ";LAYER_CHANGE\n;Z:0.4\n; BEFORE_LAYER_CHANGE 1 @ 0.4mm\nG1 X2 E1\n" +
                ";LAYER_CHANGE\n;Z:0.6\n; BEFORE_LAYER_CHANGE 2 @ 0.6mm\nG1 X3 E1\n" +
                "M104 S0\nM73 P100 R0\n; EXECUTABLE_BLOCK_END\n" +
                "; total layers count = 3\n";
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void equal(int expected, int actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
}
