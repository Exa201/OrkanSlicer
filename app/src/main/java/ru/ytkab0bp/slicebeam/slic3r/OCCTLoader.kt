package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.BuildConfig

/** Loads the OpenCascade libraries that match the selected native bundle. */
class OCCTLoader private constructor() {
    companion object {
        private val SHARED_LIBS = listOf(
            "TKXCAF",
            "TKLCAF",
            "TKCAF",
            "TKCDF",
            "TKV3d",
            "TKMesh",
            "TKXMesh",
            "TKBO",
            "TKPrim",
            "TKHLR",
            "TKShHealing",
            "TKTopAlgo",
            "TKGeomAlgo",
            "TKGeomBase",
            "TKBRep",
            "TKG3d",
            "TKG2d",
            "TKMath",
            "TKernel",
        )

        @JvmStatic
        fun getLibraries(@Suppress("UNUSED_PARAMETER") usePrebuiltNative: Boolean): List<String> =
            // The fast-build cache is now refreshed from our own successful source-built APK,
            // so both build modes use the same OpenCascade XDE library family.
            listOf("TKXDE", "TKXDESTEP") + SHARED_LIBS

        @JvmStatic
        fun load() {
            getLibraries(BuildConfig.USE_PREBUILT_NATIVE).forEach(System::loadLibrary)
        }
    }
}
