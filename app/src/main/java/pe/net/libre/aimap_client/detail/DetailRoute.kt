package pe.net.libre.aimap_client.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pe.net.libre.aimap_client.BuildConfig
import pe.net.libre.aimap_client.api.AimapApi
import pe.net.libre.aimap_client.api.ApiException
import pe.net.libre.aimap_client.home.HomeAction
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * One message, read. Two calls: the metadata draws the strip, then the body fills the letter,
 * so a slow or missing body never holds the strip back.
 */
@Composable
fun DetailRoute(
    messageId: Long,
    api: AimapApi,
    onBack: () -> Unit,
    /** Debug only: read the sample message whatever the id, because there is no API to call. */
    sample: Boolean = false,
    onAction: (Long, HomeAction) -> Unit = { _, _ -> },
) {
    val scope = rememberCoroutineScope()
    var state by remember(messageId) { mutableStateOf<Read>(Read.Loading) }
    var draftState by remember(messageId) { mutableStateOf<DraftState?>(null) }
    val sampled = BuildConfig.DEBUG && (sample || messageId == SampleMessage.ID)

    val handleMailAppError: (String?) -> Unit = { error ->
        draftState = (draftState as? DraftState.Ready)?.copy(mailAppError = error)
    }

    val loadBody: () -> Unit = {
        scope.launch {
            (state as? Read.Ready)?.let { state = it.copy(body = BodyState.Loading) }
            val body = fetchBody(api, messageId)
            (state as? Read.Ready)?.let { state = it.copy(body = body) }
        }
    }
    val load: () -> Unit = {
        scope.launch {
            state = Read.Loading
            // The sample message never leaves the app, so it needs no call, body included.
            state = if (sampled) SampleMessage.read(messageId) else fetchMessage(api, messageId)
            if (state is Read.Ready && !sampled) loadBody()
        }
    }
    val loadDraft: (String) -> Unit = { instructions ->
        scope.launch {
            draftState = DraftState.Loading
            draftState = if (sampled) {
                try {
                    DraftState.Ready(SampleMessage.sampleDraft(messageId), instructions, SampleMessage.earlier)
                } catch (e: Exception) {
                    DraftState.Failed(e.message ?: "Unknown error", instructions)
                }
            } else {
                fetchDraft(api, messageId, instructions)
            }
        }
    }

    LaunchedEffect(messageId) { load() }

    DetailScreen(
        state,
        onBack = onBack,
        onRetry = load,
        onRetryBody = loadBody,
        onAction = onAction,
        draftState = draftState,
        onDraftReply = { loadDraft("") },
        onCloseDraft = { draftState = null },
        onRegenerateDraft = loadDraft,
        onMailAppError = handleMailAppError,
    )
}

private suspend fun fetchMessage(api: AimapApi, id: Long): Read = try {
    Read.Ready(api.message(id).toUi(ZonedDateTime.now()), BodyState.Loading)
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    if (e.status == 404) Read.Gone else Read.Failed("aimap said: ${e.message}")
} catch (e: Exception) {
    Read.Failed("Couldn't reach aimap. ${e.message.orEmpty()}".trim())
}

private suspend fun fetchBody(api: AimapApi, id: Long): BodyState = try {
    BodyState.Text(api.messageBody(id))
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    BodyState.Failed(
        if (e.status == 404) "The letter itself is not in the bucket any more."
        else "aimap said: ${e.message}"
    )
} catch (e: Exception) {
    BodyState.Failed("Couldn't reach aimap. ${e.message.orEmpty()}".trim())
}

/**
 * The draft and the thread behind it, asked for together: the draft is the one that matters, so a
 * thread call that fails leaves the sheet without its list of earlier letters and nothing else.
 */
private suspend fun fetchDraft(api: AimapApi, id: Long, instructions: String): DraftState = try {
    coroutineScope {
        val thread = async { runCatching { api.thread(id).messages }.getOrDefault(emptyList()) }
        val draft = api.draft(id, instructions.ifBlank { null })
        val earlier = thread.await()
            .filter { it.messageId != id && it.messageId in draft.usedMessageIds }
        DraftState.Ready(draft, instructions, earlier)
    }
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    DraftState.Failed("aimap said: ${e.message}", instructions)
} catch (e: Exception) {
    DraftState.Failed("Couldn't reach aimap. ${e.message.orEmpty()}".trim(), instructions)
}
