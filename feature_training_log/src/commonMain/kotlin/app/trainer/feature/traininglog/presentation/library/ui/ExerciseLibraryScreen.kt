package app.trainer.feature.traininglog.presentation.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.AppFailureState
import app.trainer.base.failure.toastMessage
import app.trainer.data.traininglog.Equipment
import app.trainer.data.traininglog.ExerciseOwnerKind
import app.trainer.data.traininglog.MuscleGroup
import app.trainer.feature.traininglog.presentation.label
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseFilter
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibraryEvent
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibraryScreenModel
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibrarySideEffect
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibraryState
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseRow
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseVideo
import app.trainer.feature.traininglog.presentation.newexercise.EXERCISE_CREATED
import app.trainer.media.VideoPlayer
import app.trainer.media.rememberVideoPicker
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.exercise_library_archive_action
import app.trainer.strings.exercise_library_client_author
import app.trainer.strings.exercise_library_create_action
import app.trainer.strings.exercise_library_create_named
import app.trainer.strings.exercise_library_empty_description
import app.trainer.strings.exercise_library_empty_title
import app.trainer.strings.exercise_library_filter_action
import app.trainer.strings.exercise_library_filter_applied
import app.trainer.strings.exercise_library_filter_apply
import app.trainer.strings.exercise_library_filter_clear
import app.trainer.strings.exercise_library_filter_title
import app.trainer.strings.exercise_library_nothing_found_description
import app.trainer.strings.exercise_library_nothing_found_title
import app.trainer.strings.exercise_library_owner_all
import app.trainer.strings.exercise_library_owner_label
import app.trainer.strings.exercise_library_owner_mine
import app.trainer.strings.exercise_library_owner_shared
import app.trainer.strings.exercise_library_search_clear
import app.trainer.strings.exercise_library_search_placeholder
import app.trainer.strings.exercise_library_title
import app.trainer.strings.exercise_library_video_action
import app.trainer.strings.exercise_library_video_replace
import app.trainer.strings.exercise_library_video_upload
import app.trainer.strings.exercise_library_video_uploaded
import app.trainer.strings.new_exercise_equipment_label
import app.trainer.strings.new_exercise_muscle_label
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmer
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppChipRow
import app.trainer.uikit.widgets.AppSearchField
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.ToastHostState
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 6
private const val SHIMMER_CARD_LINES = 2
private const val LOAD_MORE_SHIMMER_LINES = 2
private const val VIDEO_ASPECT_RATIO = 16f / 9f

class ExerciseLibraryScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ExerciseLibraryScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        navigator.handleResult(EXERCISE_CREATED) {
            screenModel.dispatch(event = ExerciseLibraryEvent.OnReloadRequested)
        }

        ExerciseLibraryView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                ExerciseLibrarySideEffect.OpenExerciseCreation ->
                    navigator.push(Screens.NewExercise)
                is ExerciseLibrarySideEffect.ShowFailure ->
                    toastHost.show(effect.failure.toastMessage())
                ExerciseLibrarySideEffect.ShowVideoUploaded ->
                    toastHost.show(getString(Res.string.exercise_library_video_uploaded))
            }
        }
    }
}

@Composable
private fun ExerciseLibraryView(
    state: ExerciseLibraryState,
    onEvent: (ExerciseLibraryEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground().navigationBarsPadding()) {
        AppTopBar(
            title = stringResource(Res.string.exercise_library_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        SearchRow(state = state, onEvent = onEvent)
        if (!state.filter.isEmpty) {
            AppliedFilters(state = state, onEvent = onEvent)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ExerciseLibraryEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                state.exercises.isEmpty() && state.isFiltered -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.exercise_library_nothing_found_title),
                    description = stringResource(Res.string.exercise_library_nothing_found_description),
                    action = if (state.search.isBlank()) {
                        PlaceholderAction.None
                    } else {
                        PlaceholderAction.Button(
                            text = stringResource(Res.string.exercise_library_create_named, state.search.trim()),
                            onClick = { onEvent(ExerciseLibraryEvent.OnCreateClicked) },
                        )
                    },
                )
                state.exercises.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.exercise_library_empty_title),
                    description = stringResource(Res.string.exercise_library_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> ExerciseList(state = state, onEvent = onEvent)
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.exercise_library_create_action),
            onClick = { onEvent(ExerciseLibraryEvent.OnCreateClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
        )
    }
    state.draftFilter?.let { draft ->
        FilterSheet(draft = draft, onEvent = onEvent)
    }
}

@Composable
private fun FilterSheet(draft: ExerciseFilter, onEvent: (ExerciseLibraryEvent) -> Unit) {
    AppBottomSheetContainer(title = stringResource(Res.string.exercise_library_filter_title)) {
        AppText(
            text = stringResource(Res.string.new_exercise_muscle_label),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppChipRow(
            options = MuscleGroup.entries,
            isSelected = { draft.muscles.contains(it) },
            labelOf = { stringResource(it.label()) },
            onSelect = { onEvent(ExerciseLibraryEvent.OnMuscleToggled(it)) },
        )
        AppText(
            text = stringResource(Res.string.new_exercise_equipment_label),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppChipRow(
            options = Equipment.entries,
            isSelected = { draft.equipment.contains(it) },
            labelOf = { stringResource(it.label()) },
            onSelect = { onEvent(ExerciseLibraryEvent.OnEquipmentToggled(it)) },
        )
        AppText(
            text = stringResource(Res.string.exercise_library_owner_label),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppChipRow(
            options = OWNER_OPTIONS,
            isSelected = { draft.ownerKind == it },
            labelOf = { owner ->
                when (owner) {
                    null -> stringResource(Res.string.exercise_library_owner_all)
                    ExerciseOwnerKind.SHARED -> stringResource(Res.string.exercise_library_owner_shared)
                    else -> stringResource(Res.string.exercise_library_owner_mine)
                }
            },
            onSelect = { onEvent(ExerciseLibraryEvent.OnOwnerKindChanged(it)) },
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.exercise_library_filter_apply),
            onClick = { onEvent(ExerciseLibraryEvent.OnFilterApplied) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.exercise_library_filter_clear),
            onClick = { onEvent(ExerciseLibraryEvent.OnFilterCleared) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}

private val OWNER_OPTIONS: List<ExerciseOwnerKind?> =
    listOf(null, ExerciseOwnerKind.SHARED, ExerciseOwnerKind.COACH)

@Composable
private fun SearchRow(state: ExerciseLibraryState, onEvent: (ExerciseLibraryEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppSearchField(
            modifier = Modifier.weight(1f),
            value = state.search,
            placeholder = stringResource(Res.string.exercise_library_search_placeholder),
            onValueChange = { onEvent(ExerciseLibraryEvent.OnSearchChanged(it)) },
            onClear = { onEvent(ExerciseLibraryEvent.OnSearchChanged("")) },
            clearDescription = stringResource(Res.string.exercise_library_search_clear),
        )
        AppButton(
            text = if (state.filter.isEmpty) {
                stringResource(Res.string.exercise_library_filter_action)
            } else {
                stringResource(Res.string.exercise_library_filter_applied, state.filter.appliedCount)
            },
            onClick = { onEvent(ExerciseLibraryEvent.OnFilterOpened) },
            tone = if (state.filter.isEmpty) ButtonTone.Secondary else ButtonTone.Primary,
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun AppliedFilters(state: ExerciseLibraryState, onEvent: (ExerciseLibraryEvent) -> Unit) {
    AppChipRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.dp16),
        options = state.filter.muscles.map(::MuscleFilterValue) +
            state.filter.equipment.map(::EquipmentFilterValue),
        isSelected = { true },
        labelOf = { value ->
            when (value) {
                is MuscleFilterValue -> stringResource(value.muscle.label())
                is EquipmentFilterValue -> stringResource(value.equipment.label())
            }
        },
        onSelect = { value ->
            when (value) {
                is MuscleFilterValue -> onEvent(ExerciseLibraryEvent.OnMuscleToggled(value.muscle))
                is EquipmentFilterValue -> onEvent(ExerciseLibraryEvent.OnEquipmentToggled(value.equipment))
            }
            onEvent(ExerciseLibraryEvent.OnFilterApplied)
        },
    )
}

private sealed interface FilterValue

private data class MuscleFilterValue(val muscle: MuscleGroup) : FilterValue

private data class EquipmentFilterValue(val equipment: Equipment) : FilterValue

@Composable
private fun ExerciseList(state: ExerciseLibraryState, onEvent: (ExerciseLibraryEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        items(items = state.exercises, key = ExerciseRow::exerciseId) { exercise ->
            ExerciseCard(modifier = Modifier.animateItem(), exercise = exercise, onEvent = onEvent)
        }
        if (state.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(state.nextCursor) { onEvent(ExerciseLibraryEvent.OnEndReached) }
                AppCardShimmer(lines = LOAD_MORE_SHIMMER_LINES)
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    modifier: Modifier = Modifier,
    exercise: ExerciseRow,
    onEvent: (ExerciseLibraryEvent) -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = exercise.name,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = exercise.details,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            exercise.author?.let { author ->
                AppText(
                    text = stringResource(Res.string.exercise_library_client_author, author),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.accent,
                )
            }
            exercise.description?.let { description ->
                AppText(
                    modifier = Modifier.padding(top = AppTheme.spacing.dp4),
                    text = description,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
            when (val video = exercise.video) {
                ExerciseVideo.None -> Unit
                is ExerciseVideo.Link -> {
                    val uriHandler = LocalUriHandler.current
                    AppButton(
                        text = stringResource(Res.string.exercise_library_video_action),
                        onClick = { runCatching { uriHandler.openUri(video.url) } },
                        tone = ButtonTone.Text,
                        size = ButtonSize.Small,
                    )
                }
                is ExerciseVideo.Uploaded -> VideoPlayer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.spacing.dp8)
                        .aspectRatio(VIDEO_ASPECT_RATIO),
                    url = video.url,
                )
            }
            if (exercise.isOwnedByMe) {
                val picker = rememberVideoPicker { picked ->
                    onEvent(ExerciseLibraryEvent.OnVideoPicked(exerciseId = exercise.exerciseId, video = picked))
                }
                AppButton(
                    text = when (exercise.video) {
                        is ExerciseVideo.Uploaded -> stringResource(Res.string.exercise_library_video_replace)
                        else -> stringResource(Res.string.exercise_library_video_upload)
                    },
                    onClick = picker::pick,
                    tone = ButtonTone.Text,
                    size = ButtonSize.Small,
                    state = if (exercise.isUploadingVideo) ButtonState.Loading else ButtonState.Idle,
                )
                AppButton(
                    text = stringResource(Res.string.exercise_library_archive_action),
                    onClick = { onEvent(ExerciseLibraryEvent.OnArchiveClicked(exercise.exerciseId)) },
                    tone = ButtonTone.Text,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}
