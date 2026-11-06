package com.eightharsh.eightharsh.core.data.personal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.work.*
import com.eightharsh.eightharsh.core.data.database.ModifiedAppsDao
import com.eightharsh.eightharsh.core.data.entity.ModifiedAppEntity
import com.eightharsh.eightharsh.core.domain.personal.*
import com.google.gson.*
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.TimeUnit

@Singleton
class PersonalRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
    private val apps: ModifiedAppsDao,
    @param:ApplicationContext private val context: Context
) : PersonalRepository {
    private val notesKey = stringPreferencesKey("quick_notes")
    private val showKey = booleanPreferencesKey("show_quick_notes")
    private val shuffleKey = booleanPreferencesKey("shuffle_on_unlock")
    private val orderKey = stringPreferencesKey("shuffled_favorites")
    private val focusPackages = stringSetPreferencesKey("focus_packages")
    private val focusEnd = longPreferencesKey("focus_end")

    @OptIn(ExperimentalCoroutinesApi::class)
    override val focusSession = store.data.map { preferences ->
        FocusSession(preferences[focusPackages].orEmpty(), preferences[focusEnd] ?: 0L)
    }.distinctUntilChanged().flatMapLatest { session ->
        flow {
            val remaining = session.endsAt - System.currentTimeMillis()
            if (remaining > 0 && session.packages.isNotEmpty()) {
                emit(session)
                delay(remaining)
            }
            emit(null)
        }
    }

    override suspend fun startFocus(packages: Set<String>, minutes: Int) {
        require(packages.isNotEmpty() && minutes in 1..180)
        store.edit {
            it[focusPackages] = packages
            it[focusEnd] = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        }
    }

    override suspend fun stopFocus() {
        store.edit { it.remove(focusEnd) }
    }

    override suspend fun setNotePinned(id: String, pinned: Boolean) {
        store.edit { preferences ->
            val notes = decodeNotes(preferences[notesKey] ?: "[]")
            require(!pinned || notes.count { it.pinned && it.id != id } < 3) { "You can pin up to three notes." }
            preferences[notesKey] = encodeNotes(notes.map { if (it.id == id) it.copy(pinned = pinned) else it })
        }
    }

    override suspend fun clearCompletedNotes(): List<QuickNote> {
        var removed = emptyList<QuickNote>()
        store.edit { preferences ->
            val notes = decodeNotes(preferences[notesKey] ?: "[]")
            removed = notes.filter { it.done }
            preferences[notesKey] = encodeNotes(notes.filterNot { it.done })
        }
        removed.forEach { note ->
            WorkManager.getInstance(context).cancelUniqueWork("note-${note.id}")
            androidx.core.app.NotificationManagerCompat.from(context).cancel(note.id, 1)
        }
        return removed
    }

    override suspend fun restoreClearedNotes(notes: List<QuickNote>) {
        store.edit { preferences ->
            val current = decodeNotes(preferences[notesKey] ?: "[]")
            val ids = current.mapTo(mutableSetOf()) { it.id }
            var pinsRemaining = (3 - current.count { it.pinned }).coerceAtLeast(0)
            val restored = notes.filterNot { it.id in ids }.map { note ->
                if (!note.pinned) note
                else if (pinsRemaining > 0) {
                    pinsRemaining--
                    note
                } else note.copy(pinned = false)
            }
            require(current.size + restored.size <= 1000) { "Too many notes" }
            preferences[notesKey] = encodeNotes(current + restored)
        }
    }

    override val notes = store.data.map { decodeNotes(it[notesKey] ?: "[]") }.distinctUntilChanged()
    override val showNotes = store.data.map { it[showKey] ?: false }.distinctUntilChanged()
    override val shuffleOnUnlock = store.data.map { it[shuffleKey] ?: false }.distinctUntilChanged()
    override val shuffledPackages = store.data.map { p ->
        JsonParser.parseString(p[orderKey] ?: "[]").asJsonArray.map { it.asString }
    }.distinctUntilChanged()
    override suspend fun setShowNotes(value: Boolean) { store.edit { it[showKey] = value } }
    override suspend fun setShuffleOnUnlock(value: Boolean) {
        store.edit { it[shuffleKey] = value }
        if (value) shuffleFavorites()
    }
    override suspend fun saveNote(note: QuickNote) {
        require(note.text.isNotBlank() && note.text.length <= 2000)
        require(note.id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        var saved = note
        var dismissNotification = note.done || note.dueAt == null
        store.edit { preferences ->
            val notes = decodeNotes(preferences[notesKey] ?: "[]")
            val previous = notes.firstOrNull { it.id == note.id }
            require(notes.size < 1000 || previous != null) { "Too many notes" }
            if (note.pinned && notes.count { it.pinned && it.id != note.id } >= 3) {
                saved = note.copy(pinned = false)
            }
            dismissNotification = dismissNotification || previous?.dueAt != saved.dueAt
            preferences[notesKey] = encodeNotes(notes.filterNot { it.id == saved.id } + saved)
        }
        if (dismissNotification) {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(saved.id, 1)
        }
        schedule(saved)
    }
    override suspend fun deleteNote(id: String) {
        store.edit { p -> p[notesKey] = encodeNotes(decodeNotes(p[notesKey] ?: "[]").filterNot { it.id == id }) }
        WorkManager.getInstance(context).cancelUniqueWork("note-$id")
        androidx.core.app.NotificationManagerCompat.from(context).cancel(id, 1)
    }
    suspend fun actOnReminder(id: String, dueAt: Long, snooze: Boolean) {
        var updated: QuickNote? = null
        store.edit { preferences ->
            val notes = decodeNotes(preferences[notesKey] ?: "[]")
            val note = notes.firstOrNull { it.id == id && it.dueAt == dueAt && !it.done }
                ?: return@edit
            val replacement = if (snooze) {
                note.copy(dueAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10), notified = false)
            } else {
                note.copy(done = true)
            }
            updated = replacement
            preferences[notesKey] = encodeNotes(notes.map { if (it.id == id) replacement else it })
        }
        updated?.let {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(id, 1)
            schedule(it)
        }
    }

    override suspend fun markNotified(id: String, dueAt: Long) {
        store.edit { p -> p[notesKey] = encodeNotes(decodeNotes(p[notesKey] ?: "[]").map {
            if (it.id == id && it.dueAt == dueAt) it.copy(notified = true) else it
        }) }
    }
    private fun schedule(note: QuickNote, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val manager = WorkManager.getInstance(context)
        val due = note.dueAt
        if (note.done || due == null || note.notified) {
            manager.cancelUniqueWork("note-${note.id}")
            return
        }
        val work = OneTimeWorkRequestBuilder<NoteReminderWorker>()
            .setInputData(workDataOf("noteId" to note.id, "dueAt" to due))
            .setInitialDelay((due - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniqueWork("note-${note.id}", policy, work)
    }
    override suspend fun refreshReminders() {
        notes.first().filter { !it.done && it.dueAt != null && !it.notified }.forEach {
            schedule(it, ExistingWorkPolicy.KEEP)
        }
    }
    override suspend fun shuffleFavorites() {
        if (!shuffleOnUnlock.first()) return
        val favorites = apps.getFavouriteAppsInOrder().map { it.packageId }
        val saved = shuffledPackages.first()
        val previous = saved.filter { it in favorites } + favorites.filterNot { it in saved }
        store.edit { it[orderKey] = Gson().toJson(shuffledDifferently(previous)) }
    }

    // A versioned, typed backup. Never export active usage sessions or device-specific widget IDs.
    private val schema = buildMap<String, String> {
        listOf("haptic_feedback", "hidden_private_space", "twelve_hour_clock", "show_clock", "big_clock", "show_date",
            "show_status_bar", "show_screen_time_home", "show_screen_time_app", "show_wallpaper", "black_background",
            "show_search_box", "search_auto_open", "automatically_open_apps_in_search", "double_tap_to_lock",
            "hide_screen_time_page", "show_hidden_apps_in_search", "show_quick_notes", "shuffle_on_unlock").forEach { put(it, "boolean") }
        listOf("home_v_alignment", "home_alignment", "apps_alignment", "font").forEach { put(it, "string") }
        listOf("theme", "night_mode").forEach { put(it, "int") }
        listOf("widget_offset", "widget_height", "widget_width").forEach { put(it, "float") }
    }
    override suspend fun exportBackup(): String {
        val settings = JsonObject()
        store.data.first().asMap().forEach { (key, value) ->
            if (key.name in schema) settings.add(key.name, Gson().toJsonTree(value))
        }
        return JsonObject().apply {
            addProperty("format", "eightharsh-backup")
            addProperty("version", 1)
            add("settings", settings)
            add("notes", JsonParser.parseString(encodeNotes(notes.first())))
            add("apps", JsonArray().apply {
                apps.getAllForBackup().forEach { app -> add(JsonObject().apply {
                    addProperty("packageId", app.packageId)
                    app.displayName?.let { addProperty("displayName", it) }
                    addProperty("isHidden", app.isHidden); addProperty("isChallenge", app.isChallenge)
                    app.favouritePosition?.let { addProperty("favouritePosition", it) }
                }) }
            })
        }.toString()
    }
    override suspend fun importBackup(json: String) {
        require(json.length <= 1_000_000) { "Backup is too large" }
        val root = JsonParser.parseString(json).asJsonObject
        require(root["format"]?.asString == "eightharsh-backup" && root["version"]?.asInt == 1) { "Unsupported backup format" }
        val restoredNotes = decodeNotes(root["notes"].toString())
        val settings = root["settings"].asJsonObject
        val validatedSettings = settings.entrySet().associate { (key, value) ->
            val type = requireNotNull(schema[key]) { "Unknown setting: $key" }
            val primitive = value.asJsonPrimitive
            val parsed: Any = when (type) {
                "boolean" -> { require(primitive.isBoolean); value.asBoolean }
                "string" -> { require(primitive.isString && value.asString.length <= 200); value.asString }
                "int" -> { require(primitive.isNumber && value.asDouble == value.asInt.toDouble()); value.asInt }
                else -> { require(primitive.isNumber && value.asFloat.isFinite()); value.asFloat }
            }
            when (key) {
                "theme" -> require((parsed as Int) in setOf(0, 3, 5, 7, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19))
                "night_mode" -> require((parsed as Int) in 0..2)
                "home_alignment", "apps_alignment" -> require((parsed as String) in setOf("Left", "Center", "Right"))
                "home_v_alignment" -> require((parsed as String) in setOf("Top", "Center", "Bottom"))
                "widget_height", "widget_width" -> require((parsed as Float) in 1f..2000f)
                "widget_offset" -> require((parsed as Float) in -2000f..2000f)
            }
            key to parsed
        }
        val appArray = root["apps"].asJsonArray
        require(appArray.size() <= 10000)
        val restoredApps = appArray.map { item ->
            val obj = item.asJsonObject
            val id = obj["packageId"].asString
            require(id.matches(Regex("[A-Za-z0-9_.]{1,255}")))
            val name = obj["displayName"]?.takeUnless { it.isJsonNull }?.asString
            require(name == null || name.length <= 1000)
            val position = obj["favouritePosition"]?.takeUnless { it.isJsonNull }?.asDouble
            require(position == null || position.isFinite())
            ModifiedAppEntity(id, name, obj["isHidden"].asBoolean, obj["isChallenge"].asBoolean, position)
        }
        require(restoredApps.map { it.packageId }.distinct().size == restoredApps.size)
        // All input is validated before either store is changed. Unrelated device settings are retained.
        apps.restoreBackup(restoredApps)
        val oldNotes = notes.first()
        store.edit { p ->
            schema.keys.forEach { name -> p.asMap().keys.firstOrNull { it.name == name }?.let { p.remove(it) } }
            validatedSettings.forEach { (name, value) -> when (value) {
                is Boolean -> p[booleanPreferencesKey(name)] = value
                is String -> p[stringPreferencesKey(name)] = value
                is Int -> p[intPreferencesKey(name)] = value
                is Float -> p[floatPreferencesKey(name)] = value
            } }
            p[notesKey] = encodeNotes(restoredNotes)
            p.remove(orderKey)
        }
        oldNotes.forEach { WorkManager.getInstance(context).cancelUniqueWork("note-${it.id}") }
        refreshReminders()
    }
}

internal fun encodeNotes(notes: List<QuickNote>): String = JsonArray().apply {
    notes.forEach { note -> add(JsonObject().apply {
        addProperty("id", note.id); addProperty("text", note.text); addProperty("done", note.done)
        note.dueAt?.let { addProperty("dueAt", it) }; addProperty("notified", note.notified); addProperty("pinned", note.pinned)
    }) }
}.toString()

internal fun decodeNotes(json: String): List<QuickNote> {
    val values = JsonParser.parseString(json).asJsonArray
    require(values.size() <= 1000)
    return values.map { value ->
        val obj = value.asJsonObject
        val id = obj["id"].asString
        val text = obj["text"].asString
        require(id.matches(Regex("[a-zA-Z0-9-]{1,64}")) && text.isNotBlank() && text.length <= 2000)
        val due = obj["dueAt"]?.takeUnless { it.isJsonNull }?.asLong
        require(due == null || due in 1..4102444800000L)
        QuickNote(id, text, obj["done"]?.asBoolean ?: false, due, obj["notified"]?.asBoolean ?: false, obj["pinned"]?.asBoolean ?: false)
    }.also { list -> require(list.map { it.id }.distinct().size == list.size) }
}
