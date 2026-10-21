package ru.ytkab0bp.slicebeam.components;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Base64;

import java.io.File;
import java.io.FileNotFoundException;

/** Isolated instrumentation-only SAF destination; writes stay inside the test APK cache. */
public final class TestSaveDestinationProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"w".equals(mode) && !"wt".equals(mode)) {
            throw new IllegalArgumentException("Unsupported test destination mode: " + mode);
        }
        File target = target(uri);
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_WRITE_ONLY
                | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        File target = target(uri);
        MatrixCursor cursor = new MatrixCursor(new String[] {"payload"});
        if (target.isFile()) {
            try (java.io.FileInputStream input = new java.io.FileInputStream(target)) {
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                cursor.addRow(new Object[] {Base64.encodeToString(
                        output.toByteArray(), Base64.NO_WRAP)});
            } catch (java.io.IOException error) {
                throw new IllegalStateException(error);
            }
        }
        return cursor;
    }
    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        File target = target(uri);
        return !target.exists() || target.delete() ? 1 : 0;
    }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { return 0; }

    private File target(Uri uri) {
        return new File(requireContext().getCacheDir(),
                "save-destination-" + uri.getLastPathSegment() + ".bin");
    }
}
