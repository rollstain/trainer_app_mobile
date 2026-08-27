package app.trainer.uikit.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacing(
    val dp4: Dp = 4.dp,
    val dp8: Dp = 8.dp,
    val dp12: Dp = 12.dp,
    val dp16: Dp = 16.dp,
    val dp20: Dp = 20.dp,
    val dp24: Dp = 24.dp,
    val dp32: Dp = 32.dp,
    val dp40: Dp = 40.dp,
)

@Immutable
data class AppRadius(
    val none: Dp = 0.dp,
    val dp4: Dp = 4.dp,
    val dp8: Dp = 8.dp,
    val dp12: Dp = 12.dp,
    val dp16: Dp = 16.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class AppSizing(
    val buttonSmall: Dp = 32.dp,
    val buttonMedium: Dp = 40.dp,
    val buttonLarge: Dp = 48.dp,
    val buttonPaddingSmall: Dp = 12.dp,
    val buttonPaddingMedium: Dp = 16.dp,
    val buttonPaddingLarge: Dp = 20.dp,
    val fieldHeight: Dp = 48.dp,
    val fieldMultilineMinHeight: Dp = 88.dp,
    val inviteCodeFieldHeight: Dp = 56.dp,
    val cellSmall: Dp = 56.dp,
    val cellMedium: Dp = 64.dp,
    val cellLarge: Dp = 76.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val avatarSmall: Dp = 32.dp,
    val avatarMedium: Dp = 40.dp,
    val avatarLarge: Dp = 48.dp,
    val topBarHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 64.dp,
    val minTouchTarget: Dp = 48.dp,
    val chipHeight: Dp = 24.dp,
    val chipPadding: Dp = 10.dp,
    val chipDot: Dp = 6.dp,
    val badgeHeight: Dp = 20.dp,
    val badgeMinWidth: Dp = 20.dp,
    val badgePadding: Dp = 6.dp,
    val badgeDot: Dp = 10.dp,
    val deliveryMarker: Dp = 20.dp,
    val slotCardMinHeight: Dp = 64.dp,
    val slotRowMinHeight: Dp = 64.dp,
    val slotRowTimeColumnWidth: Dp = 48.dp,
    val daySectionHeaderHeight: Dp = 36.dp,
    val complianceCell: Dp = 18.dp,
    val habitWeekCell: Dp = 26.dp,
    val personRowMinHeight: Dp = 80.dp,
    val slotTimeColumnWidth: Dp = 52.dp,
    val timelineHourColumnWidth: Dp = 30.dp,
    val setRowHeight: Dp = 48.dp,
    val setFieldHeight: Dp = 40.dp,
    val setNumberColumnWidth: Dp = 24.dp,
    val weekDayCell: Dp = 56.dp,
    val timelineHourHeight: Dp = 32.dp,
    val sheetHandleWidth: Dp = 36.dp,
    val sheetHandleHeight: Dp = 4.dp,
    val sheetRowHeight: Dp = 48.dp,
    val toastHeight: Dp = 48.dp,
    val placeholderIcon: Dp = 56.dp,
    val placeholderTextMaxWidth: Dp = 280.dp,
    val attachmentPreview: Dp = 56.dp,
    val offlineBannerHeight: Dp = 40.dp,
)

@Immutable
data class AppElevation(
    val dialogBlur: Dp = 24.dp,
    val dialogOffsetY: Dp = 8.dp,
)

@Immutable
data class AppBorders(
    val hairline: Dp = 1.dp,
    val field: Dp = 1.5.dp,
    val focus: Dp = 2.dp,
    val accentStripe: Dp = 3.dp,
    val medicalStripe: Dp = 4.dp,
)
