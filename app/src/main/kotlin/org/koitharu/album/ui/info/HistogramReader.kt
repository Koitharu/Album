package org.koitharu.album.ui.info

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream

class HistogramReader {

    suspend fun decodeHistogram(inputStream: InputStream, tileHeight: Int = 512): ImageHistogram {
        val red = IntArray(HISTOGRAM_SIZE)
        val green = IntArray(HISTOGRAM_SIZE)
        val blue = IntArray(HISTOGRAM_SIZE)
        val luminance = IntArray(HISTOGRAM_SIZE)

        val decoder = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(inputStream)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(inputStream, false)
            }
        } ?: error("Unable to instantiate decoder")

        val imgWidth = decoder.width
        val imgHeight = decoder.height

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        var reusableBitmap: Bitmap? = null
        val rect = Rect(0, 0, imgWidth, 0)

        try {
            var y = 0
            while (y < imgHeight) {
                currentCoroutineContext().ensureActive()
                val currentTileHeight = minOf(tileHeight, imgHeight - y)
                rect.set(0, y, imgWidth, y + currentTileHeight)

                options.inBitmap = reusableBitmap

                val tileBitmap = withContext(Dispatchers.IO) {
                    decoder.decodeRegion(rect, options)
                }
                reusableBitmap = tileBitmap

                val totalPixels = tileBitmap.width * tileBitmap.height
                val pixelBuffer = IntArray(totalPixels)
                tileBitmap.getPixels(
                    pixelBuffer,
                    0,
                    tileBitmap.width,
                    0,
                    0,
                    tileBitmap.width,
                    tileBitmap.height
                )

                for (i in 0 until totalPixels) {
                    val color = pixelBuffer[i]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val l = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

                    red[r]++
                    green[g]++
                    blue[b]++
                    luminance[l]++
                }

                y += tileHeight
            }
            val rgbMax = maxOf(
                red.max(),
                green.max(),
                blue.max(),
            )
            return ImageHistogram(
                red = HistogramData(red, rgbMax),
                green = HistogramData(green, rgbMax),
                blue = HistogramData(blue, rgbMax),
                luminance = HistogramData(luminance, luminance.max()),
            )
        } finally {
            reusableBitmap?.recycle()
            decoder.recycle()
        }
    }
}