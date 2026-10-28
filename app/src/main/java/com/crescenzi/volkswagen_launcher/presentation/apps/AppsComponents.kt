package com.crescenzi.volkswagen_launcher.presentation.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crescenzi.volkswagen_launcher.theme.AppColor
import com.crescenzi.volkswagen_launcher.theme.AppDimens
import com.crescenzi.volkswagen_launcher.widgets.CircleButton

@Composable
internal fun AppCell(
    app: LaunchApp,
    labelAlpha: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.width(AppDimens.AppCircle),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleButton(
            icon = app.icon,
            desc = app.label,
            size = AppDimens.AppCircle,
            iconSize = AppDimens.AppIcon,
            enabled = enabled,
            onClick = onClick
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            color = AppColor.current.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(AppDimens.AppCircle).alpha(labelAlpha)
        )
    }
}
