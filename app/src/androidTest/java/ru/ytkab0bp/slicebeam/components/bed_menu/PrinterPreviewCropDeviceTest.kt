package ru.ytkab0bp.slicebeam.components.bed_menu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrinterPreviewCropDeviceTest {
    @Test
    fun cropRemovesBlankViewportButPreservesWholeSceneBounds() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        Canvas(source).apply {
            drawColor(Color.rgb(35, 39, 45))
            drawRect(30f, 40f, 70f, 60f, Paint().apply { color = Color.WHITE })
        }

        val cropped = SliceUploadController.cropPrinterPreviewToScene(source)
        try {
            assertTrue("Crop should remove the empty portrait margins", cropped.width < source.width)
            assertTrue("Crop should retain content on both horizontal edges", cropped.width >= 40)
            assertTrue("Crop should retain content on both vertical edges", cropped.height >= 20)
            var preservedModelPixels = 0
            for (y in 0 until cropped.height) for (x in 0 until cropped.width) {
                if (cropped.getPixel(x, y) == Color.WHITE) preservedModelPixels++
            }
            assertTrue("The crop must preserve every model pixel", preservedModelPixels == 800)
        } finally {
            if (cropped !== source) cropped.recycle()
            source.recycle()
        }
    }
}
