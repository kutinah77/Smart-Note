package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1f) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun cropWithTransform(
        bitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        density: Float,
        isCircle: Boolean
    ): Bitmap {
        val kPx = 200f * density
        val centerPx = kPx / 2f

        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val s0 = kPx / Math.min(w, h)
        val wDraw = w * s0
        val hDraw = h * s0
        val x0 = (kPx - wDraw) / 2f
        val y0 = (kPx - hDraw) / 2f

        // Un-transformed component coordinates
        val pTLx = (-offsetX - centerPx) / scale + centerPx
        val pTLy = (-offsetY - centerPx) / scale + centerPx
        val pBRx = (kPx - offsetX - centerPx) / scale + centerPx
        val pBRy = (kPx - offsetY - centerPx) / scale + centerPx

        // Source image pixels
        val leftPx = ((pTLx - x0) / s0).toInt().coerceIn(0, bitmap.width)
        val topPx = ((pTLy - y0) / s0).toInt().coerceIn(0, bitmap.height)
        val rightPx = ((pBRx - x0) / s0).toInt().coerceIn(0, bitmap.width)
        val bottomPx = ((pBRy - y0) / s0).toInt().coerceIn(0, bitmap.height)

        val cropW = (rightPx - leftPx).coerceAtLeast(10)
        val cropH = (bottomPx - topPx).coerceAtLeast(10)

        val actualCropW = Math.min(cropW, bitmap.width - leftPx)
        val actualCropH = Math.min(cropH, bitmap.height - topPx)

        // Create the square cropped bitmap
        val squareBitmap = Bitmap.createBitmap(bitmap, leftPx, topPx, actualCropW, actualCropH)

        if (!isCircle) {
            return squareBitmap
        }

        // Convert to circular bitmap
        val size = Math.min(squareBitmap.width, squareBitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = 0xff424242.toInt()
        }
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squareBitmap, rect, rect, paint)
        
        return output
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.filesDir, "business_logo.png")
            if (file.exists()) {
                file.delete()
            }
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
