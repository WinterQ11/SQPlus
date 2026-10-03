package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftIvory
import com.example.ui.viewmodel.Screen

@Composable
fun SqPlusTopBar(
    currentScreen: Screen,
    isAdmin: Boolean,
    title: String? = null,
    onBackClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = ObsidianBlack,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentScreen == Screen.CHANNEL_DETAIL || currentScreen == Screen.ADMIN_DASHBOARD) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .testTag("top_bar_back_button")
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphiteCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoftIvory
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                // SQ Monogram Logo
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphiteCard)
                        .border(1.dp, ChampagneGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SQ",
                        color = ChampagneGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: if (currentScreen == Screen.ADMIN_DASHBOARD) "Admin Console" else "SQPlus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftIvory
                )
                Text(
                    text = if (isAdmin) "Authorized Administrator" else "Private Cloud Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAdmin) ChampagneGold else MutedTaupe
                )
            }

            if (isAdmin && currentScreen != Screen.ADMIN_DASHBOARD) {
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .testTag("top_bar_admin_shortcut")
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, ChampagneGold.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Admin Panel",
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
