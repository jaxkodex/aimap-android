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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.net.libre.aimap_client.auth.SignInScreen
import pe.net.libre.aimap_client.home.AppState
import pe.net.libre.aimap_client.home.HomeRoute
import pe.net.libre.aimap_client.home.HomeViewModel
import pe.net.libre.aimap_client.ui.theme.AimapTheme

class MainActivity : ComponentActivity() {
    private val model: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AimapTheme {
                val state by model.state.collectAsStateWithLifecycle()
                when (val s = state) {
                    AppState.Starting -> Box(Modifier.fillMaxSize().background(AimapTheme.colors.console))
                    is AppState.SignedOut -> SignInScreen(s.busy, s.error) { model.signIn(this) }
                    is AppState.SignedIn -> HomeRoute(s, onRefresh = model::refresh, onSignOut = { model.signOut(this) })
                }
            }
        }
    }
}
