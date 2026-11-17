package com.aarogya.sanjeevani.patient.profile

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream

private const val MAX_AVATAR_BYTES = 2 * 1024 * 1024 // Matches the doctor-avatars bucket's own file_size_limit.
private const val MAX_DIMENSION_PX = 1024
private const val INITIAL_JPEG_QUALITY = 90
private const val MIN_JPEG_QUALITY = 40
private const val QUALITY_STEP = 10

/** [rememberAvatarPickerLauncher] hands back a plain `() -> Unit` --
 *  the Android Photo Picker (no storage permission needed, API 21+ via
 *  the backport in `androidx.activity`) -- [onImagePicked] fires with
 *  whatever the user chose, or never if they backed out. */
@Composable
fun rememberAvatarPickerLauncher(onImagePicked: (Uri) -> Unit): () -> Unit {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onImagePicked(uri)
        }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

data class CompressedAvatar(
    val bytes: ByteArray,
    val contentType: String,
)

/**
 * Downscales to at most [MAX_DIMENSION_PX] on the long edge, then
 * step-compresses JPEG quality until the result fits under
 * [MAX_AVATAR_BYTES] (the bucket's own hard limit -- the upload would
 * just be rejected past it otherwise) or [MIN_JPEG_QUALITY] is reached,
 * whichever comes first. Always re-encodes to JPEG regardless of the
 * original format -- the one format this can reliably compress to a
 * byte budget via quality alone, and one of the bucket's allowed types.
 * `null` means the image still doesn't fit even at the lowest quality
 * this is willing to go (an extremely large/detailed source image) --
 * the caller shows a friendly "choose a smaller photo" message, nothing
 * silently half-uploads.
 */
fun compressAvatarImage(
    contentResolver: ContentResolver,
    uri: Uri,
): CompressedAvatar? {
    val original = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val scaled = original.downscaleToFit(MAX_DIMENSION_PX)
    if (scaled !== original) original.recycle()

    var quality = INITIAL_JPEG_QUALITY
    var bytes = scaled.toJpegBytes(quality)
    while (bytes.size > MAX_AVATAR_BYTES && quality > MIN_JPEG_QUALITY) {
        quality -= QUALITY_STEP
        bytes = scaled.toJpegBytes(quality)
    }
    scaled.recycle()

    return if (bytes.size <= MAX_AVATAR_BYTES) CompressedAvatar(bytes, "image/jpeg") else null
}

private fun Bitmap.downscaleToFit(maxDimension: Int): Bitmap {
    val longestEdge = maxOf(width, height)
    if (longestEdge <= maxDimension) return this
    val scale = maxDimension.toFloat() / longestEdge
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}

private fun Bitmap.toJpegBytes(quality: Int): ByteArray =
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
