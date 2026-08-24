package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.leadingStripe

private val CARD_PADDING_VERTICAL = 12.dp
private val CARD_PADDING_HORIZONTAL = 14.dp
private const val COMPLETED_MARK = "✓"

enum class SlotStatusView { Free, Booked, Cancelled, Completed }

sealed interface SlotRequestView {

    data object None : SlotRequestView

    data class Reschedule(val proposedTime: String, val onApprove: () -> Unit, val onReject: () -> Unit) :
        SlotRequestView

    data class Cancel(val onApprove: () -> Unit, val onReject: () -> Unit) : SlotRequestView
}

sealed interface SlotClientView {

    data object Nobody : SlotClientView

    data class Booked(val displayName: String) : SlotClientView
}

@Composable
fun AppCoachSlotCard(
    modifier: Modifier = Modifier,
    time: String,
    duration: String,
    status: SlotStatusView,
    client: SlotClientView,
    request: SlotRequestView = SlotRequestView.None,
) {
    val colors = AppTheme.colors
    val hasRequest = request != SlotRequestView.None
    val isCancelRequest = request is SlotRequestView.Cancel
    val requestTone = if (isCancelRequest) colors.danger else colors.warning

    SlotCardShell(
        modifier = modifier,
        status = status,
        hasRequest = hasRequest,
        requestTone = requestTone,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlotTime(time = time, duration = duration, status = status)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                when (client) {
                    SlotClientView.Nobody -> Text(
                        text = "Свободно",
                        style = AppTheme.typography.body,
                        color = colors.textSecondary,
                    )
                    is SlotClientView.Booked -> Text(
                        text = client.displayName,
                        style = AppTheme.typography.bodyStrong,
                        color = colors.textPrimary,
                    )
                }
                if (status == SlotStatusView.Completed) {
                    Text(
                        text = "$COMPLETED_MARK Проведена",
                        style = AppTheme.typography.caption,
                        color = colors.success,
                    )
                }
                when (request) {
                    SlotRequestView.None -> Unit
                    is SlotRequestView.Reschedule -> Text(
                        text = "Просит перенос на ${request.proposedTime}",
                        style = AppTheme.typography.caption,
                        color = requestTone,
                    )
                    is SlotRequestView.Cancel -> Text(
                        text = "Просит отменить тренировку",
                        style = AppTheme.typography.caption,
                        color = requestTone,
                    )
                }
            }
            if (client is SlotClientView.Booked && status != SlotStatusView.Cancelled) {
                AppAvatar(
                    displayName = client.displayName,
                    size = AvatarSize.Small,
                    tone = AvatarTone.Neutral,
                )
            }
        }
        RequestActions(request = request)
    }
}

@Composable
fun AppClientSlotCard(
    modifier: Modifier = Modifier,
    time: String,
    duration: String,
    availability: ClientSlotAvailability,
    action: ClientSlotAction,
    request: SlotRequestView = SlotRequestView.None,
    note: String = "",
) {
    val colors = AppTheme.colors
    val hasPendingRequest = request != SlotRequestView.None
    val shape = RoundedCornerShape(AppTheme.radius.dp12)

    val background = when {
        hasPendingRequest -> colors.warningSoft
        availability == ClientSlotAvailability.Mine -> colors.accentSoft
        availability == ClientSlotAvailability.TakenBySomeone -> colors.bgSlotUnavailable
        else -> colors.bgSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.sizing.slotCardMinHeight)
            .background(color = background, shape = shape)
            .then(
                when {
                    hasPendingRequest -> Modifier.dashedBorder(
                        color = colors.warning,
                        cornerRadius = AppTheme.radius.dp12,
                    )
                    availability == ClientSlotAvailability.Mine -> Modifier.border(
                        width = AppTheme.borders.hairline,
                        color = colors.accent,
                        shape = shape,
                    )
                    availability == ClientSlotAvailability.Free -> Modifier.border(
                        width = AppTheme.borders.hairline,
                        color = colors.border,
                        shape = shape,
                    )
                    else -> Modifier
                }
            )
            .padding(horizontal = CARD_PADDING_HORIZONTAL, vertical = CARD_PADDING_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlotTime(time = time, duration = duration, status = SlotStatusView.Free)
            Text(
                modifier = Modifier.weight(1f),
                text = when (availability) {
                    ClientSlotAvailability.Free -> "Свободно"
                    ClientSlotAvailability.TakenBySomeone -> "Занято"
                    ClientSlotAvailability.Mine -> "Моя тренировка"
                },
                style = AppTheme.typography.body,
                color = when (availability) {
                    ClientSlotAvailability.Mine -> colors.textPrimary
                    ClientSlotAvailability.TakenBySomeone -> colors.textMuted
                    ClientSlotAvailability.Free -> colors.textSecondary
                },
            )
            ClientSlotActionButton(action = action)
        }
        if (note.isNotEmpty()) {
            Text(
                text = note,
                style = AppTheme.typography.caption,
                color = colors.textMuted,
            )
        }
        if (hasPendingRequest) {
            Text(
                text = "Заявка отправлена, ждём ответа тренера",
                style = AppTheme.typography.caption,
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun ClientSlotActionButton(action: ClientSlotAction) {
    when (action) {
        ClientSlotAction.None -> Unit
        is ClientSlotAction.Book -> AppButton(
            text = "Записаться",
            onClick = action.onClick,
            tone = ButtonTone.Primary,
            size = ButtonSize.Small,
        )
        is ClientSlotAction.Cancel -> AppButton(
            text = "Отменить",
            onClick = action.onClick,
            tone = ButtonTone.Text,
            size = ButtonSize.Small,
        )
        is ClientSlotAction.JoinWaitlist -> AppButton(
            text = "Ждать",
            onClick = action.onClick,
            tone = ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
        is ClientSlotAction.LeaveWaitlist -> AppButton(
            text = "Не ждать",
            onClick = action.onClick,
            tone = ButtonTone.Text,
            size = ButtonSize.Small,
        )
    }
}

sealed interface ClientSlotAction {

    data object None : ClientSlotAction

    data class Book(val onClick: () -> Unit) : ClientSlotAction

    data class Cancel(val onClick: () -> Unit) : ClientSlotAction

    data class JoinWaitlist(val onClick: () -> Unit) : ClientSlotAction

    data class LeaveWaitlist(val onClick: () -> Unit) : ClientSlotAction
}

enum class ClientSlotAvailability { Free, TakenBySomeone, Mine }

@Composable
private fun SlotCardShell(
    modifier: Modifier,
    status: SlotStatusView,
    hasRequest: Boolean,
    requestTone: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp12)
    val background = when {
        hasRequest -> if (requestTone == colors.danger) colors.dangerSoft else colors.warningSoft
        status == SlotStatusView.Cancelled -> colors.bgScreen
        else -> colors.bgSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.sizing.slotCardMinHeight)
            .background(color = background, shape = shape)
            .then(
                when {
                    hasRequest -> Modifier.dashedBorder(
                        color = requestTone,
                        cornerRadius = AppTheme.radius.dp12,
                    )
                    status == SlotStatusView.Booked -> Modifier.leadingStripe(
                        color = colors.accent,
                        width = AppTheme.borders.accentStripe,
                    )
                    status == SlotStatusView.Free -> Modifier.border(
                        width = AppTheme.borders.hairline,
                        color = colors.border,
                        shape = shape,
                    )
                    else -> Modifier
                }
            )
            .padding(horizontal = CARD_PADDING_HORIZONTAL, vertical = CARD_PADDING_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        content = content,
    )
}

@Composable
private fun SlotTime(time: String, duration: String, status: SlotStatusView) {
    Column(modifier = Modifier.width(AppTheme.sizing.slotTimeColumnWidth)) {
        Text(
            text = time,
            style = AppTheme.typography.numeric,
            color = if (status == SlotStatusView.Cancelled) {
                AppTheme.colors.textMuted
            } else {
                AppTheme.colors.textPrimary
            },
            textDecoration = if (status == SlotStatusView.Cancelled) TextDecoration.LineThrough else null,
        )
        Text(
            text = duration,
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textMuted,
        )
    }
}

@Composable
private fun RequestActions(request: SlotRequestView) {
    val actions = when (request) {
        SlotRequestView.None -> return
        is SlotRequestView.Reschedule -> request.onApprove to request.onReject
        is SlotRequestView.Cancel -> request.onApprove to request.onReject
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        AppButton(
            modifier = Modifier.weight(1f),
            text = "Подтвердить",
            onClick = actions.first,
            tone = ButtonTone.Primary,
            size = ButtonSize.Small,
        )
        AppButton(
            modifier = Modifier.weight(1f),
            text = "Отклонить",
            onClick = actions.second,
            tone = ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
    }
}
