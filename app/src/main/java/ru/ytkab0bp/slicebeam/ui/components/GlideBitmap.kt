package ru.ytkab0bp.slicebeam.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

internal enum class GlideBitmapShape {
    CIRCLE,
    ROUNDED,
}

/** Loads a bounded Glide bitmap into Compose and cancels the request with the composition. */
@Composable
internal fun rememberGlideBitmap(
    model: String?,
    size: Dp,
    shape: GlideBitmapShape,
    cornerRadius: Dp = size / 4,
): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx().coerceAtLeast(1) }
    val cornerRadiusPx = with(density) { cornerRadius.roundToPx().coerceAtLeast(1) }
    var bitmap by remember(model) { mutableStateOf<ImageBitmap?>(null) }

    DisposableEffect(context, model, sizePx, shape, cornerRadiusPx) {
        val url = model?.takeIf(String::isNotBlank)
        if (url == null) {
            bitmap = null
            onDispose { }
        } else {
            bitmap = null
            val requestManager = Glide.with(context)
            val target = object : CustomTarget<Bitmap>(sizePx, sizePx) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmap = resource.asImageBitmap()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    bitmap = null
                }
            }
            val request = requestManager.asBitmap().load(url).override(sizePx, sizePx)
            when (shape) {
                GlideBitmapShape.CIRCLE -> request.circleCrop()
                GlideBitmapShape.ROUNDED -> request.transform(CenterCrop(), RoundedCorners(cornerRadiusPx))
            }
            request.into(target)
            onDispose { requestManager.clear(target) }
        }
    }

    return bitmap
}
