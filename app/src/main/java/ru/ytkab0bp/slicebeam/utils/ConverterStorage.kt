package ru.ytkab0bp.slicebeam.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import ru.ytkab0bp.slicebeam.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale

/** Remembers a user-selected root folder and stores each local generator in its own child folder. */
class ConverterStorage private constructor() {
    companion object {
        private const val PREF_TREE = "converter_storage_tree_uri"
        private const val PREF_FIRST_PROMPT = "converter_storage_first_prompt"

        internal fun hasWritablePersistableGrant(flags: Int): Boolean =
            flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0

        internal fun isPermissionFailure(error: Throwable): Boolean {
            var current: Throwable? = error
            while (current != null) {
                if (current is SecurityException) return true
                current = current.cause
            }
            return false
        }

        @JvmStatic
        fun getRoot(): Uri? {
            val saved = Prefs.getPrefs()?.getString(PREF_TREE, null)
            return saved?.let(Uri::parse)
        }

        @JvmStatic
        fun hasRoot(): Boolean = getRoot() != null

        /** Forget a tree whose persisted grant has been revoked by Android or its provider. */
        @JvmStatic
        fun clearRoot(context: Context) {
            val root = getRoot()
            Prefs.getPrefs()?.edit()?.remove(PREF_TREE)?.apply()
            if (root != null) {
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        root,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // The grant may already have been revoked; clearing the stale preference is enough.
                } catch (_: IllegalArgumentException) {
                    // A provider can reject release for a URI that is no longer persisted.
                }
            }
        }

        @JvmStatic
        fun markAndShouldPromptFirstLaunch(): Boolean {
            val prefs = Prefs.getPrefs() ?: return false
            if (prefs.getBoolean(PREF_FIRST_PROMPT, false)) return false
            prefs.edit().putBoolean(PREF_FIRST_PROMPT, true).apply()
            return true
        }

        @JvmStatic
        fun createFolderPickerIntent(): Intent {
            val picker = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            if (Build.VERSION.SDK_INT >= 26) {
                val documents = DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", "primary:Documents"
                )
                picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documents)
            }
            return picker
        }

        @JvmStatic
        @Throws(SecurityException::class)
        fun persistSelectedRoot(activity: Activity, result: Intent?) {
            val tree = result?.data ?: return
            val resultFlags = result.flags
            val canRead = resultFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
            val canWrite = hasWritablePersistableGrant(resultFlags)
            if (!canWrite) {
                throw SecurityException("Der ausgewählte Ordner wurde nicht mit Schreibzugriff freigegeben.")
            }
            if (canRead && canWrite) {
                activity.contentResolver.takePersistableUriPermission(
                    tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } else if (canRead) {
                activity.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else if (canWrite) {
                activity.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            Prefs.getPrefs()?.edit()?.putString(PREF_TREE, tree.toString())?.apply()
        }

        @JvmStatic
        fun getRootDisplayName(context: Context): String {
            val tree = getRoot() ?: return context.getString(R.string.ConverterFolderNotSet)
            val rootDocument = DocumentsContract.buildDocumentUriUsingTree(
                tree, DocumentsContract.getTreeDocumentId(tree)
            )
            try {
                context.contentResolver.query(
                    rootDocument,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrEmpty()) return name
                    }
                }
            } catch (_: Exception) {
            }
            return tree.lastPathSegment ?: tree.toString()
        }

        @JvmStatic
        @Throws(IOException::class)
        fun saveGeneratedFile(context: Context, root: Uri?, source: File?): String {
            val sourceFile = source
            if (root == null || sourceFile == null || !sourceFile.isFile) {
                throw IOException("Der Speicherordner ist nicht verfügbar.")
            }
            val resolver = context.contentResolver
            val folderName = folderFor(sourceFile.name)
            val rootId = try {
                DocumentsContract.getTreeDocumentId(root)
            } catch (error: Exception) {
                throw IOException("Der ausgewählte Speicherordner ist ungültig.", error)
            }
            val rootDocument = DocumentsContract.buildDocumentUriUsingTree(root, rootId)
            var folder = findChild(resolver, root, rootId, folderName, true)
            if (folder == null) {
                folder = DocumentsContract.createDocument(
                    resolver,
                    rootDocument,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    folderName
                )
            }
            val destinationFolder = folder
                ?: throw IOException("Der Unterordner „$folderName“ konnte nicht erstellt werden.")

            val mime = mimeFor(sourceFile.name)
            var fileName = sourceFile.name
            var suffix = 2
            while (containsChild(resolver, root, DocumentsContract.getDocumentId(destinationFolder), fileName)) {
                val ext = extensionOf(sourceFile.name)
                val stem = if (ext.isEmpty()) sourceFile.name
                else sourceFile.name.substring(0, sourceFile.name.length - ext.length - 1)
                fileName = stem + " (" + suffix++ + ")" + if (ext.isEmpty()) "" else ".$ext"
            }
            val destination = DocumentsContract.createDocument(resolver, destinationFolder, mime, fileName)
                ?: throw IOException("Die Datei konnte im Konverterordner nicht angelegt werden.")

            FileInputStream(sourceFile).use { input ->
                val output = resolver.openOutputStream(destination, "w")
                    ?: throw IOException("Der Speicheranbieter hat keinen Dateistream geöffnet.")
                output.use { sink -> input.copyTo(sink, 16 * 1024) }
            }
            return folderName
        }

        private fun findChild(
            resolver: ContentResolver,
            tree: Uri,
            parentId: String,
            name: String,
            folderOnly: Boolean
        ): Uri? {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            return try {
                resolver.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (name != cursor.getString(1)) continue
                        if (folderOnly && DocumentsContract.Document.MIME_TYPE_DIR != cursor.getString(2)) continue
                        return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                    }
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun containsChild(resolver: ContentResolver, tree: Uri, parentId: String, name: String): Boolean {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            return try {
                resolver.query(
                    children,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) if (name == cursor.getString(0)) return true
                    false
                } ?: false
            } catch (_: Exception) {
                false
            }
        }

        private fun extensionOf(name: String): String {
            val dot = name.lastIndexOf('.')
            return if (dot < 0) "" else name.substring(dot + 1)
        }

        private fun mimeFor(name: String): String = when (extensionOf(name).lowercase(Locale.ROOT)) {
            "stl" -> "model/stl"
            "3mf" -> "model/3mf"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

        private fun folderFor(name: String): String {
            val extension = extensionOf(name).lowercase(Locale.ROOT)
            return if (extension.isEmpty()) "Weitere Formate" else extension.uppercase(Locale.ROOT)
        }
    }
}
