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

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.toRect
import xyz.mcmxciv.halauncher.utils.GraphicsUtils
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.math.*

class IconNormalizer(context: Context,
                     iconBitmapSize: Int,
                     private val enableShapeDetection: Boolean
) {
    private val maxSize: Int = iconBitmapSize * 2
    private val bitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ALPHA_8)
    private val canvas = Canvas(bitmap)
    private val pixels = ByteArray(maxSize * maxSize)
    private val leftBorder = FloatArray(maxSize)
    private val rightBorder = FloatArray(maxSize)
    private val bounds = Rect()
    private val adaptiveIconBounds = RectF()
    private val paintMaskShape = Paint()
    private val paintMaskShapeOutline = Paint()
    private val shapePath = Path()
    private val matrix = Matrix()
    private var adaptiveIconScale = SCALE_NOT_INITIALIZED

    init {
        paintMaskShape.color = Color.RED
        paintMaskShape.style = Paint.Style.FILL
        paintMaskShape.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)

        paintMaskShapeOutline.strokeWidth = 2 * context.resources.displayMetrics.density
        paintMaskShapeOutline.style = Paint.Style.STROKE
        paintMaskShapeOutline.color = Color.BLACK
        paintMaskShapeOutline.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    /**
     * Returns if the shape of the icon is same as the path.
     * For this method to work, the shape path bounds should be in [0,1]x[0,1] bounds.
     */
    private fun isShape(maskPath: Path): Boolean {
        // Condition1:
        // If width and height of the path not close to a square, then the icon shape is
        // not same as the mask shape.
        val iconRatio = bounds.width() / bounds.height()
        if (abs(iconRatio - 1) > BOUND_RATIO_MARGIN) {
            return false
        }

        // Condition 2:
        // Actual icon (white) and the fitted shape (e.g., circle)(red) XOR operation
        // should generate transparent image, if the actual icon is equivalent to the shape.

        // Fit the shape within the icon's bounding box
        matrix.reset()
        matrix.setScale(bounds.width().toFloat(), bounds.height().toFloat())
        matrix.postTranslate(bounds.left.toFloat(), bounds.top.toFloat())
        maskPath.transform(matrix, shapePath)

        canvas.drawPath(shapePath, paintMaskShape)
        canvas.drawPath(shapePath, paintMaskShapeOutline)

        return isTransparentBitmap()
    }

    /**
     * Used to determine if certain the bitmap is transparent.
     */
    private fun isTransparentBitmap(): Boolean {
        val buffer = ByteBuffer.wrap(pixels)
        buffer.rewind()
        bitmap.copyPixelsToBuffer(buffer)

        var y = bounds.top
        // buffer position
        var index = y * maxSize
        // buffer shift after every row, width of buffer = mMaxSize
        val rowSizeDiff = maxSize - bounds.right

        var sum = 0
        while (y < bounds.bottom) {
            index += bounds.left

            var x = bounds.left
            while (x < bounds.right) {
                if ((pixels[index] and 0xFF.toByte()) > MIN_VISIBLE_ALPHA) {
                    sum++
                }

                index++
                x++
            }

            index += rowSizeDiff
            y++
        }

        val percentageDiffPixels = sum.toFloat() / (bounds.width() * bounds.height())
        return percentageDiffPixels < PIXEL_DIFF_PERCENTAGE_THRESHOLD
    }

    /**
     * Returns the amount by which the {@param d} should be scaled (in both dimensions) so that it
     * matches the design guidelines for a launcher icon.
     *
     * We first calculate the convex hull of the visible portion of the icon.
     * This hull then compared with the bounding rectangle of the hull to find how closely it
     * resembles a circle and a square, by comparing the ratio of the areas. Note that this is not an
     * ideal solution but it gives satisfactory result without affecting the performance.
     *
     * This closeness is used to determine the ratio of hull area to the full icon size.
     * Refer {@link #MAX_CIRCLE_AREA_FACTOR} and {@link #MAX_SQUARE_AREA_FACTOR}
     *
     * @param outBounds optional rect to receive the fraction distance from each edge.
     */
    @Synchronized
    fun getScale(
        d: Drawable, outBounds: RectF?,
        path: Path?, outMaskShape: BooleanArray?
    ): Float {
        if (IconFactory.atleastOreo && d is AdaptiveIconDrawable) {
            adaptiveIconScale = normalizeAdaptiveIcon(d, maxSize, adaptiveIconBounds)
            outBounds?.set(adaptiveIconBounds)
            return adaptiveIconScale
        }

        var width = d.intrinsicWidth
        var height = d.intrinsicHeight

        if (width <= 0 || height <= 0) {
            width = if (width <= 0 || width > maxSize) maxSize else width
            height = if (height <= 0 || height > maxSize) maxSize else height
        }
        else if (width > maxSize || height > maxSize) {
            val max = max(width, height)
            width = maxSize * width / max
            height = maxSize * height / max
        }

        bitmap.eraseColor(Color.TRANSPARENT)
        d.setBounds(0, 0, width, height)
        d.draw(canvas)

        val buffer = ByteBuffer.wrap(pixels)
        buffer.rewind()
        bitmap.copyPixelsToBuffer(buffer)

        // Overall bounds of the visible icon.
        var topY = -1
        var bottomY = -1
        var leftX = maxSize + 1
        var rightX = -1

        // Create border by going through all pixels one row at a time and for each row find
        // the first and the last non-transparent pixel. Set those values to mLeftBorder and
        // mRightBorder and use -1 if there are no visible pixel in the row.

        // buffer position
        var index = 0
        // buffer shift after every row, width of buffer = mMaxSize
        val rowSizeDiff = maxSize - width
        // first and last position for any row.
        var firstX: Int
        var lastX: Int

        for (y in 0 until height) {
            lastX = -1
            firstX = lastX
            for (x in 0 until width) {
                if (pixels[index] and 0xFF.toByte() > MIN_VISIBLE_ALPHA) {
                    if (firstX == -1) {
                        firstX = x
                    }
                    lastX = x
                }
                index++
            }
            index += rowSizeDiff

            leftBorder[y] = firstX.toFloat()
            rightBorder[y] = lastX.toFloat()

            // If there is at least one visible pixel, update the overall bounds.
            if (firstX != -1) {
                bottomY = y
                if (topY == -1) {
                    topY = y
                }

                leftX = Math.min(leftX, firstX)
                rightX = Math.max(rightX, lastX)
            }
        }

        if (topY == -1 || rightX == -1) {
            // No valid pixels found. Do not scale.
            return 1f
        }

        convertToConvexArray(leftBorder, 1, topY, bottomY)
        convertToConvexArray(rightBorder, -1, topY, bottomY)

        // Area of the convex hull
        var area = 0f
        for (y in 0 until height) {
            if (leftBorder[y] <= -1) {
                continue
            }
            area += rightBorder[y] - leftBorder[y] + 1
        }

        bounds.left = leftX
        bounds.right = rightX
        bounds.top = topY
        bounds.bottom = bottomY

        outBounds?.set(
            bounds.left.toFloat() / width, bounds.top.toFloat() / height,
            1 - bounds.right.toFloat() / width,
            1 - bounds.bottom.toFloat() / height
        )

        if (outMaskShape != null && enableShapeDetection && outMaskShape.isNotEmpty()) {
            outMaskShape[0] = isShape(path!!)
        }
        // Area of the rectangle required to fit the convex hull
        val rectArea = ((bottomY + 1 - topY) * (rightX + 1 - leftX)).toFloat()
        return getScale(area, rectArea, (width * height).toFloat())
    }

    companion object {
        private const val TAG = "IconNormalizer"
        // Ratio of icon visible area to full icon size for a square shaped icon
        private const val MAX_SQUARE_AREA_FACTOR = 375.0f / 576
        // Ratio of icon visible area to full icon size for a circular shaped icon
        private const val MAX_CIRCLE_AREA_FACTOR = 380.0f / 576

        private const val CIRCLE_AREA_BY_RECT = Math.PI.toFloat() / 4

        // Slope used to calculate icon visible area to full icon size for any generic shaped icon.
        private const val LINEAR_SCALE_SLOPE =
            (MAX_CIRCLE_AREA_FACTOR - MAX_SQUARE_AREA_FACTOR) / (1 - CIRCLE_AREA_BY_RECT)

        private const val MIN_VISIBLE_ALPHA = 40

        // Shape detection related constants
        private const val BOUND_RATIO_MARGIN = .05f
        private const val PIXEL_DIFF_PERCENTAGE_THRESHOLD = 0.005f
        private const val SCALE_NOT_INITIALIZED = 0f

        // Ratio of the diameter of an normalized circular icon to the actual icon size.
        const val ICON_VISIBLE_AREA_FACTOR = 0.92f

        private fun getScale(hullArea: Float, boundingArea: Float, fullArea: Float): Float {
            val hullByRect = hullArea / boundingArea
            val scaleRequired = if (hullByRect < CIRCLE_AREA_BY_RECT) {
                MAX_CIRCLE_AREA_FACTOR
            }
            else {
                MAX_SQUARE_AREA_FACTOR + LINEAR_SCALE_SLOPE * (1 - hullByRect)
            }

            val areaScale = hullArea / fullArea
            return if (areaScale > scaleRequired) sqrt(scaleRequired / areaScale) else 1f
        }

        /**
         * @param d Should be AdaptiveIconDrawable
         * @param size Canvas size to use
         */
        @TargetApi(Build.VERSION_CODES.O)
        fun normalizeAdaptiveIcon(drawable: Drawable, size: Int, outBounds: RectF?): Float {
            val tempBounds = Rect(drawable.bounds)
            drawable.bounds = Rect(0, 0, size, size)
            val path = IconShape.shapePath //(drawable as AdaptiveIconDrawable).iconMask
            val region = Region()
            region.setPath(path, Region(0, 0, size, size))

            val hullBounds = region.bounds
            val hullArea = GraphicsUtils.getArea(region).toFloat()
            val sizeF = size.toFloat()

            outBounds?.set(
                hullBounds.left / sizeF,
                hullBounds.top / sizeF,
                1 - (hullBounds.right / sizeF),
                1 - (hullBounds.bottom / sizeF)
            )

            drawable.bounds = tempBounds
            return getScale(hullArea, hullArea, sizeF * sizeF)
        }

        /**
         * Modifies {@param xCoordinates} to represent a convex border. Fills in all missing values
         * (except on either ends) with appropriate values.
         * @param xCoordinates map of x coordinate per y.
         * @param direction 1 for left border and -1 for right border.
         * @param topY the first Y position (inclusive) with a valid value.
         * @param bottomY the last Y position (inclusive) with a valid value.
         */
        private fun convertToConvexArray(
            xCoordinates: FloatArray, direction: Int,
            topY: Int, bottomY: Int
        ) {
            val total = xCoordinates.size
            val angles = FloatArray(total - 1) // The tangent at each pixel.
            var last = -1    // Last valid y coordinate which didn't have a missing value
            var lastAngle = Float.MAX_VALUE
            var i = topY + 1

            while (i <= bottomY) {
                if (xCoordinates[i] > -1) {
                    var start = topY

                    if (lastAngle != Float.MAX_VALUE) {
                        var currentAngle = (xCoordinates[i] - xCoordinates[last]) / (i - last)
                        start = last

                        // If this position creates a concave angle, keep moving up until we find a
                        // position which creates a convex angle.
                        if ((currentAngle - lastAngle) * direction < 0) {
                            while (start > topY) {
                                start--
                                currentAngle = (xCoordinates[i] - xCoordinates[start]) / (i - start)

                                if ((currentAngle - angles[start]) * direction >= 0) {
                                    break
                                }
                            }
                        }
                    }

                    // Reset from last check
                    lastAngle = (xCoordinates[i] - xCoordinates[start]) / (i - start)

                    // Update all the points from start.
                    for (j in start until i) {
                        angles[j] = lastAngle
                        xCoordinates[j] = xCoordinates[start] + lastAngle * (j - start)
                    }

                    last = i
                }

                i++
            }
        }

        /**
         * @return The diameter of the normalized circle that fits inside of the square (size x size).
         */
        fun getNormalizedCircleSize(size: Int): Int {
            val area = size * size * MAX_SQUARE_AREA_FACTOR
            return round(sqrt((4 * area) / Math.PI)).toInt()
        }
    }
}