package pe.net.libre.aimap_client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.net.libre.aimap_client.auth.SignInScreen
import pe.net.libre.aimap_client.home.AppState
import pe.net.libre.aimap_client.home.HomeViewModel
import pe.net.libre.aimap_client.settings.HomeLayout
import pe.net.libre.aimap_client.settings.HomeLayoutStore
import pe.net.libre.aimap_client.ui.theme.AimapTheme

class MainActivity : ComponentActivity() {
    private val model: HomeViewModel by viewModels()
    private val layouts by lazy { HomeLayoutStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AimapTheme {
                val state by model.state.collectAsStateWithLifecycle()
                val layout by layouts.layout.collectAsStateWithLifecycle(HomeLayout.V4)
                val scope = rememberCoroutineScope()
                when (val s = state) {
                    AppState.Starting -> Box(Modifier.fillMaxSize().background(AimapTheme.colors.console))
                    is AppState.SignedOut -> SignInScreen(
                        busy = s.busy,
                        error = s.error,
                        // Debug only: the Bay on sample data, for when no account can sign in.
                        onSample = if (BuildConfig.DEBUG) model::openSample else null,
                        onSignIn = { model.signIn(this) },
                    )
                    is AppState.SignedIn -> SignedInApp(
                        state = s,
                        api = model.api,
                        layout = layout,
                        onLayout = { chosen -> scope.launch { layouts.set(chosen) } },
                        onRefresh = { model.refresh() },
                        onSignOut = { model.signOut(this) },
                        onAction = model::act,
                        onDismissError = model::dismissActionError,
                    )
                }
            }
        }
    }
}
