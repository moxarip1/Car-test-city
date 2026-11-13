package com.example.game.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ProceduralTextures(
    val asphaltTexId: Int,
    val skyscraperTexId: Int,
    val waterTexId: Int,
    val curbTexId: Int
) {
    fun release() {
        GLES30.glDeleteTextures(
            4,
            intArrayOf(asphaltTexId, skyscraperTexId, waterTexId, curbTexId),
            0
        )
    }

    companion object {
        fun createAll(): ProceduralTextures {
            return ProceduralTextures(
                asphaltTexId = uploadTexture(generateAsphaltBitmap()),
                skyscraperTexId = uploadTexture(generateSkyscraperBitmap()),
                waterTexId = uploadTexture(generateWaterBitmap()),
                curbTexId = uploadTexture(generateCurbBitmap())
            )
        }

        private fun uploadTexture(bitmap: Bitmap): Int {
            val ids = IntArray(1)
            GLES30.glGenTextures(1, ids, 0)
            val texId = ids[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR_MIPMAP_LINEAR
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            bitmap.recycle()
            return texId
        }

        /**
         * Fast 256x256 procedural dark asphalt road texture using direct pixel buffer (< 2ms).
         */
        private fun generateAsphaltBitmap(): Bitmap {
            val size = 256
            val pixels = IntArray(size * size)
            val edgeCol = Color.rgb(235, 240, 248)
            val goldCol = Color.rgb(245, 192, 66)

            for (y in 0 until size) {
                val dashOn = (y % 48) < 22
                val rowOff = y * size
                for (x in 0 until size) {
                    val col = when {
                        x in 7..11 || x in (size - 12)..(size - 8) -> edgeCol
                        x in 123..125 || x in 130..132 -> goldCol
                        dashOn && (x in 66..68 || x in 187..189) -> edgeCol
                        else -> {
                            // Fast deterministic integer hash for asphalt aggregate grain
                            var h = x * 374761393 + y * 668265263
                            h = (h xor (h ushr 13)) * 1274126177
                            val grain = ((h ushr 24) and 0x1F)
                            val wear = if (x in 48..78 || x in 178..208) -5 else 0
                            val v = (24 + grain + wear).coerceIn(16, 62)
                            Color.rgb(v, v + 2, v + 5)
                        }
                    }
                    pixels[rowOff + x] = col
                }
            }

            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, size, 0, 0, size, size)
            return bmp
        }

        /**
         * Fast 256x256 glass skyscraper facade with mullions and warm gold / cyan interior lit windows.
         */
        private fun generateSkyscraperBitmap(): Bitmap {
            val size = 256
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.rgb(22, 27, 38))

            val cols = 8
            val rows = 12
            val cellW = size.toFloat() / cols
            val cellH = size.toFloat() / rows
            val rng = Random(1337)
            val panePaint = Paint()

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val left = c * cellW + 2.5f
                    val top = r * cellH + 2.5f
                    val right = (c + 1) * cellW - 2.5f
                    val bottom = (r + 1) * cellH - 2.5f

                    val roll = rng.nextFloat()
                    panePaint.color = when {
                        roll > 0.84f -> Color.rgb(0, 215, 245)
                        roll > 0.62f -> Color.rgb(255, 195, 105)
                        else -> Color.rgb(38, 62, 92)
                    }
                    canvas.drawRect(left, top, right, bottom, panePaint)

                    if (roll <= 0.62f) {
                        panePaint.color = Color.argb(42, 255, 210, 175)
                        canvas.drawRect(left + 1.5f, top + 1.5f, left + 6f, bottom - 2f, panePaint)
                    }
                }
            }
            return bmp
        }

        /**
         * Fast 128x128 tileable shimmering turquoise coastal wave & caustic texture (< 1ms).
         */
        private fun generateWaterBitmap(): Bitmap {
            val size = 128
            val pixels = IntArray(size * size)
            val sinLut = DoubleArray(size) { i -> sin((i.toDouble() / size) * 2.0 * PI) }
            val cosLut = DoubleArray(size) { i -> cos((i.toDouble() / size) * 2.0 * PI) }
            val mask = size - 1

            for (y in 0 until size) {
                val rowOff = y * size
                val c2y = cosLut[(y * 2) and mask]
                val s4y = sinLut[(y * 4) and mask]
                for (x in 0 until size) {
                    val w1 = sinLut[((x * 3) + (c2y * 18.0).toInt()) and mask]
                    val w2 = cosLut[((y * 4) + (sinLut[(x * 3) and mask] * 16.0).toInt()) and mask]
                    val caustic = ((w1 + w2 + s4y * 0.2) * 0.23 + 0.5).coerceIn(0.0, 1.0)
                    val crest = caustic * caustic
                    val r = (12 + crest * 75).toInt().coerceIn(0, 255)
                    val g = (145 + crest * 95).toInt().coerceIn(0, 255)
                    val b = (165 + crest * 80).toInt().coerceIn(0, 255)
                    pixels[rowOff + x] = Color.rgb(r, g, b)
                }
            }
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, size, 0, 0, size, size)
            return bmp
        }

        /**
         * Fast 128x128 red/white alternating racing curb texture.
         */
        private fun generateCurbBitmap(): Bitmap {
            val size = 128
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val red = Paint().apply { color = Color.rgb(218, 36, 48) }
            val white = Paint().apply { color = Color.rgb(240, 244, 248) }
            for (i in 0 until 4) {
                val y0 = i * 32f
                canvas.drawRect(0f, y0, 128f, y0 + 16f, red)
                canvas.drawRect(0f, y0 + 16f, 128f, y0 + 32f, white)
            }
            return bmp
        }
    }
}
