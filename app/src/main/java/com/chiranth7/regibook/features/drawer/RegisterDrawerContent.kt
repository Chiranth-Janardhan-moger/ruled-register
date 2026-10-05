package com.chiranth7.regibook.features.drawer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiranth7.regibook.R
import com.chiranth7.regibook.util.RegisterType

@Composable
fun RegisterDrawerContent(
    activeRegister: RegisterType,
    onSelectRegister: (RegisterType) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("menu_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.register_books),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = inkColor
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 12.sp
                        ),
                        color = secondaryInk
                    )
                }
            }

            // Divider under header
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                drawLine(
                    color = outlineVariant,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Pigmi register
            DrawerItem(
                title = stringResource(R.string.pigmi_register),
                isSelected = activeRegister == RegisterType.PIGMI,
                icon = Icons.Default.MenuBook,
                onClick = { onSelectRegister(RegisterType.PIGMI) },
                testTag = "drawer_item_pigmi"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Lic
            DrawerItem(
                title = stringResource(R.string.lic_register),
                isSelected = activeRegister == RegisterType.LIC,
                icon = Icons.Default.Description,
                onClick = { onSelectRegister(RegisterType.LIC) },
                testTag = "drawer_item_lic"
            )

            // Push to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Divider above Settings
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                drawLine(
                    color = outlineVariant,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Last at the end: Settings
            DrawerItem(
                title = stringResource(R.string.settings),
                isSelected = false,
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                testTag = "drawer_item_settings"
            )
        }
    }
}

@Composable
private fun DrawerItem(
    title: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val inkColor = MaterialTheme.colorScheme.onBackground
    val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
    val activeBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val activeTextColor = if (isSelected) MaterialTheme.colorScheme.primary else inkColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .background(
                color = activeBg,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else secondaryInk,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 16.sp
            ),
            color = activeTextColor,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
