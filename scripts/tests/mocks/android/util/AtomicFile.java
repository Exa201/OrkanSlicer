package android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** JVM test implementation for the Android atomic-file persistence contract. */
public final class AtomicFile {
    private final File baseFile;
    private final File backupFile;

    public AtomicFile(File baseFile) {
        this.baseFile = baseFile;
        this.backupFile = new File(baseFile.getPath() + ".bak");
    }

    public FileOutputStream startWrite() throws IOException {
        File parent = baseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create config directory: " + parent);
        }
        if (baseFile.exists()) {
            if (backupFile.exists()) {
                if (!baseFile.delete()) throw new IOException("Could not clear old config file");
            } else if (!baseFile.renameTo(backupFile)) {
                throw new IOException("Could not preserve previous config file");
            }
        }
        return new FileOutputStream(baseFile);
    }

    public void finishWrite(FileOutputStream stream) {
        closeAndSync(stream);
        if (backupFile.exists() && !backupFile.delete()) {
            throw new IllegalStateException("Could not remove previous config backup");
        }
    }

    public void failWrite(FileOutputStream stream) {
        closeAndSync(stream);
        if (baseFile.exists() && !baseFile.delete()) {
            throw new IllegalStateException("Could not remove incomplete config file");
        }
        if (backupFile.exists() && !backupFile.renameTo(baseFile)) {
            throw new IllegalStateException("Could not restore previous config file");
        }
    }

    public FileInputStream openRead() throws IOException {
        if (backupFile.exists()) {
            if (baseFile.exists() && !baseFile.delete()) {
                throw new IOException("Could not remove incomplete config file");
            }
            if (!backupFile.renameTo(baseFile)) {
                throw new IOException("Could not restore previous config file");
            }
        }
        return new FileInputStream(baseFile);
    }

    private static void closeAndSync(FileOutputStream stream) {
        if (stream == null) return;
        try {
            stream.flush();
            stream.getFD().sync();
        } catch (IOException ignored) {
            // The production Android implementation reports failures through AtomicFile.
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Closing is best-effort in the JVM test double.
            }
        }
    }
}
