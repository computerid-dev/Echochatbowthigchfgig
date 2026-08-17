package com.echochat.cid.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    private const val TARGET_SIZE = 160
    private const val JPEG_QUALITY = 70

    private const val CHAT_IMAGE_MAX_DIMENSION = 1080
    private const val CHAT_IMAGE_MAX_BASE64_CHARS = 650_000 // aman di bawah batas 1MB per dokumen Firestore
    private const val CHAT_IMAGE_MIN_QUALITY = 25

    /** Baca gambar dari Uri, perkecil ke ukuran avatar, lalu ubah jadi base64 JPEG. */
    fun uriToCompressedBase64(context: Context, uri: Uri): String? {
        val bitmap = loadBitmap(context, uri) ?: return null
        val scaled = scaleDown(bitmap, TARGET_SIZE)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Kirim gambar chat: dikompres lebih besar dari avatar (masih enak dilihat),
     * tapi kualitas JPEG diturunkan bertahap sampai ukuran base64-nya aman
     * di bawah batas 1 dokumen Firestore (1MB), supaya kirim pesan tidak gagal.
     */
    fun uriToChatImageBase64(context: Context, uri: Uri): String? {
        val bitmap = loadBitmap(context, uri) ?: return null
        val scaled = scaleDown(bitmap, CHAT_IMAGE_MAX_DIMENSION)

        var quality = 85
        var base64: String
        while (true) {
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            if (base64.length <= CHAT_IMAGE_MAX_BASE64_CHARS || quality <= CHAT_IMAGE_MIN_QUALITY) break
            quality -= 15
        }
        return if (base64.length <= CHAT_IMAGE_MAX_BASE64_CHARS) base64 else null
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val ratio = minOf(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )
        if (ratio >= 1f) return bitmap
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
