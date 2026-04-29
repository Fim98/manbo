package com.manbo.videoplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentPlayItem(
    fileName: String,
    thumbnailPath: String,
    position: Long,
    duration: Long,
    lastPlayedAt: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbFile = File(thumbnailPath)
        if (thumbFile.exists()) {
            AsyncImage(
                model = thumbFile,
                contentDescription = fileName,
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (duration > 0) {
                LinearProgressIndicator(
                    progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(lastPlayedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(epoch: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epoch))
}
