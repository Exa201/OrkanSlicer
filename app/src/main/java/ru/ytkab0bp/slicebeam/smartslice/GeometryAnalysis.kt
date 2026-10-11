package ru.ytkab0bp.slicebeam.smartslice

import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.util.WeakHashMap

/** Fast pass based only on geometry facts exposed by the existing native Orca bridge. */
class GeometryAnalysis private constructor(
    objectCount: Int,
    widthMm: Double,
    depthMm: Double,
    heightMm: Double,
    modelRevision: Long,
) {
    @JvmField val objectCount: Int = objectCount
    @JvmField val modelRevision: Long = modelRevision
    @JvmField val widthMm: Double = Math.max(0.0, widthMm)
    @JvmField val depthMm: Double = Math.max(0.0, depthMm)
    @JvmField val heightMm: Double = Math.max(0.0, heightMm)
    @JvmField val footprintMm2: Double = this.widthMm * this.depthMm
    @JvmField val volumeEnvelopeMm3: Double = footprintMm2 * this.heightMm

    @JvmField val tall: Boolean
    @JvmField val slender: Boolean
    @JvmField val small: Boolean
    @JvmField val verySmall: Boolean
    @JvmField val wide: Boolean

    init {
        val base = Math.max(1.0, Math.min(this.widthMm, this.depthMm))
        tall = this.heightMm > base * 2.2
        slender = this.heightMm > base * 3.5
        small = Math.max(this.widthMm, Math.max(this.depthMm, this.heightMm)) < 35.0
        verySmall = Math.max(this.widthMm, Math.max(this.depthMm, this.heightMm)) < 18.0
        wide = Math.max(this.widthMm, this.depthMm) > 160.0
    }

    companion object {
        private val CACHE = WeakHashMap<Model, CachedAnalysis>()

        @JvmStatic
        fun analyze(model: Model?): GeometryAnalysis {
            if (model == null) return GeometryAnalysis(0, 0.0, 0.0, 0.0, -1L)
            val revision = model.geometryRevision
            synchronized(CACHE) {
                val cached = CACHE[model]
                if (cached != null && cached.revision == revision) return cached.analysis
            }
            val objects = model.objectsCount
            if (objects == 0) {
                return cache(model, revision, GeometryAnalysis(0, 0.0, 0.0, 0.0, revision))
            }
            val min: Vec3d = model.boundingBoxExactMin
            val max: Vec3d = model.boundingBoxExactMax
            val analysis = GeometryAnalysis(
                objects,
                max.x - min.x,
                max.y - min.y,
                max.z - min.z,
                revision,
            )
            return cache(model, revision, analysis)
        }

        private fun cache(model: Model, revision: Long, result: GeometryAnalysis): GeometryAnalysis {
            synchronized(CACHE) {
                if (model.geometryRevision == revision) CACHE[model] = CachedAnalysis(revision, result)
            }
            return result
        }
    }

    private class CachedAnalysis(val revision: Long, val analysis: GeometryAnalysis)
}
