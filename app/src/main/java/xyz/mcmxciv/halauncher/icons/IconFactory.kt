/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.mcmxciv.halauncher.icons

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import xyz.mcmxciv.halauncher.R
import xyz.mcmxciv.halauncher.models.InvariantDeviceProfile
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

class IconFactory @Inject constructor(
    private val context: Context,
    private val iconNormalizer: IconNormalizer,
    private val invariantDeviceProfile: InvariantDeviceProfile,
    private val shadowGenerator: ShadowGenerator
) : AutoCloseable {
    private var colorExtractorDisabled = false
    private var wrapperBackgroundColor = DEFAULT_WRAPPER_BACKGROUND
    private val canvas = Canvas()
    private val oldBounds = Rect()

    init {
        canvas.drawFilter = PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG)
        clear()
    }

    override fun close() {
        clear()
    }

    fun createIconBitmap(icon: Drawable): Bitmap {
        val scale = FloatArray(1)
        val normalizedIcon = normalizeAndWrapToAdaptiveIcon(icon, scale)
        val bitmap = createIconBitmap(normalizedIcon, scale[0])

        if (ATLEAST_OREO && icon is AdaptiveIconDrawable) {
            canvas.setBitmap(bitmap)
            shadowGenerator.recreateIcon(Bitmap.createBitmap(bitmap), canvas)
            canvas.setBitmap(null)
        }

        return bitmap
    }

    private fun createIconBitmap(icon: Drawable, scale: Float): Bitmap {
        val size = invariantDeviceProfile.iconBitmapSize
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        canvas.setBitmap(bitmap)
        oldBounds.set(icon.bounds)

        if (ATLEAST_OREO && icon is AdaptiveIconDrawable) {
            val offset = max(ceil(BLUR_FACTOR * size), round(size * (1 - scale) / 2)).toInt()
            icon.setBounds(offset, offset, size - offset, size - offset)
            icon.draw(canvas)
        }
        else {
            if (icon is BitmapDrawable) {
                val bmp = icon.bitmap

                if (bmp.density == Bitmap.DENSITY_NONE) {
                    icon.setTargetDensity(context.resources.displayMetrics)
                }
            }

            var width = size
            var height = size

            val intrinsicWidth = icon.intrinsicWidth
            val intrinsicHeight = icon.intrinsicHeight

            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                val ratio = intrinsicWidth.toFloat() / intrinsicHeight

                if (intrinsicWidth > intrinsicHeight) {
                    height = (width / ratio).toInt()
                }

                if (intrinsicHeight > intrinsicWidth) {
                    width = (height / ratio).toInt()
                }
            }

            val left = (size - width) / 2
            val top = (size - height) / 2

            icon.setBounds(left, top, left + width, top + height)
            canvas.save()
            canvas.scale(scale, scale, size.toFloat() / 2, size.toFloat() / 2)
            icon.draw(canvas)
            canvas.restore()
        }

        icon.bounds = oldBounds
        canvas.setBitmap(null)
        return bitmap
    }

//    fun createIcon(icon: Drawable): Drawable {
//        return normalizeAndWrapToAdaptiveIcon(icon)
//    }

    private fun normalizeAndWrapToAdaptiveIcon(icon: Drawable, outScale: FloatArray): Drawable {
        var scale: Float
        val outBounds: RectF? = null

        if (ATLEAST_OREO) {
            val normalizedIcon: Drawable
            val wrapperIcon = context
                .getDrawable(R.drawable.adaptive_icon_drawable_wrapper)?.mutate()
            val adaptiveIconDrawable = wrapperIcon as AdaptiveIconDrawable
            adaptiveIconDrawable.setBounds(0, 0, 1, 1)

            val outShape = BooleanArray(1)
            scale = iconNormalizer
                .getScale(icon, outBounds, adaptiveIconDrawable.iconMask, outShape)

            if (icon !is AdaptiveIconDrawable && !outShape[0]) {
                val fixedScaleDrawable = adaptiveIconDrawable.foreground as FixedScaleDrawable
                fixedScaleDrawable.drawable = icon
                fixedScaleDrawable.setScale(scale)
                normalizedIcon = adaptiveIconDrawable
                scale = iconNormalizer
                    .getScale(normalizedIcon, outBounds, null, null)
                (adaptiveIconDrawable.background as ColorDrawable).color = wrapperBackgroundColor
            }
        }
        else {
            scale = iconNormalizer.getScale(icon, outBounds, null, null)
        }

        outScale[0] = scale
        return icon
    }

    private fun clear() {
        wrapperBackgroundColor = DEFAULT_WRAPPER_BACKGROUND
        colorExtractorDisabled = false
    }

    companion object {
        private const val DEFAULT_WRAPPER_BACKGROUND = Color.WHITE
        private const val BLUR_FACTOR = 0.5f / 48
        private val ATLEAST_OREO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}