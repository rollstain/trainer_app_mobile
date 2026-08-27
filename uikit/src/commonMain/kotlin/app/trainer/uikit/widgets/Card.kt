package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.leadingStripe

sealed interface CardAction {

    data object None : CardAction

    data class Click(val onClick: () -> Unit) : CardAction
}

sealed interface CardDecoration {

    data object None : CardDecoration

    data class Stripe(val color: Color) : CardDecoration

    data class DashedOutline(val color: Color) : CardDecoration
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    background: Color = AppTheme.colors.bgSurface,
    action: CardAction = CardAction.None,
    decoration: CardDecoration = CardDecoration.None,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.radius.dp12)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val surface = when (action) {
        CardAction.None -> background
        is CardAction.Click -> if (isPressed) AppTheme.colors.bgSurfaceSunken else background
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = surface, shape = shape)
            .then(decorationModifier(decoration))
            .then(clickModifier(action = action, interactionSource = interactionSource))
            .padding(AppTheme.spacing.dp16),
        content = { content() },
    )
}

@Composable
private fun decorationModifier(decoration: CardDecoration): Modifier = when (decoration) {
    CardDecoration.None -> Modifier
    is CardDecoration.Stripe -> Modifier.leadingStripe(
        color = decoration.color,
        width = AppTheme.borders.medicalStripe,
    )
    is CardDecoration.DashedOutline -> Modifier.dashedBorder(
        color = decoration.color,
        cornerRadius = AppTheme.radius.dp12,
    )
}

@Composable
private fun clickModifier(
    action: CardAction,
    interactionSource: MutableInteractionSource,
): Modifier = when (action) {
    CardAction.None -> Modifier
    is CardAction.Click -> Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Button,
        onClick = action.onClick,
    )
}
