package com.aught.wakawaka.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.aught.wakawaka.data.WakaDataHandler
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ShareButton(
    wakaDataHandler: WakaDataHandler
) {
    var shareDialogOpen by remember {
        mutableStateOf(false)
    }

    IconButton(
        onClick = {}
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = "Share"
        )
    }

    if (shareDialogOpen) {
        Dialog(
            onDismissRequest = {
                shareDialogOpen = false
            }
        ) {
            ShareDialog(wakaDataHandler)
        }
    }
}



@Composable
fun ShareDialog(
    wakaDataHandler: WakaDataHandler
) {
}

