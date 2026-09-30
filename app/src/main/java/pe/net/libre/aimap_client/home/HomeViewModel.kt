package pe.net.libre.aimap_client.home

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.aimap_client.BuildConfig
import pe.net.libre.aimap_client.api.AimapApi
import pe.net.libre.aimap_client.api.ApiException
import pe.net.libre.aimap_client.auth.GoogleAuth
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

sealed interface AppState {
    /** Firebase has not said yet whether someone is signed in. */
    data object Starting : AppState

    data class SignedOut(val busy: Boolean = false, val error: String? = null) : AppState

    data class SignedIn(
        val email: String,
        val home: HomeUi? = null,
        val loading: Boolean = true,
        val error: String? = null,
        /** The API refused this account: signing in again with another one is the fix. */
        val forbidden: Boolean = false,
    ) : AppState
}

class HomeViewModel(
    private val auth: GoogleAuth = GoogleAuth(),
    private val api: AimapApi = AimapApi(BuildConfig.API_BASE_URL, auth::idToken),
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.Starting)
    val state: StateFlow<AppState> = _state.asStateFlow()
    private var loading: Job? = null

    init {
        viewModelScope.launch {
            auth.user.collect { user ->
                if (user == null) {
                    loading?.cancel()
                    _state.value = AppState.SignedOut()
                } else if ((_state.value as? AppState.SignedIn)?.email != user.email) {
                    _state.value = AppState.SignedIn(user.email.orEmpty())
                    refresh()
                }
            }
        }
    }

    fun signIn(activity: Context) {
        _state.value = AppState.SignedOut(busy = true)
        viewModelScope.launch {
            try {
                auth.signIn(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (_: GetCredentialCancellationException) {
                _state.value = AppState.SignedOut()
            } catch (_: NoCredentialException) {
                _state.value = AppState.SignedOut(error = "No Google account on this device. Add one in Settings.")
            } catch (e: Exception) {
                _state.value = AppState.SignedOut(error = "Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch { auth.signOut(context) }
    }

    fun refresh() {
        val user = auth.currentUser ?: return
        loading?.cancel()
        _state.update { (it as? AppState.SignedIn)?.copy(loading = true, error = null) ?: it }
        loading = viewModelScope.launch {
            try {
                val accounts = async { api.accounts() }
                val home = api.home()
                val ui = home.toUi(ZonedDateTime.now(), accounts = accounts.await().size, initials = initials(user))
                _state.update { (it as? AppState.SignedIn)?.copy(home = ui, loading = false) ?: it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val forbidden = e.status == 403
                val message = if (forbidden) "${user.email} is not allowed to use this aimap." else "aimap said: ${e.message}"
                _state.update { (it as? AppState.SignedIn)?.copy(loading = false, error = message, forbidden = forbidden) ?: it }
            } catch (e: Exception) {
                _state.update {
                    (it as? AppState.SignedIn)?.copy(loading = false, error = "Couldn't reach aimap. ${e.message.orEmpty()}".trim())
                        ?: it
                }
            }
        }
    }
}

/** "Ana Ruiz" -> "AR"; falls back to the email's first letter. */
fun initials(name: String?, email: String?): String {
    val words = name.orEmpty().split(' ').filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words.first().first()}${words.last().first()}"
        words.size == 1 -> words.first().take(2)
        else -> email.orEmpty().take(1)
    }.uppercase()
}

private fun initials(user: FirebaseUser) = initials(user.displayName, user.email)
