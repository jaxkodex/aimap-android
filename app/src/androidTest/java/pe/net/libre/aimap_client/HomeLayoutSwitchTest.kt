package pe.net.libre.aimap_client

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.net.libre.aimap_client.home.AppState
import pe.net.libre.aimap_client.home.HomeRoute
import pe.net.libre.aimap_client.home.SampleHome
import pe.net.libre.aimap_client.home.toUi
import pe.net.libre.aimap_client.settings.HomeLayout
import pe.net.libre.aimap_client.settings.HomeLayoutStore
import pe.net.libre.aimap_client.ui.theme.AimapTheme

/** The account sheet switches the Home layout at once, and the store remembers the choice. */
@RunWith(AndroidJUnit4::class)
class HomeLayoutSwitchTest {
    @get:Rule
    val rule = createComposeRule()

    private val store = HomeLayoutStore(InstrumentationRegistry.getInstrumentation().targetContext)

    private val state = AppState.SignedIn(
        email = "someone@example.com",
        home = SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "JV"),
        loading = false,
    )

    @Test
    fun the_account_sheet_swaps_the_bay_for_the_queue_and_back() {
        runBlocking { store.set(HomeLayout.V4) }
        rule.setContent {
            val layout by store.layout.collectAsState(HomeLayout.V4)
            val scope = rememberCoroutineScope()
            AimapTheme(darkTheme = true) {
                HomeRoute(
                    state = state,
                    layout = layout,
                    onLayout = { chosen -> scope.launch { store.set(chosen) } },
                    onRefresh = {},
                    onSignOut = {},
                )
            }
        }

        pick("v5")
        rule.onNodeWithText("Queue layout coming next").assertExists()
        assertEquals(HomeLayout.V5, runBlocking { store.layout.first() })

        pick("v4")
        rule.onNodeWithText("Ana Ruiz").assertExists()
        assertEquals(HomeLayout.V4, runBlocking { store.layout.first() })
    }

    /** Open the sheet from the avatar, pick a layout, close. */
    private fun pick(label: String) {
        rule.onNodeWithText("JV").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(label).performClick()
        rule.onNodeWithText("Close").performClick()
        rule.waitForIdle()
    }
}
