package app.trainer.feature.account.coachcard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.account.coachcard.mvi.CoachCardEvent
import app.trainer.feature.account.coachcard.mvi.CoachCardState
import app.trainer.strings.Res
import app.trainer.strings.coach_card_account_section
import app.trainer.strings.coach_card_active_clients
import app.trainer.strings.coach_card_archived_clients
import app.trainer.strings.coach_card_clients_section
import app.trainer.strings.coach_card_contacts_section
import app.trainer.strings.coach_card_email
import app.trainer.strings.coach_card_joined
import app.trainer.strings.coach_card_last_seen
import app.trainer.strings.coach_card_login
import app.trainer.strings.coach_card_never_seen
import app.trainer.strings.coach_card_no_contacts
import app.trainer.strings.coach_card_no_sign_in
import app.trainer.strings.coach_card_password
import app.trainer.strings.coach_card_phone
import app.trainer.strings.coach_card_sign_in_section
import app.trainer.strings.coach_card_title
import app.trainer.strings.coach_card_zone
import app.trainer.strings.coaches_owner_mark
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 3

@Composable
fun CoachCardView(
    modifier: Modifier = Modifier,
    state: CoachCardState,
    onEvent: (CoachCardEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.coach_card_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachCardEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                else -> CoachCardContent(state = state)
            }
        }
    }
}

@Composable
private fun CoachCardContent(state: CoachCardState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        NameCard(state = state)
        FactCard(title = stringResource(Res.string.coach_card_clients_section)) {
            Fact(
                label = stringResource(Res.string.coach_card_active_clients),
                value = state.activeClients.toString(),
            )
            Fact(
                label = stringResource(Res.string.coach_card_archived_clients),
                value = state.archivedClients.toString(),
            )
        }
        FactCard(title = stringResource(Res.string.coach_card_account_section)) {
            Fact(label = stringResource(Res.string.coach_card_joined), value = state.joinedLabel)
            Fact(label = stringResource(Res.string.coach_card_zone), value = state.zoneId)
            Fact(
                label = stringResource(Res.string.coach_card_last_seen),
                value = state.lastSeenLabel ?: stringResource(Res.string.coach_card_never_seen),
            )
        }
        FactCard(title = stringResource(Res.string.coach_card_sign_in_section)) {
            if (state.hasPassword) {
                Line(text = stringResource(Res.string.coach_card_password))
            }
            state.providers.forEach { provider -> Line(text = provider) }
            if (!state.hasSignInMethods) {
                Line(text = stringResource(Res.string.coach_card_no_sign_in))
            }
        }
        FactCard(title = stringResource(Res.string.coach_card_contacts_section)) {
            state.email?.let { Fact(label = stringResource(Res.string.coach_card_email), value = it) }
            state.phone?.let { Fact(label = stringResource(Res.string.coach_card_phone), value = it) }
            state.login?.let { Fact(label = stringResource(Res.string.coach_card_login), value = it) }
            if (!state.hasContacts) {
                Line(text = stringResource(Res.string.coach_card_no_contacts))
            }
        }
    }
}

@Composable
private fun NameCard(state: CoachCardState) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(displayName = state.displayName, size = AvatarSize.Large)
            Column {
                AppText(
                    text = state.displayName,
                    style = AppTheme.typography.headline,
                    color = AppTheme.colors.textPrimary,
                )
                if (state.isOwner) {
                    AppText(
                        text = stringResource(Res.string.coaches_owner_mark),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FactCard(title: String, facts: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = title,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            facts()
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        AppText(
            modifier = Modifier.weight(1f),
            text = label,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = value,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun Line(text: String) {
    AppText(
        text = text,
        style = AppTheme.typography.body,
        color = AppTheme.colors.textPrimary,
    )
}
