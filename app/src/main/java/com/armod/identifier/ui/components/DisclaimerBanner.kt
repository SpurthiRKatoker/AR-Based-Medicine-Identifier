package com.armod.identifier.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Standard safety disclaimer component per UI/UX Design Brief.
 */
@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Text(
        text = "This is an academic prototype. It may misread packaging. Verify medicine details with a pharmacist or doctor.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
