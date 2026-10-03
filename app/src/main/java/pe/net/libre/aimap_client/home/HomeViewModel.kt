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
        /** The last handled message can still be put back. */
        val undo: UndoOffer? = null,
        /** An action failed and the Bay is back as it was. */
        val actionError: String? = null,
        /** Debug only: the Bay on [SampleHome], with no account and no API behind it. */
        val sample: Boolean = false,
    ) : AppState
}

class HomeViewModel(
    private val auth: GoogleAuth = GoogleAuth(),
    /** Read-only, so the screens that fetch one message share this one. */
    val api: AimapApi = AimapApi(BuildConfig.API_BASE_URL, auth::idToken),
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.Starting)
    val state: StateFlow<AppState> = _state.asStateFlow()
    private var loading: Job? = null

    /** What the last `GET /home` said, plus whatever the keys have done to it since. */
    private val actions = ActionStore(
        scope = viewModelScope,
        write = { id, action -> if (sample) SampleHome.act(id, action) else api.act(id, action) },
        reconcile = { refresh(quiet = true) },
    )

    /** The rest of what [HomeUi] needs, kept from the last refresh so an action can redraw the Bay. */
    private var accounts = 0
    private var initials = ""
    private var sample = false

    init {
        viewModelScope.launch { actions.state.collect(::draw) }
        viewModelScope.launch {
            auth.user.collect { user ->
                if (user == null && !sample) {
                    loading?.cancel()
                    actions.show(null)
                    _state.value = AppState.SignedOut()
                } else if (user != null && (_state.value as? AppState.SignedIn)?.email != user.email) {
                    sample = false
                    _state.value = AppState.SignedIn(user.email.orEmpty())
                    refresh()
                }
            }
        }
    }

    /** Handled, Later or Undo on one message, from v4, from v5 or from the read screen. */
    fun act(messageId: Long, action: HomeAction) = actions.act(messageId, action)

    fun dismissActionError() = actions.clearError()

    /**
     * Debug only: the Bay on [SampleHome], so the keys can be tried with no account and no API.
     * Acting on [SampleHome.REFUSED] always fails, which shows the rollback and the error bar.
     */
    fun openSample() {
        if (!BuildConfig.DEBUG) return
        sample = true
        accounts = 3
        initials = "AR"
        _state.value = AppState.SignedIn(SampleHome.EMAIL, loading = false, sample = true)
        actions.show(SampleHome.home)
    }

    /** The Bay the store holds, drawn. Every action and every refresh comes through here. */
    private fun draw(acting: Acting) {
        _state.update { state ->
            (state as? AppState.SignedIn)?.copy(
                home = acting.home?.toUi(ZonedDateTime.now(), accounts = accounts, initials = initials),
                undo = acting.undo,
                actionError = acting.error,
            ) ?: state
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
        if (sample) {
            // Nobody is signed in behind the sample Bay, so leaving it is the whole sign-out.
            sample = false
            actions.show(null)
            _state.value = AppState.SignedOut()
        }
        viewModelScope.launch { auth.signOut(context) }
    }

    /** A [quiet] refresh leaves the pull-to-refresh spinner alone: it only reconciles an action. */
    fun refresh(quiet: Boolean = false) {
        if (sample) return
        val user = auth.currentUser ?: return
        loading?.cancel()
        if (!quiet) _state.update { (it as? AppState.SignedIn)?.copy(loading = true, error = null) ?: it }
        loading = viewModelScope.launch {
            try {
                val inboxes = async { api.accounts() }
                val home = api.home()
                accounts = inboxes.await().size
                initials = initials(user)
                actions.show(home)
                _state.update { (it as? AppState.SignedIn)?.copy(loading = false, error = null) ?: it }
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
