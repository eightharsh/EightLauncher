package com.eightharsh.eightharsh.core.domain.personal

import kotlinx.coroutines.flow.Flow

data class QuickNote(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val dueAt: Long? = null,
    val notified: Boolean = false,
    val pinned: Boolean = false
)

data class FocusSession(val packages: Set<String>, val endsAt: Long)

interface PersonalRepository {
    val focusSession: Flow<FocusSession?>
    suspend fun startFocus(packages: Set<String>, minutes: Int)
    suspend fun stopFocus()
    suspend fun clearCompletedNotes(): List<QuickNote>
    suspend fun restoreClearedNotes(notes: List<QuickNote>)
    suspend fun setNotePinned(id: String, pinned: Boolean)

    val notes: Flow<List<QuickNote>>
    val showNotes: Flow<Boolean>
    val shuffleOnUnlock: Flow<Boolean>
    val shuffledPackages: Flow<List<String>>
    suspend fun setShowNotes(value: Boolean)
    suspend fun setShuffleOnUnlock(value: Boolean)
    suspend fun saveNote(note: QuickNote)
    suspend fun deleteNote(id: String)
    suspend fun markNotified(id: String, dueAt: Long)
    suspend fun shuffleFavorites()
    suspend fun refreshReminders()
    suspend fun exportBackup(): String
    suspend fun importBackup(json: String)
}

/** Preserve every favorite, while ensuring an unlock changes the previous visible order. */
fun <T> shuffledDifferently(items: List<T>, random: kotlin.random.Random = kotlin.random.Random.Default): List<T> {
    if (items.size < 2) return items.toList()
    val shuffled = items.shuffled(random)
    return if (shuffled == items) items.drop(1) + items.first() else shuffled
}
