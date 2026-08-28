package app.trainer.feature.traininglog.presentation.newexercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trainer.base.BaseScreenModel
import app.trainer.base.failure.toastMessage
import app.trainer.data.traininglog.Equipment
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.MuscleGroup
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.feature.traininglog.presentation.label
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.exercise_fields_bodyweight
import app.trainer.strings.exercise_fields_cardio
import app.trainer.strings.exercise_fields_strength
import app.trainer.strings.exercise_kind_bodyweight
import app.trainer.strings.exercise_kind_cardio
import app.trainer.strings.exercise_kind_strength
import app.trainer.strings.new_exercise_description_label
import app.trainer.strings.new_exercise_equipment_label
import app.trainer.strings.new_exercise_kind_hint
import app.trainer.strings.new_exercise_muscle_label
import app.trainer.strings.new_exercise_name_label
import app.trainer.strings.new_exercise_save_action
import app.trainer.strings.new_exercise_saved_message
import app.trainer.strings.new_exercise_title
import app.trainer.strings.new_exercise_video_label
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.ToastHostState
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

data class NewExerciseState(
    val name: String,
    val primaryMuscle: MuscleGroup?,
    val equipment: Equipment?,
    val kind: ExerciseKind,
    val description: String,
    val videoUrl: String,
    val isSaving: Boolean,
) {

    val isSaveEnabled: Boolean
        get() = name.isNotBlank() && primaryMuscle != null && equipment != null && !isSaving

    companion object {

        fun initial(): NewExerciseState = NewExerciseState(
            description = "",
            videoUrl = "",
            name = "",
            primaryMuscle = null,
            equipment = null,
            kind = ExerciseKind.STRENGTH,
            isSaving = false,
        )
    }
}

sealed interface NewExerciseEvent {

    data object OnSaveClicked : NewExerciseEvent

    data class OnNameChanged(val name: String) : NewExerciseEvent

    data class OnMuscleChanged(val muscle: MuscleGroup) : NewExerciseEvent

    data class OnEquipmentChanged(val equipment: Equipment) : NewExerciseEvent

    data class OnKindChanged(val kind: ExerciseKind) : NewExerciseEvent

    data class OnDescriptionChanged(val text: String) : NewExerciseEvent

    data class OnVideoUrlChanged(val text: String) : NewExerciseEvent
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
            is NewExerciseEvent.OnMuscleChanged -> updateState { it.copy(primaryMuscle = event.muscle) }
            is NewExerciseEvent.OnEquipmentChanged -> updateState { it.copy(equipment = event.equipment) }
            is NewExerciseEvent.OnKindChanged -> updateState { it.copy(kind = event.kind) }
            is NewExerciseEvent.OnDescriptionChanged -> updateState { it.copy(description = event.text) }
            is NewExerciseEvent.OnVideoUrlChanged -> updateState { it.copy(videoUrl = event.text) }
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            updateState { it.copy(isSaving = true) }
            val muscle = state.primaryMuscle ?: return@screenModelScope
            val equipment = state.equipment ?: return@screenModelScope
            val created = trainingLogRepository.createExercise(
                name = state.name.trim(),
                primaryMuscle = muscle,
                equipment = equipment,
                kind = state.kind,
                description = state.description.trim().ifEmpty { null },
                videoUrl = state.videoUrl.trim().ifEmpty { null },
            )
            updateState { it.copy(isSaving = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(NewExerciseSideEffect.ShowFailure(created))
                is RequestResult.Success -> postSideEffect(NewExerciseSideEffect.Saved)
            }
        }
    }
}

internal val EXERCISE_CREATED = ScreenRequestKey<Unit>("exerciseCreated")

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
                    toastHost.show(getString(Res.string.new_exercise_saved_message))
                    navigator.popWithResult(EXERCISE_CREATED, Unit)
                }
                is NewExerciseSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
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
        AppTopBar(
            title = stringResource(Res.string.new_exercise_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
        ) {
            AppTextField(
                value = state.name,
                onValueChange = { onEvent(NewExerciseEvent.OnNameChanged(it)) },
                label = TextFieldLabel.Text(stringResource(Res.string.new_exercise_name_label)),
            )
            ChoiceRow(
                title = stringResource(Res.string.new_exercise_muscle_label),
                options = MuscleGroup.entries,
                selected = state.primaryMuscle,
                labelOf = { stringResource(it.label()) },
                onSelect = { onEvent(NewExerciseEvent.OnMuscleChanged(it)) },
            )
            ChoiceRow(
                title = stringResource(Res.string.new_exercise_equipment_label),
                options = Equipment.entries,
                selected = state.equipment,
                labelOf = { stringResource(it.label()) },
                onSelect = { onEvent(NewExerciseEvent.OnEquipmentChanged(it)) },
            )
            ExerciseKind.entries.forEach { kind ->
                KindOption(
                    kind = kind,
                    isSelected = state.kind == kind,
                    onClick = { onEvent(NewExerciseEvent.OnKindChanged(kind)) },
                )
            }
            AppTextField(
                value = state.description,
                onValueChange = { onEvent(NewExerciseEvent.OnDescriptionChanged(it)) },
                kind = TextFieldKind.Multiline,
                label = TextFieldLabel.Text(stringResource(Res.string.new_exercise_description_label)),
            )
            AppTextField(
                value = state.videoUrl,
                onValueChange = { onEvent(NewExerciseEvent.OnVideoUrlChanged(it)) },
                label = TextFieldLabel.Text(stringResource(Res.string.new_exercise_video_label)),
            )
            AppText(
                text = stringResource(Res.string.new_exercise_kind_hint),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.new_exercise_save_action),
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
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T?,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        AppText(
            text = title,
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            options.forEach { option ->
                AppButton(
                    text = labelOf(option),
                    onClick = { onSelect(option) },
                    tone = if (option == selected) ButtonTone.Primary else ButtonTone.Secondary,
                    size = ButtonSize.Small,
                )
            }
        }
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

@Composable
private fun kindTitle(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> stringResource(Res.string.exercise_kind_strength)
    ExerciseKind.BODYWEIGHT -> stringResource(Res.string.exercise_kind_bodyweight)
    ExerciseKind.CARDIO -> stringResource(Res.string.exercise_kind_cardio)
}

@Composable
private fun kindHint(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> stringResource(Res.string.exercise_fields_strength)
    ExerciseKind.BODYWEIGHT -> stringResource(Res.string.exercise_fields_bodyweight)
    ExerciseKind.CARDIO -> stringResource(Res.string.exercise_fields_cardio)
}
