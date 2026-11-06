package com.eightharsh.eightharsh.core.data.personal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.eightharsh.eightharsh.core.data.database.ModifiedAppsDao
import com.eightharsh.eightharsh.core.data.entity.ModifiedAppEntity
import com.google.gson.JsonParser
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    private val store = mockk<DataStore<Preferences>> {
        every { data } returns state
        coEvery { updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            transform(state.value).also { state.value = it }
        }
    }
    private val apps = mockk<ModifiedAppsDao>(relaxed = true)
    private val repo = PersonalRepositoryImpl(store, apps, mockk<Context>())
    private fun backup(settings: String = "{}", notes: String = "[]") = """{"format":"eightharsh-backup","version":1,"settings":$settings,"notes":$notes,"apps":[]}"""

    @Test fun `export includes custom app data and excludes active sessions and widget binding`() = runBlocking {
        state.value = preferencesOf(intPreferencesKey("theme") to 11, stringPreferencesKey("active_session_package") to "private.app", intPreferencesKey("widget_ID") to 101)
        val app = ModifiedAppEntity("test.app", "Custom name", true, true, 2.5)
        coEvery { apps.getAllForBackup() } returns listOf(app)
        val root = JsonParser.parseString(repo.exportBackup()).asJsonObject
        assertEquals(11, root["settings"].asJsonObject["theme"].asInt)
        assertFalse(root["settings"].asJsonObject.has("active_session_package"))
        assertFalse(root["settings"].asJsonObject.has("widget_ID"))
        assertEquals("Custom name", root["apps"].asJsonArray[0].asJsonObject["displayName"].asString)
        assertEquals(2.5, root["apps"].asJsonArray[0].asJsonObject["favouritePosition"].asDouble, 0.0)
    }
    @Test fun `restore replaces supported preferences and retains onboarding and widget binding`() = runBlocking {
        state.value = preferencesOf(intPreferencesKey("theme") to 13, intPreferencesKey("widget_ID") to 101, booleanPreferencesKey("first_time") to false)
        repo.importBackup(backup("""{"theme":11,"apps_alignment":"Center","show_quick_notes":true}"""))
        assertEquals(11, state.value[intPreferencesKey("theme")])
        assertEquals("Center", state.value[stringPreferencesKey("apps_alignment")])
        assertEquals(101, state.value[intPreferencesKey("widget_ID")])
        assertEquals(false, state.value[booleanPreferencesKey("first_time")])
        coVerify(exactly = 1) { apps.restoreBackup(emptyList()) }
    }
    @Test fun `invalid alignment is rejected without modifying either store`() = runBlocking {
        val before = preferencesOf(intPreferencesKey("theme") to 13)
        state.value = before
        try { repo.importBackup(backup("""{"apps_alignment":"bad"}""")); fail("Should reject") } catch (_: IllegalArgumentException) { }
        assertEquals(before, state.value)
        coVerify(exactly = 0) { apps.restoreBackup(any()) }
    }
    @Test fun `unknown backup version is rejected without modifying apps`() = runBlocking {
        try { repo.importBackup(backup().replace("\"version\":1", "\"version\":2")); fail("Should reject") } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { apps.restoreBackup(any()) }
    }
    @Test fun `invalid notes are rejected before preferences or database change`() = runBlocking {
        try { repo.importBackup(backup(notes = """[{"id":"a","text":""}]""")); fail("Should reject") } catch (_: IllegalArgumentException) { }
        assertEquals(emptyPreferences(), state.value)
        coVerify(exactly = 0) { apps.restoreBackup(any()) }
    }
}
