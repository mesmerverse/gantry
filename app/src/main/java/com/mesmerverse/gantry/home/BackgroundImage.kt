package com.mesmerverse.gantry.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Longest edge any decoded background may reach. */
private const val MAX_EDGE = 4096

/**
 * Pixel budget for a decoded background (~64 MB as ARGB_8888): under the
 * platform's 100 MB draw limit, so a huge or oddly shaped image can't take
 * the launcher down at every start.
 */
private const val MAX_PIXELS = 16_000_000L

/** Largest picked file copied in before decoding; anything bigger is refused. */
internal const val MAX_BACKGROUND_BYTES = 50L * 1024 * 1024

/**
 * Decodes [file] downsampled toward [targetWidth], never past [MAX_EDGE] on
 * either side or [MAX_PIXELS] in total. Null — never a throw — for a
 * missing, corrupt, or undecodable file, including out-of-memory.
 */
internal fun decodeBoundedBitmap(file: File, targetWidth: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) {
        null
    } else {
        var sample = 1
        while (w / (sample * 2) >= targetWidth) sample *= 2
        while (maxOf(w, h) / sample > MAX_EDGE ||
            (w.toLong() / sample) * (h.toLong() / sample) > MAX_PIXELS
        ) {
            sample *= 2
        }
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
} catch (_: Throwable) {
    null
}

/** Copies at most [MAX_BACKGROUND_BYTES]; a larger stream throws. */
internal fun InputStream.copyCapped(out: OutputStream) {
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > MAX_BACKGROUND_BYTES) throw IOException("background image too large")
        out.write(buffer, 0, read)
    }
}

/** Largest decode kept at import: the full budget, re-encoded as the source. */
internal const val IMPORT_TARGET_WIDTH = MAX_EDGE
