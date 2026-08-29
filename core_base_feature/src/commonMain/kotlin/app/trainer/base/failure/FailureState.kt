package app.trainer.base.failure

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.failure_conflict_description
import app.trainer.strings.failure_conflict_title
import app.trainer.strings.failure_forbidden_description
import app.trainer.strings.failure_forbidden_title
import app.trainer.strings.failure_gone_description
import app.trainer.strings.failure_gone_title
import app.trainer.strings.failure_network_description
import app.trainer.strings.failure_network_title
import app.trainer.strings.failure_not_found_description
import app.trainer.strings.failure_not_found_title
import app.trainer.strings.failure_parsing_description
import app.trainer.strings.failure_parsing_title
import app.trainer.strings.failure_retry
import app.trainer.strings.failure_server_description
import app.trainer.strings.failure_server_title
import app.trainer.strings.failure_toast
import app.trainer.strings.failure_too_many_requests_description
import app.trainer.strings.failure_too_many_requests_title
import app.trainer.strings.failure_unauthorized_description
import app.trainer.strings.failure_unauthorized_title
import app.trainer.strings.failure_unknown_description
import app.trainer.strings.failure_unknown_title
import app.trainer.strings.failure_validation_description
import app.trainer.strings.failure_validation_title
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

suspend fun RequestResult.Error.toastMessage(): String = userMessage.ifBlank {
    getString(Res.string.failure_toast, getString(titleOf(kind)), getString(descriptionOf(kind)))
}

@Composable
fun AppFailureState(
    modifier: Modifier = Modifier,
    failure: RequestResult.Error,
    onRetry: () -> Unit,
) {
    AppStatePlaceholder(
        modifier = modifier,
        kind = placeholderKindOf(failure.kind),
        title = stringResource(titleOf(failure.kind)),
        description = failure.userMessage.ifBlank { stringResource(descriptionOf(failure.kind)) },
        action = if (isRetryable(failure.kind)) {
            PlaceholderAction.Button(
                text = stringResource(Res.string.failure_retry),
                onClick = onRetry,
            )
        } else {
            PlaceholderAction.None
        },
    )
}

private fun placeholderKindOf(failure: RequestFailure): PlaceholderKind = when (failure) {
    RequestFailure.Forbidden, RequestFailure.Unauthorized -> PlaceholderKind.NoAccess
    RequestFailure.Network,
    RequestFailure.NotFound,
    RequestFailure.Conflict,
    RequestFailure.Gone,
    RequestFailure.TooManyRequests,
    RequestFailure.Validation,
    RequestFailure.Server,
    RequestFailure.Parsing,
    RequestFailure.Unknown,
    -> PlaceholderKind.Failure
}

private fun isRetryable(failure: RequestFailure): Boolean = when (failure) {
    RequestFailure.Forbidden,
    RequestFailure.Unauthorized,
    RequestFailure.Gone,
    RequestFailure.TooManyRequests,
    -> false
    RequestFailure.Network,
    RequestFailure.NotFound,
    RequestFailure.Conflict,
    RequestFailure.Validation,
    RequestFailure.Server,
    RequestFailure.Parsing,
    RequestFailure.Unknown,
    -> true
}

private fun titleOf(failure: RequestFailure): StringResource = when (failure) {
    RequestFailure.Network -> Res.string.failure_network_title
    RequestFailure.Unauthorized -> Res.string.failure_unauthorized_title
    RequestFailure.Forbidden -> Res.string.failure_forbidden_title
    RequestFailure.NotFound -> Res.string.failure_not_found_title
    RequestFailure.Conflict -> Res.string.failure_conflict_title
    RequestFailure.Gone -> Res.string.failure_gone_title
    RequestFailure.TooManyRequests -> Res.string.failure_too_many_requests_title
    RequestFailure.Validation -> Res.string.failure_validation_title
    RequestFailure.Server -> Res.string.failure_server_title
    RequestFailure.Parsing -> Res.string.failure_parsing_title
    RequestFailure.Unknown -> Res.string.failure_unknown_title
}

private fun descriptionOf(failure: RequestFailure): StringResource = when (failure) {
    RequestFailure.Network -> Res.string.failure_network_description
    RequestFailure.Unauthorized -> Res.string.failure_unauthorized_description
    RequestFailure.Forbidden -> Res.string.failure_forbidden_description
    RequestFailure.NotFound -> Res.string.failure_not_found_description
    RequestFailure.Conflict -> Res.string.failure_conflict_description
    RequestFailure.Gone -> Res.string.failure_gone_description
    RequestFailure.TooManyRequests -> Res.string.failure_too_many_requests_description
    RequestFailure.Validation -> Res.string.failure_validation_description
    RequestFailure.Server -> Res.string.failure_server_description
    RequestFailure.Parsing -> Res.string.failure_parsing_description
    RequestFailure.Unknown -> Res.string.failure_unknown_description
}
