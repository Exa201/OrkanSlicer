package scripts.tests;

import java.io.File;
import java.nio.file.Files;

import ru.ytkab0bp.slicebeam.utils.UploadFileValidator;

public final class UploadFileValidatorTest {
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("orca-upload-check").toFile();
        File empty = new File(directory, "empty.gcode");
        check(empty.createNewFile(), "empty fixture created");
        File populated = new File(directory, "ready.gcode");
        Files.write(populated.toPath(), "G1 X1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        check(!UploadFileValidator.isReadyForUpload(null), "null file is rejected");
        check(!UploadFileValidator.isReadyForUpload(new File(directory, "missing.gcode")),
                "missing file is rejected");
        check(!UploadFileValidator.isReadyForUpload(directory), "directory is rejected");
        check(!UploadFileValidator.isReadyForUpload(empty), "empty file is rejected");
        check(UploadFileValidator.isReadyForUpload(populated), "non-empty readable file is accepted");

        System.out.println("UploadFileValidatorTest passed: missing, empty and non-file inputs are rejected.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
