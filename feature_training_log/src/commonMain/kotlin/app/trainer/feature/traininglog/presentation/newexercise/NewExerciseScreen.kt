package app.trainer.feature.traininglog.presentation.newexercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trainer.base.BaseScreenModel
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.ToastHostState
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.AppTopBar

private const val TITLE = "Новое упражнение"
private const val NAME_LABEL = "Название"
private const val MUSCLE_LABEL = "Группа мышц"
private const val SAVE_ACTION = "Сохранить в справочник"
private const val KIND_HINT = "Тип нельзя изменить после первой записи."
private const val SAVED_MESSAGE = "Упражнение добавлено"

data class NewExerciseState(
    val name: String,
    val muscleGroup: String,
    val kind: ExerciseKind,
    val isSaving: Boolean,
) {

    val isSaveEnabled: Boolean
        get() = name.isNotBlank() && !isSaving

    companion object {

        fun initial(): NewExerciseState = NewExerciseState(
            name = "",
            muscleGroup = "",
            kind = ExerciseKind.STRENGTH,
            isSaving = false,
        )
    }
}

sealed interface NewExerciseEvent {

    data object OnSaveClicked : NewExerciseEvent

    data class OnNameChanged(val name: String) : NewExerciseEvent

    data class OnMuscleGroupChanged(val muscleGroup: String) : NewExerciseEvent

    data class OnKindChanged(val kind: ExerciseKind) : NewExerciseEvent
}

sealed interface NewExerciseSideEffect {

    data object Saved : NewExerciseSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NewExerciseSideEffect
}

class NewExerciseScreenModel(
    private val trainingLogRepository: TrainingLogRepository,
) : BaseScreenModel<NewExerciseState, NewExerciseSideEffect, NewExerciseEvent>(
    initialState = NewExerciseState.initial(),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: NewExerciseEvent) {
        when (event) {
            NewExerciseEvent.OnSaveClicked -> save()
            is NewExerciseEvent.OnNameChanged -> updateState { it.copy(name = event.name) }
            is NewExerciseEvent.OnMuscleGroupChanged -> updateState {
                it.copy(muscleGroup = event.muscleGroup)
            }
            is NewExerciseEvent.OnKindChanged -> updateState { it.copy(kind = event.kind) }
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            updateState { it.copy(isSaving = true) }
            val created = trainingLogRepository.createExercise(
                name = state.name.trim(),
                muscleGroup = state.muscleGroup.trim().ifEmpty { null },
                kind = state.kind,
            )
            updateState { it.copy(isSaving = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(NewExerciseSideEffect.ShowFailure(created))
                is RequestResult.Success -> postSideEffect(NewExerciseSideEffect.Saved)
            }
        }
    }
}

class NewExerciseScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NewExerciseScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        NewExerciseView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                NewExerciseSideEffect.Saved -> {
                    toastHost.show(SAVED_MESSAGE)
                    navigator.pop()
                }
                is NewExerciseSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
            }
        }
    }
}

@Composable
private fun NewExerciseView(
    state: NewExerciseState,
    onEvent: (NewExerciseEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = TITLE, leading = TopBarLeading.Back(onClick = onBackClick))
        Column(
            modifier = Modifier.weight(1f).padding(AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
        ) {
            AppTextField(
                value = state.name,
                onValueChange = { onEvent(NewExerciseEvent.OnNameChanged(it)) },
                label = TextFieldLabel.Text(NAME_LABEL),
            )
            AppTextField(
                value = state.muscleGroup,
                onValueChange = { onEvent(NewExerciseEvent.OnMuscleGroupChanged(it)) },
                label = TextFieldLabel.Text(MUSCLE_LABEL),
            )
            ExerciseKind.entries.forEach { kind ->
                KindOption(
                    kind = kind,
                    isSelected = state.kind == kind,
                    onClick = { onEvent(NewExerciseEvent.OnKindChanged(kind)) },
                )
            }
            AppText(
                text = KIND_HINT,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = SAVE_ACTION,
            onClick = { onEvent(NewExerciseEvent.OnSaveClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSaving -> ButtonState.Loading
                !state.isSaveEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}

@Composable
private fun KindOption(kind: ExerciseKind, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) AppTheme.colors.accentSoft else AppTheme.colors.bgSurface,
                shape = shape,
            )
            .border(
                width = if (isSelected) AppTheme.borders.field else AppTheme.borders.hairline,
                color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        AppText(
            text = kindTitle(kind),
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = kindHint(kind),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
    }
}

private fun kindTitle(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> "Силовое"
    ExerciseKind.BODYWEIGHT -> "Свой вес"
    ExerciseKind.CARDIO -> "Кардио"
}

private fun kindHint(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> "повторы и вес"
    ExerciseKind.BODYWEIGHT -> "только повторы"
    ExerciseKind.CARDIO -> "минуты и метры"
}
