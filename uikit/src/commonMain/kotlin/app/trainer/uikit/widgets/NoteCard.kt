package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe

private val CARD_PADDING_VERTICAL = 12.dp
private val CARD_PADDING_HORIZONTAL = 14.dp
private val CONTENT_GAP = 6.dp
private val MEDICAL_BADGE_HEIGHT = 20.dp
private val MEDICAL_BADGE_PADDING = 8.dp
private const val MEDICAL_LABEL = "МЕДИЦИНСКАЯ"
private const val PINNED_LABEL = "ЗАКРЕПЛЕНА"

enum class NoteKindView { Medical, General }

sealed interface NoteDetails {

    data object None : NoteDetails

    data class Text(val value: String) : NoteDetails
}

@Composable
fun AppNoteCard(
    modifier: Modifier = Modifier,
    title: String,
    kind: NoteKindView,
    isPinned: Boolean,
    updatedAt: String,
    onClick: () -> Unit,
    details: NoteDetails = NoteDetails.None,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp12)
    val isMedical = kind == NoteKindView.Medical

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.bgSurface, shape = shape)
            .border(width = AppTheme.borders.hairline, color = colors.border, shape = shape)
            .then(
                if (isMedical) {
                    Modifier.leadingStripe(
                        color = colors.danger,
                        width = AppTheme.borders.medicalStripe,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = CARD_PADDING_HORIZONTAL, vertical = CARD_PADDING_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(CONTENT_GAP),
    ) {
        if (isMedical || isPinned) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isMedical) {
                    Box(
                        modifier = Modifier
                            .height(MEDICAL_BADGE_HEIGHT)
                            .background(
                                color = colors.dangerSoft,
                                shape = RoundedCornerShape(AppTheme.radius.dp4),
                            )
                            .padding(horizontal = MEDICAL_BADGE_PADDING),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = MEDICAL_LABEL,
                            style = AppTheme.typography.overline.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.danger,
                        )
                    }
                }
                if (isPinned) {
                    Text(
                        text = PINNED_LABEL,
                        style = AppTheme.typography.overline,
                        color = if (isMedical) colors.danger else colors.textSecondary,
                    )
                }
            }
        }
        Text(
            text = title,
            style = if (isMedical) AppTheme.typography.bodyStrong else AppTheme.typography.body,
            color = colors.textPrimary,
        )
        when (details) {
            NoteDetails.None -> Unit
            is NoteDetails.Text -> Text(
                text = details.value,
                style = AppTheme.typography.body,
                color = colors.textSecondary,
            )
        }
        Text(
            text = updatedAt,
            style = AppTheme.typography.overline,
            color = colors.textSecondary,
        )
    }
}
