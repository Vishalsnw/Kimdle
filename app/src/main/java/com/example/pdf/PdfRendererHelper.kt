package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererHelper(private val filePath: String) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pdDocument: com.tom_roush.pdfbox.pdmodel.PDDocument? = null
    private var totalPagesCount: Int = 0

    // Memory cache for up to 12 rendered page bitmaps for ultra-smooth Kindle page turning
    private val pageCache = LruCache<Int, Bitmap>(12)
    // Cache for extracted text strings for instant reflow reading
    private val textCache = LruCache<Int, String>(50)

    suspend fun open(): Int = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext 0
            }
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor?.let { fd ->
                pdfRenderer = PdfRenderer(fd)
                totalPagesCount = pdfRenderer?.pageCount ?: 0
            }
            totalPagesCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun getTotalPages(): Int = totalPagesCount

    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPagesCount || width <= 0 || height <= 0) {
            return@withContext null
        }

        val cacheKey = pageIndex * 10000 + width // unique key based on index and resolution
        val cached = pageCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        try {
            synchronized(this@PdfRendererHelper) {
                val page = pdfRenderer?.openPage(pageIndex) ?: return@withContext null
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Ensure standard white background for PDFs
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                pageCache.put(cacheKey, bitmap)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun extractPageText(pageIndex: Int): String? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= totalPagesCount) return@withContext null
        val cached = textCache.get(pageIndex)
        if (cached != null) return@withContext cached

        try {
            synchronized(this@PdfRendererHelper) {
                if (pdDocument == null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        pdDocument = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file)
                    }
                }
                val doc = pdDocument ?: return@withContext null
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                val text = stripper.getText(doc)?.trim() ?: ""
                textCache.put(pageIndex, text)
                text
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            synchronized(this@PdfRendererHelper) {
                pageCache.evictAll()
                textCache.evictAll()
                pdfRenderer?.close()
                pdfRenderer = null
                pdDocument?.close()
                pdDocument = null
                fileDescriptor?.close()
                fileDescriptor = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        suspend fun renderThumbnail(filePath: String, destPath: String, width: Int = 400, height: Int = 600): String? = withContext(Dispatchers.IO) {
            var fd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext null

                fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(fd)
                if (renderer.pageCount <= 0) return@withContext null

                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val destFile = File(destPath)
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                destPath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try {
                    renderer?.close()
                    fd?.close()
                } catch (ignored: Exception) {}
            }
        }
    }
}
