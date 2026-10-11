package ru.ytkab0bp.slicebeam.utils

import java.io.File

class UploadFileValidator private constructor() {
    companion object {
        @JvmStatic
        fun isReadyForUpload(file: File?): Boolean =
            file != null && file.isFile && file.canRead() && file.length() > 0
    }
}
