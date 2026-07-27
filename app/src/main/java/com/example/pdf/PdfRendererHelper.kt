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

data class ReflowPageContent(
    val text: String,
    val images: List<Bitmap>
)

class PdfRendererHelper(private val filePath: String) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pdDocument: com.tom_roush.pdfbox.pdmodel.PDDocument? = null
    private var totalPagesCount: Int = 0

    // Memory cache for up to 12 rendered page bitmaps for ultra-smooth page turning
    private val pageCache = LruCache<Int, Bitmap>(12)
    // Cache for extracted text and images for instant reflow reading
    private val reflowCache = LruCache<Int, ReflowPageContent>(20)

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

    suspend fun extractReflowContent(pageIndex: Int): ReflowPageContent? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= totalPagesCount) return@withContext null
        val cached = reflowCache.get(pageIndex)
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
                val page = doc.getPage(pageIndex)

                // Extract images from page resources including Form XObjects recursively
                val images = mutableListOf<Bitmap>()
                extractImagesRecursive(page.resources, images)

                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
                val text = stripper.getText(doc)?.trim() ?: ""

                val content = ReflowPageContent(text = text, images = images)
                reflowCache.put(pageIndex, content)
                content
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractImagesRecursive(
        resources: com.tom_roush.pdfbox.pdmodel.PDResources?,
        images: MutableList<Bitmap>,
        visited: MutableSet<String> = mutableSetOf(),
        depth: Int = 0
    ) {
        if (resources == null || depth > 10) return
        try {
            for (name in resources.xObjectNames) {
                try {
                    val xobject = resources.getXObject(name)
                    val objKey = xobject?.cosObject?.hashCode()?.toString() ?: name.name
                    if (!visited.add(objKey)) continue

                    if (xobject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        try {
                            val bmp = xobject.image
                            if (bmp != null && bmp.width > 10 && bmp.height > 10) {
                                images.add(bmp)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (xobject is com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject) {
                        extractImagesRecursive(xobject.resources, images, visited, depth + 1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun extractPageText(pageIndex: Int): String? {
        return extractReflowContent(pageIndex)?.text
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            synchronized(this@PdfRendererHelper) {
                pageCache.evictAll()
                reflowCache.evictAll()
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
