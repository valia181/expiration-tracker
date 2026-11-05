package com.example.expirationtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expirationtracker.domain.model.Item
import com.example.expirationtracker.domain.model.StatusColor
import com.example.expirationtracker.ui.theme.StatusGreenContainer
import com.example.expirationtracker.ui.theme.StatusGreenText
import com.example.expirationtracker.ui.theme.StatusOrangeContainer
import com.example.expirationtracker.ui.theme.StatusOrangeText
import com.example.expirationtracker.ui.theme.StatusRedContainer
import com.example.expirationtracker.ui.theme.StatusRedText

/**
 * Item Card composable displaying a tracked item with:
 * - Dynamic color status indicator badge adhering to WCAG 2.1 AA (REQ-NF-005)
 * - Minimum 48x48 dp touch target for action buttons adhering to Material Design 3 (REQ-NF-006)
 * - Stabilized sub-composition and lambda handling for 60fps list scrolling (REQ-NF-001)
 */
@Composable
fun ItemCard(
    item: Item,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(statusColor = item.statusColor)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Expires: ${item.expirationDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Category: ${item.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!item.storageLocation.isNullOrBlank()) {
                    Text(
                        text = "Location: ${item.storageLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Interactive element with minimum 48x48 dp touch target (REQ-NF-006)
            IconButton(
                onClick = { onDeleteClick(item.id) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${item.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    statusColor: StatusColor,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = remember(statusColor) {
        when (statusColor) {
            StatusColor.RED -> Triple(StatusRedContainer, StatusRedText, "Expired")
            StatusColor.ORANGE -> Triple(StatusOrangeContainer, StatusOrangeText, "Expiring Soon")
            StatusColor.GREEN -> Triple(StatusGreenContainer, StatusGreenText, "Safe")
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
