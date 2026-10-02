package pe.net.libre.aimap_client.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which Home the Bay draws. V5 is the queue layout, still a placeholder. */
enum class HomeLayout(val label: String) {
    V4("v4"),
    V5("v5"),
}

/** An unknown or missing name means the default, so a bad write never leaves the app blank. */
fun homeLayoutOf(stored: String?): HomeLayout =
    HomeLayout.entries.firstOrNull { it.name == stored } ?: HomeLayout.V4

private val Context.settings: DataStore<Preferences> by preferencesDataStore("settings")

private val HomeLayoutKey = stringPreferencesKey("home_layout")

/** The chosen Home layout, remembered across restarts. */
class HomeLayoutStore(private val context: Context) {
    val layout: Flow<HomeLayout> = context.settings.data.map { homeLayoutOf(it[HomeLayoutKey]) }

    suspend fun set(layout: HomeLayout) {
        context.settings.edit { it[HomeLayoutKey] = layout.name }
    }
}
