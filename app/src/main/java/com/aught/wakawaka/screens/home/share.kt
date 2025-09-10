package com.aught.wakawaka.screens.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.glance.LocalContext
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.WakaHelpers
import okio.IOException
import java.io.File
import androidx.core.graphics.createBitmap
import java.io.FileOutputStream


// region IMAGE GENERATION
// ? ........................

fun generateImage(
    imageName: String,
    imageSize: Size,
    context: Context,
    drawToCanvas: Canvas.() -> Unit
): Uri? {
    val bitmap = createBitmap(imageSize.width, imageSize.height)
    val canvas = Canvas(bitmap)

    canvas.apply {
        drawToCanvas()
    }

    val imagesDir = File(context.cacheDir, "images")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val file = File(imagesDir, "$imageName.png")
    try {
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun generateCalendarShareImage(context: Context, dateToDurationInSeconds: Map<String, Int>): Uri? {
    val CELL_SIZE = 20f
    val CELL_MARGIN = 8f

    val HEADER_HEIGHT = 50f
    val YEAR_LABEL_HEIGHT = 30f
    val YEAR_LABEL_PADDING = 10f

    val PADDING = 20f

    val MIN_NUM_YEARS = 4

    val numYears = dateToDurationInSeconds.keys.map {
        it.split("-")[0].toIntOrNull() ?: 0
    }.distinct().size.coerceAtLeast(MIN_NUM_YEARS)

    val imageWidth = PADDING * 2 + (CELL_SIZE + CELL_MARGIN) * 53 - CELL_MARGIN
    val imageHeight = (
            PADDING * 2 + HEADER_HEIGHT +
                    (YEAR_LABEL_HEIGHT + YEAR_LABEL_PADDING * 2) * numYears +
                    ((CELL_SIZE + CELL_MARGIN) * 7 - CELL_MARGIN) * numYears
            )


    return generateImage(
        "calendar_share_image",
        Size(800, 200),
        context
    ) {

    }
}

fun generateDailyShareImage(context: Context, dateToDurationInSeconds: Map<String, Int>): Uri? {
    return generateImage(
        "daily_share_image",
        Size(800, 400),
        context
    ) { }
}

// ? ........................
// endregion ........................


// region DIALOG
// ? ........................

@Composable
fun SharePane(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val paneSize = 65
    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(4.dp)
            .clickable(true) {
                onClick()
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // sample image
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier.height(paneSize.dp).fillMaxWidth(0.7f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                modifier = Modifier
            )
        }
        Box(
            modifier = Modifier
                .size(paneSize.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
        }
    }
}

fun shareImage(context: Context, uri: Uri?) {
    if (uri == null) return

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share post to..."))
}

enum class CaptureMode {
    DAILY, CALENDAR, NONE
}

@Composable
fun ShareDialog(
    context: Context,
    selectedProject: String,
    wakaDataHandler: WakaDataHandler
) {
    val isAggregate = remember {
        selectedProject == WakaHelpers.ALL_PROJECTS_ID
    }

    val label = remember {
        if (isAggregate) "aggregate" else selectedProject
    }

    val dateToDurationInSeconds: Map<String, Int> = remember {
        if (isAggregate) {
            wakaDataHandler.aggregateData?.dailyRecords?.mapValues {
                it.value.totalSeconds
            } ?: emptyMap()
        } else {
            wakaDataHandler.projectSpecificData[selectedProject]?.dailyDurationInSeconds ?: emptyMap()
        }
    }

    var captureMode by remember {
        mutableStateOf(CaptureMode.NONE)
    }

    Column(
        modifier= Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier.height(0.dp)
        )
        SharePane(
            "Share Daily Image",
            "Share your current $label coding stats as an image",
        ) {
            captureMode = CaptureMode.DAILY
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.7f),
            color = MaterialTheme.colorScheme.onSurface.copy(0.1f)
        )

        SharePane(
            "Share Calendar Image",
            "Share your $label coding activity calendar as an image",
        ) {
            captureMode = CaptureMode.CALENDAR
        }
        Spacer(
            modifier = Modifier.height(0.dp)
        )
    }

    when (captureMode) {
        CaptureMode.DAILY -> {
            val uri = generateDailyShareImage(
                context, dateToDurationInSeconds
            )
            shareImage(context, uri)
            captureMode = CaptureMode.NONE
        }

        CaptureMode.CALENDAR -> {
            val uri = generateCalendarShareImage(
                context, dateToDurationInSeconds
            )
            shareImage(context, uri)
            captureMode = CaptureMode.NONE
        }

        else -> {}
    }
}

// ? ........................
// endregion ........................


@Composable
fun ShareButton(
    context: Context,
    selectedProject: String,
    wakaDataHandler: WakaDataHandler,
    modifier: Modifier = Modifier
) {

    var shareDialogOpen by remember {
        mutableStateOf(false)
    }

    IconButton(
        onClick = {
            shareDialogOpen = true
        },
        modifier = modifier,
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
            ShareDialog(context, selectedProject, wakaDataHandler)
        }
    }
}




