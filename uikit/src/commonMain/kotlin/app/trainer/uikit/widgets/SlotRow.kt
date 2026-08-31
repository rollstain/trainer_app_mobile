package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe

private val ROW_PADDING_VERTICAL = 10.dp
private const val TITLE_MAX_LINES = 2

enum class SlotRowStatus { Free, Booked, Cancelled, Completed }

sealed interface SlotRowNote {

    data object None : SlotRowNote

    data class Text(val value: String) : SlotRowNote
}

sealed interface SlotRowTrailing {

    data class Client(val displayName: String) : SlotRowTrailing

    data class Status(val text: String, val kind: StatusChipKind) : SlotRowTrailing

    data class Seats(val label: String, val state: SeatsState) : SlotRowTrailing
}

@Composable
fun AppSlotRow(
    modifier: Modifier = Modifier,
    timeLabel: String,
    durationLabel: String,
    title: String,
    status: SlotRowStatus,
    trailing: SlotRowTrailing,
    onClick: () -> Unit,
    note: SlotRowNote = SlotRowNote.None,
    participants: List<String> = emptyList(),
    hasRequest: Boolean = false,
    isNext: Boolean = false,
) {
    val colors = AppTheme.colors
    val isDimmed = status == SlotRowStatus.Completed || status == SlotRowStatus.Cancelled
    val titleColor = if (isDimmed) colors.textSecondary else colors.textPrimary
    val background = when {
        status == SlotRowStatus.Cancelled -> colors.bgScreen
        isNext -> colors.accentTint
        else -> colors.bgSurface
    }
    Column(modifier = modifier.fillMaxWidth().background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppTheme.sizing.slotRowMinHeight)
                .then(stripeModifier(status = status, hasRequest = hasRequest))
                .clickable(onClick = onClick)
                .padding(
                    horizontal = AppTheme.spacing.dp16,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.widthIn(min = AppTheme.sizing.slotRowTimeColumnWidth),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                AppText(
                    text = timeLabel,
                    style = if (status == SlotRowStatus.Cancelled) {
                        AppTheme.typography.numeric.copy(textDecoration = TextDecoration.LineThrough)
                    } else {
                        AppTheme.typography.numeric
                    },
                    color = titleColor,
                    maxLines = 1,
                )
                AppText(
                    text = durationLabel,
                    style = AppTheme.typography.overline,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                AppText(
                    text = title,
                    style = AppTheme.typography.bodyStrong,
                    color = titleColor,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                when (note) {
                    SlotRowNote.None -> Unit
                    is SlotRowNote.Text -> AppText(
                        text = note.value,
                        style = AppTheme.typography.numeric,
                        color = colors.accent,
                    )
                }
                AppAvatarStack(names = participants)
            }
            when (trailing) {
                is SlotRowTrailing.Client -> AppAvatar(
                    displayName = trailing.displayName,
                    size = AvatarSize.Small,
                    tone = if (isDimmed) AvatarTone.Neutral else AvatarTone.Active,
                )
                is SlotRowTrailing.Status -> AppStatusChip(
                    text = trailing.text,
                    kind = trailing.kind,
                )
                is SlotRowTrailing.Seats -> AppSeatsChip(
                    label = trailing.label,
                    state = trailing.state,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(colors.border),
        )
    }
}

@Composable
private fun stripeModifier(status: SlotRowStatus, hasRequest: Boolean): Modifier {
    val stripe: Color? = when {
        hasRequest -> AppTheme.colors.warning
        status == SlotRowStatus.Booked -> AppTheme.colors.accent
        else -> null
    }
    if (stripe == null) return Modifier
    return Modifier.leadingStripe(color = stripe, width = AppTheme.borders.accentStripe)
}
