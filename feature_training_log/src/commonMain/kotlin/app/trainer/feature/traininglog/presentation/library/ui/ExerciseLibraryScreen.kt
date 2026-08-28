package app.trainer.feature.traininglog.presentation.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import app.trainer.strings.exercise_library_create_action
import app.trainer.strings.exercise_library_empty_description
import app.trainer.strings.exercise_library_empty_title
import app.trainer.strings.exercise_library_title
import app.trainer.strings.exercise_library_video_action
import app.trainer.strings.exercise_library_video_replace
import app.trainer.strings.exercise_library_video_upload
import app.trainer.strings.exercise_library_video_uploaded
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmer
import app.trainer.uikit.widgets.AppCardShimmerList
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
}

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
            if (exercise.isOwnedByCoach) {
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
            }
        }
    }
}
