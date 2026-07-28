package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyReadingStat
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.ProgressGreen
import com.example.ui.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsHabitSheet(
    viewModel: LibraryViewModel,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()

    // Calculate totals
    val totalPagesRead = remember(dailyStats, books) {
        val fromStats = dailyStats.sumOf { it.pagesRead }
        val fromBooks = books.sumOf { it.currentPage }
        fromStats.coerceAtLeast(fromBooks)
    }
    val totalMinutesRead = remember(dailyStats) { dailyStats.sumOf { it.readingTimeMinutes } }
    val totalWordsRead = remember(dailyStats) { dailyStats.sumOf { it.wordsRead } }
    val rsvpUsed = remember(dailyStats) { dailyStats.any { it.rsvpUsed } }
    val nightRead = remember(dailyStats) { dailyStats.any { it.nightRead } }
    val completedBooksCount = remember(books) { books.count { it.currentPage + 1 >= it.totalPages && it.totalPages > 1 } }

    // Calculate streak
    val streakDays = remember(dailyStats) {
        computeStreak(dailyStats)
    }

    // Today's stat
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }
    val todayStat = remember(dailyStats, todayStr) {
        dailyStats.find { it.dateString == todayStr } ?: DailyReadingStat(dateString = todayStr)
    }

    // Badges list
    val badges = remember(totalPagesRead, totalMinutesRead, streakDays, books.size, rsvpUsed, nightRead) {
        listOf(
            BadgeItem(
                id = "first_step",
                title = "First Step",
                description = "Turn your first 5 pages",
                icon = Icons.Default.MenuBook,
                isUnlocked = totalPagesRead >= 5 || streakDays >= 1
            ),
            BadgeItem(
                id = "habit_starter",
                title = "Habit Starter",
                description = "Reach a 1-day reading streak",
                icon = Icons.Default.LocalFireDepartment,
                isUnlocked = streakDays >= 1 || totalMinutesRead >= 5
            ),
            BadgeItem(
                id = "speed_demon",
                title = "Speed Demon",
                description = "Use RSVP Speed Reading mode",
                icon = Icons.Default.Speed,
                isUnlocked = rsvpUsed || totalWordsRead > 500
            ),
            BadgeItem(
                id = "library_collector",
                title = "Collector",
                description = "Have 3+ books in your library",
                icon = Icons.Default.AutoStories,
                isUnlocked = books.size >= 3
            ),
            BadgeItem(
                id = "bookworm",
                title = "Bookworm",
                description = "Spend 30+ mins reading total",
                icon = Icons.Default.AccessTime,
                isUnlocked = totalMinutesRead >= 30
            ),
            BadgeItem(
                id = "night_owl",
                title = "Night Owl",
                description = "Read late at night (after 10 PM)",
                icon = Icons.Default.Timeline,
                isUnlocked = nightRead
            ),
            BadgeItem(
                id = "century_club",
                title = "Century Club",
                description = "Turn 100 pages across all books",
                icon = Icons.Default.EmojiEvents,
                isUnlocked = totalPagesRead >= 100
            ),
            BadgeItem(
                id = "marathoner",
                title = "Marathoner",
                description = "Reach a 7-day reading streak",
                icon = Icons.Default.Assessment,
                isUnlocked = streakDays >= 7
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Stats & Habits",
                        tint = AmberPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reading Stats & Habit Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Habit & Streak Card
            Surface(
                color = AmberPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AmberPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak Flame",
                                    tint = if (streakDays > 0) Color(0xFFFF5722) else Color.Gray,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$streakDays Day Streak!",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (streakDays > 0) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (streakDays > 0) "You're building a daily reading habit. Keep going!" else "Read today to start your daily reading streak!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7-Day Habit Heatmap Bubbles
                    Text(
                        text = "THIS WEEK'S ACTIVITY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val last7Days = remember(todayStr) { getLast7DaysDates() }
                        val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                        
                        last7Days.forEachIndexed { idx, dateStr ->
                            val stat = dailyStats.find { it.dateString == dateStr }
                            val isToday = dateStr == todayStr
                            val hasActivity = stat != null && (stat.readingTimeMinutes > 0 || stat.pagesRead > 0 || stat.wordsRead > 0)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasActivity -> ProgressGreen
                                                isToday -> AmberPrimary.copy(alpha = 0.3f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            }
                                        )
                                ) {
                                    if (hasActivity) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Read",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (isToday) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Today",
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNames.getOrElse(idx % 7) { "-" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) AmberPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Today's Goal Progress Bar
                    val dailyGoalMins = 15
                    val todayMins = todayStat.readingTimeMinutes
                    val goalProgress = (todayMins.toFloat() / dailyGoalMins).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's Goal ($dailyGoalMins mins)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${todayMins}/${dailyGoalMins} mins",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (todayMins >= dailyGoalMins) ProgressGreen else AmberPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { goalProgress },
                        color = if (todayMins >= dailyGoalMins) ProgressGreen else AmberPrimary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Log Habit Demo Button
                    Button(
                        onClick = {
                            viewModel.simulateHabitActivity(pages = 8, minutes = 15, words = 3200)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Log Today's Reading Habit (+15m, +8 pages)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Grid
            Text(
                text = "OVERALL STATISTICS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Books Read",
                    value = "${completedBooksCount} / ${books.size}",
                    icon = Icons.Default.AutoStories,
                    color = AmberPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pages Read",
                    value = "$totalPagesRead",
                    icon = Icons.Default.MenuBook,
                    color = ProgressGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val hours = totalMinutesRead / 60
                val mins = totalMinutesRead % 60
                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                StatCard(
                    title = "Total Time",
                    value = timeStr,
                    icon = Icons.Default.AccessTime,
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "RSVP Words",
                    value = "$totalWordsRead",
                    icon = Icons.Default.Speed,
                    color = Color(0xFF8E24AA),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gamified Achievements Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GAMIFIED ACHIEVEMENTS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                val unlockedCount = badges.count { it.isUnlocked }
                Text(
                    text = "$unlockedCount / ${badges.size} Unlocked",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AmberPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.forEach { badge ->
                    BadgeCard(badge = badge)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun BadgeCard(badge: BadgeItem) {
    val bgColor = if (badge.isUnlocked) AmberPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (badge.isUnlocked) AmberPrimary else Color.Transparent
    val iconColor = if (badge.isUnlocked) AmberPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = if (badge.isUnlocked) BorderStroke(1.5.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (badge.isUnlocked) badge.icon else Icons.Default.Lock,
                    contentDescription = badge.title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    if (badge.isUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = ProgressGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (badge.isUnlocked) 0.8f else 0.5f)
                )
            }
        }
    }
}

fun computeStreak(stats: List<DailyReadingStat>): Int {
    if (stats.isEmpty()) return 0
    val activeDates = stats.filter { it.readingTimeMinutes > 0 || it.pagesRead > 0 || it.wordsRead > 0 }
        .map { it.dateString }
        .toSet()
    
    if (activeDates.isEmpty()) return 0

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    var streak = 0

    // Check today first, if not active, check yesterday
    var checkDateStr = dateFormat.format(calendar.time)
    if (!activeDates.contains(checkDateStr)) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        checkDateStr = dateFormat.format(calendar.time)
        if (!activeDates.contains(checkDateStr)) {
            return 0
        }
    }

    // Count backwards consecutive days
    while (activeDates.contains(checkDateStr)) {
        streak++
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        checkDateStr = dateFormat.format(calendar.time)
    }
    return streak
}

private fun getLast7DaysDates(): List<String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    // Start from Monday of this week or 6 days ago
    val dates = mutableListOf<String>()
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    for (i in 0..6) {
        dates.add(dateFormat.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return dates
}
