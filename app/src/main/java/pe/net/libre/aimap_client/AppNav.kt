package pe.net.libre.aimap_client

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import pe.net.libre.aimap_client.api.AimapApi
import pe.net.libre.aimap_client.detail.DetailRoute
import pe.net.libre.aimap_client.home.AppState
import pe.net.libre.aimap_client.home.HomeAction
import pe.net.libre.aimap_client.home.HomeRoute
import pe.net.libre.aimap_client.settings.HomeLayout

/**
 * Where you are once signed in: the Bay, or one message read. No navigation library, just
 * which message is open. System Back (and the predictive back gesture, which [BackHandler]
 * drives on this target) closes the message, and the Bay comes back where it was: its scroll
 * position and its own remembered state stay in the saveable holder while the read screen is up.
 */
@Composable
fun SignedInApp(
    state: AppState.SignedIn,
    api: AimapApi,
    layout: HomeLayout,
    onLayout: (HomeLayout) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onAction: (Long, HomeAction) -> Unit,
    onDismissError: () -> Unit,
) {
    val screens = rememberSaveableStateHolder()
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val id = open

    if (id == null) {
        screens.SaveableStateProvider("bay") {
            HomeRoute(
                state,
                layout,
                onLayout,
                onRefresh = onRefresh,
                onSignOut = onSignOut,
                onOpenMessage = { open = it },
                onAction = onAction,
                onDismissError = onDismissError,
            )
        }
    } else {
        val close = {
            screens.removeState(key(id))
            open = null
        }
        BackHandler(onBack = close)
        screens.SaveableStateProvider(key(id)) {
            DetailRoute(id, api, onBack = close, sample = state.sample) { messageId, action ->
                // Handled and Later both finish the read: the Bay shows what moved, with the undo bar.
                onAction(messageId, action)
                close()
            }
        }
    }
}

private fun key(messageId: Long) = "message-$messageId"
