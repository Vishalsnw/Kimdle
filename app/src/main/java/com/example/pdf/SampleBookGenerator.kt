package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleBookGenerator {

    suspend fun generateAllSampleBooks(context: Context): List<File> = withContext(Dispatchers.IO) {
        val samplesDir = File(context.filesDir, "sample_books")
        if (!samplesDir.exists()) {
            samplesDir.mkdirs()
        }

        val book1 = File(samplesDir, "digital_reader_guide.pdf")
        val book2 = File(samplesDir, "sherlock_holmes_scandal.pdf")
        val book3 = File(samplesDir, "art_of_mindful_reading.pdf")

        val generated = mutableListOf<File>()

        if (!book1.exists()) {
            createReaderGuidePdf(book1)
            generated.add(book1)
        } else {
            generated.add(book1)
        }

        if (!book2.exists()) {
            createSherlockPdf(book2)
            generated.add(book2)
        } else {
            generated.add(book2)
        }

        if (!book3.exists()) {
            createMindfulReadingPdf(book3)
            generated.add(book3)
        } else {
            generated.add(book3)
        }

        generated
    }

    private fun createReaderGuidePdf(destFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842 // Standard A4 points

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1D1B18")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D97706")
            textSize = 16f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2D2A26")
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#8B5CF6")
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val chapters = listOf(
            "Welcome to the Digital PDF Reader" to listOf(
                "Thank you for choosing our digital PDF Reader! This application transforms ordinary PDF files into an immersive, comfortable book reading experience.",
                "With our custom rendering engine, every PDF page is processed with high-fidelity bitmap scaling and instant caching, ensuring smooth horizontal page flips just like turning pages in a physical paper book.",
                "Notice the generous margins and crisp typography on this page. You can customize the look of this page at any time using our Reading Themes: Light Mode, Sepia Mode, and Night Mode."
            ),
            "Chapter 1: Reading Themes & Eye Comfort" to listOf(
                "Long reading sessions require optimal eye comfort. That is why we built our signature Sepia Mode.",
                "When you switch to Sepia Mode, our engine applies a warm vintage cream background (#F4ECD8) and deep brown ink (#4A3B32) across the entire PDF document. This reduces blue light glare while maintaining perfect typographic contrast.",
                "For late-night reading in bed, switch to Night Mode. Our dark charcoal OLED theme turns glaring white page backgrounds into deep dark gray, keeping your eyes rested."
            ),
            "Chapter 2: Realistic Page Flip vs Vertical Scroll" to listOf(
                "Everyone has a different reading preference. Some readers prefer the nostalgic, rhythmic feel of flipping pages horizontally, while others prefer continuous vertical scrolling.",
                "You can toggle between Horizontal Page Flip and Vertical Continuous Scroll in the Reader Settings bottom sheet.",
                "In Horizontal mode, simply tap the left or right edge of the screen, or swipe horizontally across the page to glide effortlessly to the next page."
            ),
            "Chapter 3: Bookmarks, Progress & Smart Stats" to listOf(
                "Never lose your place again. As you read, your page progress is saved automatically into our local SQLite database.",
                "You can tap the Bookmark ribbon icon in the top toolbar to save important pages and add personal study notes.",
                "Check the bottom navigation bar to see your current page progress, reading percentage, and time left estimation calculated from your personal reading pace!"
            ),
            "Chapter 4: Importing Your Own Library" to listOf(
                "You are not limited to sample books! You can import any PDF file from your phone storage, Google Drive, WhatsApp downloads, or email attachments.",
                "Tap the '+ Import PDF' button on the My Library screen. Our app will automatically copy the document into secure local storage and generate a crisp book cover thumbnail.",
                "Enjoy reading your textbooks, novels, research papers, and documents in comfort!"
            )
        )

        var pageNum = 1
        for ((title, paragraphs) in chapters) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Draw decorative top border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#D97706")
                strokeWidth = 4f
            }
            canvas.drawLine(50f, 40f, pageWidth - 50f, 40f, borderPaint)

            var y = 100f
            canvas.drawText(title, 50f, y, if (pageNum == 1) titlePaint else headerPaint)
            y += 35f

            if (pageNum == 1) {
                canvas.drawText("A Comprehensive Guide to Digital Reading", 50f, y, subtitlePaint)
                y += 50f
            } else {
                y += 20f
            }

            for (para in paragraphs) {
                val words = para.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val measure = bodyPaint.measureText(testLine)
                    if (measure > (pageWidth - 100)) {
                        canvas.drawText(currentLine, 50f, y, bodyPaint)
                        y += 24f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 50f, y, bodyPaint)
                    y += 36f
                }
            }

            // Draw Footer
            canvas.drawText("Page $pageNum • Digital PDF Reader", 50f, pageHeight - 40f, footerPaint)
            document.finishPage(page)
            pageNum++
        }

        FileOutputStream(destFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun createSherlockPdf(destFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1D1B18")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val authorPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 15f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textPages = listOf(
            listOf(
                "To Sherlock Holmes she is always the woman. I have seldom heard him mention her under any other name. In his eyes she eclipses and predominates the whole of her sex.",
                "It was not that he felt any emotion akin to love for Irene Adler. All emotions, and that one particularly, were abhorrent to his cold, precise but admirably balanced mind.",
                "He was, I take it, the most perfect reasoning and observing machine that the world has seen, but as a lover he would have placed himself in a false position.",
                "He never spoke of the softer passions, save with a gibe and a sneer. They were admirable things for the observer—excellent for drawing the veil from men's motives and actions."
            ),
            listOf(
                "One night—it was on the twentieth of March, 1888—I was returning from a journey to a patient (for I had now returned to civil practice), when my way led me through Baker Street.",
                "As I passed the well-remembered door, which must always be associated in my mind with my wooing, and with the dark incidents of the Study in Scarlet, I was seized with a keen desire to see Holmes again, and to know how he was employing his extraordinary powers.",
                "His rooms were brilliantly lit, and, even as I looked up, I saw his tall, spare figure pass twice in a dark silhouette against the blind. He was pacing the room swiftly, eagerly, with his head sunk upon his chest and his hands clasped behind him."
            ),
            listOf(
                "I rang the bell and was shown up to the chamber which had formerly been in part my own. His manner was not effusive. It seldom was; but he was glad, I think, to see me.",
                "With hardly a word spoken, but with a kindly eye, he waved me to an armchair, threw across his case of cigars, and indicated a spirit case and a gasogen in the corner.",
                "Then he stood before the fire and looked me over in his singular introspective fashion. 'Wedlock suits you,' he remarked. 'I think, Watson, that you have put on seven and a half pounds since I saw you.'"
            ),
            listOf(
                "'Seven!' I answered. 'Indeed, I should have thought a little more. Just a trifle more, I fancy, Watson. And in practice again, I observe. You did not tell me that you intended to go into harness.'",
                "'Then, how do you know?' 'I see it, I deduce it. How do I know that you have been getting yourself very wet lately, and that you have a most clumsy and careless servant girl?'",
                "'My dear Holmes,' said I, 'this is too much. You would certainly have been burned, had you lived a few centuries ago. It is true that I had a country walk on Thursday and came home in a dreadful mess, but as I have changed my clothes I can't imagine how you deduce it.'"
            )
        )

        for (i in textPages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = 80f
            if (i == 0) {
                canvas.drawText("A Scandal in Bohemia", 50f, y, titlePaint)
                y += 30f
                canvas.drawText("by Arthur Conan Doyle", 50f, y, authorPaint)
                y += 50f
            } else {
                val headerPaint = Paint().apply {
                    color = Color.parseColor("#999999")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                canvas.drawText("A Scandal in Bohemia", 50f, 40f, headerPaint)
                y = 80f
            }

            for (para in textPages[i]) {
                val words = para.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val measure = bodyPaint.measureText(testLine)
                    if (measure > (pageWidth - 100)) {
                        canvas.drawText(currentLine, 50f, y, bodyPaint)
                        y += 24f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 50f, y, bodyPaint)
                    y += 36f
                }
            }

            canvas.drawText("Page ${i + 1} of ${textPages.size} • Classic Collection", 50f, pageHeight - 40f, footerPaint)
            document.finishPage(page)
        }

        FileOutputStream(destFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun createMindfulReadingPdf(destFile: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1D1B18")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2D2A26")
            textSize = 14f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 11f
        }

        val textPages = listOf(
            listOf(
                "In an era dominated by endless notification pings, short-form video feeds, and fragmented attention spans, the simple act of sitting down with a book is an act of deep mental restoration.",
                "When we read deeply, our brain enters a state of cognitive immersion known as deep work or flow. This state calms the nervous system while activating empathy and imaginative visual pathways.",
                "Digital reading does not have to be distracting. By designing an electronic reading environment that mimics the serene simplicity of physical paper—with warm sepia tones, clean page boundaries, and intuitive touch interaction—we reclaim our focus."
            ),
            listOf(
                "How to Cultivate a Daily Reading Ritual:",
                "1. Establish a Sacred Time: Set aside 20 minutes before bedtime or during morning tea.",
                "2. Use Warm Lighting: Switch your reader to Sepia Mode during dusk or Night Mode in total darkness to protect your circadian rhythm.",
                "3. Annotate & Bookmark: Active reading is a dialogue between the author and your own thoughts. Use bookmarks to mark resonant passages."
            )
        )

        for (i in textPages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = 80f
            if (i == 0) {
                canvas.drawText("The Art of Mindful Reading", 50f, y, titlePaint)
                y += 50f
            }

            for (para in textPages[i]) {
                val words = para.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val measure = bodyPaint.measureText(testLine)
                    if (measure > (pageWidth - 100)) {
                        canvas.drawText(currentLine, 50f, y, bodyPaint)
                        y += 24f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 50f, y, bodyPaint)
                    y += 36f
                }
            }

            canvas.drawText("Page ${i + 1} • Mindful Reading Essay", 50f, pageHeight - 40f, footerPaint)
            document.finishPage(page)
        }

        FileOutputStream(destFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }
}
