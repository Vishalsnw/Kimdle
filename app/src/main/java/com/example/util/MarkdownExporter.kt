package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.data.model.Book
import com.example.data.model.Bookmark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HighlightColorOption(
    val id: String,
    val name: String,
    val color: Color,
    val label: String,
    val emoji: String
)

object HighlightColors {
    val Yellow = HighlightColorOption("Yellow", "Yellow", Color(0xFFFFD54F), "Key Point", "💡")
    val Green = HighlightColorOption("Green", "Green", Color(0xFF81C784), "Idea / Concept", "🌿")
    val Blue = HighlightColorOption("Blue", "Blue", Color(0xFF64B5F6), "Question / Fact", "❓")
    val Pink = HighlightColorOption("Pink", "Pink", Color(0xFFF06292), "Favorite Quote", "💖")
    val Purple = HighlightColorOption("Purple", "Purple", Color(0xFFBA68C8), "Action Item", "⚡")
    val Orange = HighlightColorOption("Orange", "Orange", Color(0xFFFFB74D), "Vocabulary", "📝")

    val ALL = listOf(Yellow, Green, Blue, Pink, Purple, Orange)

    fun getOption(name: String): HighlightColorOption {
        return ALL.find { it.name.equals(name, ignoreCase = true) } ?: Yellow
    }
}

object MarkdownExporter {

    fun generateMarkdown(book: Book, bookmarks: List<Bookmark>): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.US)
        val exportDate = dateFormat.format(Date())

        val sortedBookmarks = bookmarks.sortedWith(compareBy({ it.pageNumber }, { it.timestamp }))

        val sb = StringBuilder()
        sb.append("# 📚 Reading Notes & Highlights\n\n")
        sb.append("**Book Title:** ${book.title}\n")
        sb.append("**Author:** ${book.author}\n")
        sb.append("**Total Annotations:** ${bookmarks.size}\n")
        sb.append("**Exported On:** $exportDate\n\n")
        sb.append("---\n\n")

        if (sortedBookmarks.isEmpty()) {
            sb.append("*No highlights or notes recorded for this book yet.*\n")
            return sb.toString()
        }

        sb.append("## 🔖 Highlights & Study Notes\n\n")

        sortedBookmarks.forEachIndexed { index, bm ->
            val colorOpt = HighlightColors.getOption(bm.highlightColor)
            val dateStr = dateFormat.format(Date(bm.timestamp))

            sb.append("### ${index + 1}. Page ${bm.pageNumber + 1} ${colorOpt.emoji} [${colorOpt.name} - ${colorOpt.label}]\n\n")

            if (bm.selectedText.isNotBlank()) {
                sb.append("> \"${bm.selectedText.trim()}\"\n\n")
            }

            if (bm.note.isNotBlank()) {
                sb.append("**Note:** ${bm.note.trim()}\n\n")
            }

            sb.append("*Added on $dateStr*\n\n")
            sb.append("---\n\n")
        }

        sb.append("\n*Generated with Kindle PDF Reader*\n")
        return sb.toString()
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Reading Notes") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Markdown notes copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareMarkdown(context: Context, bookTitle: String, markdownText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Notes: $bookTitle")
            putExtra(Intent.EXTRA_TEXT, markdownText)
        }
        val chooser = Intent.createChooser(intent, "Export Notes as Markdown via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveMarkdownFile(context: Context, bookTitle: String, markdownText: String): File? {
        return try {
            val sanitizedTitle = bookTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${sanitizedTitle}_Notes.md"
            val dir = File(context.filesDir, "exported_notes").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(markdownText)
            Toast.makeText(context, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
