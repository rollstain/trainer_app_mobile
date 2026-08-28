package app.trainer.feature.clientcard.presentation.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleEvent
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleState
import app.trainer.feature.clientcard.presentation.people.mvi.PersonRow
import app.trainer.strings.Res
import app.trainer.strings.people_booked_title
import app.trainer.strings.people_empty_description
import app.trainer.strings.people_empty_title
import app.trainer.strings.people_everyone_title
import app.trainer.strings.people_invite_action
import app.trainer.strings.people_medical_badge
import app.trainer.strings.people_not_booked
import app.trainer.strings.people_others_title
import app.trainer.strings.people_request_mark
import app.trainer.strings.people_search_clear
import app.trainer.strings.people_search_empty_description
import app.trainer.strings.people_search_empty_title
import app.trainer.strings.people_search_found
import app.trainer.strings.people_search_placeholder
import app.trainer.strings.people_search_reset
import app.trainer.strings.people_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppBadge
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppSearchField
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.BadgeValue
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SectionCount
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 5
private const val LOAD_MORE_ROWS = 2
private const val REQUEST_SEPARATOR = " · "

@Composable
fun PeopleView(
    modifier: Modifier = Modifier,
    state: PeopleState,
    onEvent: (PeopleEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = stringResource(Res.string.people_title))
        if (state.isSearchable) {
            AppSearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
                value = state.search,
                placeholder = stringResource(Res.string.people_search_placeholder),
                onValueChange = { onEvent(PeopleEvent.OnSearchChanged(it)) },
                onClear = { onEvent(PeopleEvent.OnSearchChanged("")) },
                clearDescription = stringResource(Res.string.people_search_clear),
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCellShimmerList(count = SHIMMER_ROWS)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(PeopleEvent.OnRetryClicked) },
                )
                state.isEmpty && state.isSearching -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.people_search_empty_title),
                    description = stringResource(Res.string.people_search_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.people_search_reset),
                        onClick = { onEvent(PeopleEvent.OnSearchChanged("")) },
                    ),
                )
                state.isEmpty -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.people_empty_title),
                    description = stringResource(Res.string.people_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.people_invite_action),
                        onClick = { onEvent(PeopleEvent.OnCreateInviteClicked) },
                    ),
                )
                else -> PeopleList(state = state, onEvent = onEvent)
            }
        }
        if (!state.isEmpty && !state.isSearching) {
            AppButton(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                text = stringResource(Res.string.people_invite_action),
                onClick = { onEvent(PeopleEvent.OnCreateInviteClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = if (state.isCreatingInvite) ButtonState.Loading else ButtonState.Idle,
            )
        }
    }
}

@Composable
private fun PeopleList(state: PeopleState, onEvent: (PeopleEvent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.isSearching) {
            item(key = "found") {
                AppSectionHeader(
                    title = stringResource(Res.string.people_search_found, state.others.size),
                )
            }
        }
        if (state.booked.isNotEmpty()) {
            item(key = "booked-title") {
                AppSectionHeader(
                    title = stringResource(Res.string.people_booked_title),
                    count = SectionCount.Value(state.booked.size),
                )
            }
            items(items = state.booked, key = { "booked-${it.userId}" }) { person ->
                PersonCell(
                    modifier = Modifier.animateItem(),
                    person = person,
                    onClick = { onEvent(PeopleEvent.OnPersonClicked(person.userId)) },
                )
            }
        }
        if (state.others.isNotEmpty()) {
            if (!state.isSearching) {
                item(key = "others-title") {
                    AppSectionHeader(
                        title = when {
                            state.booked.isEmpty() -> stringResource(Res.string.people_everyone_title)
                            else -> stringResource(Res.string.people_others_title)
                        },
                        count = if (state.hasMore) SectionCount.None else SectionCount.Value(state.others.size),
                    )
                }
            }
            items(items = state.others, key = { "other-${it.userId}" }) { person ->
                PersonCell(
                    modifier = Modifier.animateItem(),
                    person = person,
                    onClick = { onEvent(PeopleEvent.OnPersonClicked(person.userId)) },
                )
            }
        }
        if (state.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(state.nextCursor) { onEvent(PeopleEvent.OnEndReached) }
                AppCellShimmerList(count = LOAD_MORE_ROWS)
            }
        }
    }
}

@Composable
private fun PersonCell(modifier: Modifier = Modifier, person: PersonRow, onClick: () -> Unit) {
    Column(modifier = modifier.background(AppTheme.colors.bgSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppTheme.sizing.personRowMinHeight)
                .then(
                    if (person.attentionReason == null) {
                        Modifier
                    } else {
                        Modifier.leadingStripe(
                            color = AppTheme.colors.warning,
                            width = AppTheme.borders.medicalStripe,
                        )
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(displayName = person.displayName, size = AvatarSize.Large)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        modifier = Modifier.weight(1f, fill = false),
                        text = person.displayName,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (person.hasMedicalNotes) {
                        MedicalBadge()
                    }
                }
                NextSessionLine(person = person)
                person.attentionReason?.let { reason ->
                    AttentionLine(reason = reason)
                }
            }
            if (person.unreadCount > 0) {
                AppBadge(value = BadgeValue.Count(person.unreadCount))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(AppTheme.colors.border),
        )
    }
}

@Composable
private fun AttentionLine(reason: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizing.attentionDotSize)
                .background(color = AppTheme.colors.warning, shape = CircleShape),
        )
        AppText(
            text = reason,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.warning,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MedicalBadge() {
    Box(
        modifier = Modifier
            .background(
                color = AppTheme.colors.dangerSoft,
                shape = RoundedCornerShape(AppTheme.radius.dp4),
            )
            .padding(horizontal = AppTheme.spacing.dp8, vertical = AppTheme.spacing.dp4),
    ) {
        AppText(
            text = stringResource(Res.string.people_medical_badge),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.danger,
        )
    }
}

@Composable
private fun NextSessionLine(person: PersonRow) {
    val session = person.nextSessionLabel
    if (session == null) {
        AppText(
            text = stringResource(Res.string.people_not_booked),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
        return
    }
    val label = if (person.hasPendingChangeRequest) {
        session + REQUEST_SEPARATOR + stringResource(Res.string.people_request_mark)
    } else {
        session
    }
    AppText(
        text = label,
        style = AppTheme.typography.numeric,
        color = if (person.hasPendingChangeRequest) {
            AppTheme.colors.warning
        } else {
            AppTheme.colors.textSecondary
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
