package app.trainer.feature.account.identities.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.LinkedIdentity
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.login_methods_linked_at
import app.trainer.strings.welcome_telegram_expired
import app.trainer.strings.welcome_telegram_failed
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private val CONFIRMATION_POLL_DELAY = 2.seconds
private const val CONFIRMATION_ATTEMPTS = 60

class LoginMethodsScreenModel(
    private val identitiesRepository: IdentitiesRepository,
    private val authRepository: AuthRepository,
) : BaseScreenModel<LoginMethodsState, LoginMethodsSideEffect, LoginMethodsEvent>(
    initialState = LoginMethodsState.initial(),
) {

    private var linkJob: Job? = null

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = identitiesRepository.linkedIdentities()) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = loaded) }
                is RequestResult.Success -> show(loaded.data)
            }
        }
    }

    override fun dispatch(event: LoginMethodsEvent) {
        when (event) {
            LoginMethodsEvent.OnReloadRequested -> onFetchData()
            LoginMethodsEvent.OnLinkTelegramClicked -> linkTelegram()
            LoginMethodsEvent.OnLinkCancelled -> cancelLink()
            is LoginMethodsEvent.OnUnlinkClicked -> updateState { it.copy(confirmedUnlink = event.provider) }
            LoginMethodsEvent.OnUnlinkDismissed -> updateState { it.copy(confirmedUnlink = null) }
            LoginMethodsEvent.OnUnlinkConfirmed -> unlink()
        }
    }

    private fun cancelLink() {
        linkJob?.cancel()
        linkJob = null
        updateState { it.copy(link = LinkProgress.Idle) }
    }

    private fun linkTelegram() {
        linkJob?.cancel()
        linkJob = screenModelScope {
            updateState { it.copy(link = LinkProgress.Waiting) }
            when (val started = authRepository.startTelegramLogin()) {
                is RequestResult.Error -> showLinkFailure(started)
                is RequestResult.Success -> {
                    postSideEffect(LoginMethodsSideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    awaitLink(claimToken = started.data.claimToken)
                }
            }
        }
    }

    private suspend fun awaitLink(claimToken: String) {
        repeat(CONFIRMATION_ATTEMPTS) {
            delay(CONFIRMATION_POLL_DELAY)
            val linked = identitiesRepository.linkProvider(
                provider = AuthProvider.TELEGRAM,
                token = claimToken,
            )
            when {
                linked is RequestResult.Success -> {
                    updateState { it.copy(link = LinkProgress.Idle) }
                    show(linked.data)
                    postSideEffect(LoginMethodsSideEffect.ShowLinked)
                    return
                }
                linked is RequestResult.Error && linked.kind == RequestFailure.Conflict -> Unit
                linked is RequestResult.Error -> {
                    showLinkFailure(linked)
                    return
                }
            }
        }
        val expired = getString(Res.string.welcome_telegram_expired)
        updateState { it.copy(link = LinkProgress.Failed(expired)) }
    }

    private fun unlink() {
        screenModelScope { current ->
            val provider = current.confirmedUnlink ?: return@screenModelScope
            updateState { it.copy(confirmedUnlink = null, unlinking = provider) }
            val unlinked = identitiesRepository.unlinkProvider(provider)
            updateState { it.copy(unlinking = null) }
            when (unlinked) {
                is RequestResult.Error -> postSideEffect(LoginMethodsSideEffect.ShowFailure(unlinked))
                is RequestResult.Success -> show(unlinked.data)
            }
        }
    }

    private suspend fun showLinkFailure(failure: RequestResult.Error) {
        val message = when (failure.kind) {
            RequestFailure.Gone -> getString(Res.string.welcome_telegram_expired)
            RequestFailure.Conflict -> getString(Res.string.welcome_telegram_failed)
            else -> getString(Res.string.welcome_telegram_failed)
        }
        updateState { it.copy(link = LinkProgress.Failed(message)) }
    }

    private suspend fun show(identities: List<LinkedIdentity>) {
        val rows = identities.map { identity ->
            LoginMethodRow(
                provider = identity.provider,
                linkedAtLabel = getString(
                    Res.string.login_methods_linked_at,
                    dayMonthOf(
                        Instant.parse(identity.linkedAtIso)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    ),
                ),
            )
        }
        updateState { it.copy(methods = rows.toImmutableList(), isLoading = false, failure = null) }
    }
}
