package app.trainer.feature.clientcard.presentation.people.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList

class PeopleScreenModel(
    private val participantsRepository: ParticipantsRepository,
    private val authRepository: AuthRepository,
) : BaseScreenModel<PeopleState, PeopleSideEffect, PeopleEvent>(
    initialState = PeopleState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            loadPeople()
        }
    }

    override fun dispatch(event: PeopleEvent) {
        when (event) {
            PeopleEvent.OnRetryClicked -> onFetchData()
            PeopleEvent.OnCreateInviteClicked -> createInvite()
            is PeopleEvent.OnPersonClicked -> openPerson(event.userId)
        }
    }

    private suspend fun loadPeople() {
        updateState { it.copy(isLoading = true, isFailed = false) }
        when (val loaded = participantsRepository.clientsOfCoach()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, isFailed = true) }
                postSideEffect(PeopleSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> updateState { current ->
                current.copy(
                    people = loaded.data.map(::toRow).toImmutableList(),
                    isLoading = false,
                    isFailed = false,
                )
            }
        }
    }

    private fun createInvite() {
        screenModelScope {
            updateState { it.copy(isCreatingInvite = true) }
            val created = authRepository.createInvite()
            updateState { it.copy(isCreatingInvite = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(PeopleSideEffect.ShowFailure(created))
                is RequestResult.Success -> postSideEffect(
                    PeopleSideEffect.ShowInviteCode(code = created.data.code)
                )
            }
        }
    }

    private fun openPerson(userId: String) {
        screenModelScope {
            postSideEffect(PeopleSideEffect.OpenPerson(userId = userId))
        }
    }

    private fun toRow(client: CoachClient): PersonRow = PersonRow(
        userId = client.userId,
        displayName = client.displayName,
    )
}
