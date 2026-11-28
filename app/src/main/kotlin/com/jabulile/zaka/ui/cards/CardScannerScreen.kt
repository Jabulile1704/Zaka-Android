package com.jabulile.zaka.ui.cards

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jabulile.zaka.R
import com.jabulile.zaka.core.BarcodeReader
import com.jabulile.zaka.core.Scanned
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads a card's number off its barcode.
 *
 * Three states, and the two that are not "here is a viewfinder" matter more:
 * a device with no camera says so, and a refused permission says so. Neither
 * blocks anything — every card can be typed in, which is why the permission
 * is asked for here rather than at launch.
 */
@Composable
fun CardScannerScreen(
    onScanned: (Scanned) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var refused by remember { mutableStateOf(false) }

    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        refused = !allowed
    }

    LaunchedEffect(hasCamera, granted) {
        if (hasCamera && !granted && !refused) ask.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            !hasCamera -> Explain(
                message = stringResource(R.string.scan_no_camera),
                onCancel = onCancel,
            )

            !granted -> Explain(
                message = stringResource(R.string.scan_no_permission),
                onCancel = onCancel,
                retry = if (refused) {
                    { refused = false }
                } else {
                    null
                },
            )

            else -> Viewfinder(onScanned = onScanned, onCancel = onCancel)
        }
    }
}

@Composable
private fun Viewfinder(
    onScanned: (Scanned) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // One scan per visit. Without this the analyser keeps firing while the
    // screen is on its way out and the field is filled several times over.
    val alreadyScanned = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { viewContext ->
                PreviewView(viewContext).also { view ->
                    val future = ProcessCameraProvider.getInstance(viewContext)
                    future.addListener(
                        {
                            val provider = future.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(view.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
                                )
                                .build()

                            analysis.setAnalyzer(executor) { frame ->
                                frame.scan()?.let { scanned ->
                                    if (alreadyScanned.compareAndSet(false, true)) {
                                        ContextCompat.getMainExecutor(viewContext).execute {
                                            onScanned(scanned)
                                        }
                                    }
                                }
                            }

                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        },
                        ContextCompat.getMainExecutor(viewContext),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.scan_type_instead))
            }
        }
    }
}

@Composable
private fun Explain(
    message: String,
    onCancel: () -> Unit,
    retry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (retry != null) {
            Button(onClick = retry) { Text(stringResource(R.string.scan_try_again)) }
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.scan_type_instead))
        }
    }
}

/**
 * The Y plane of a camera frame is already a plane of luminance bytes, so it
 * goes straight to the reader with no bitmap in between. [rowStride] is passed
 * because cameras pad rows out to an alignment boundary.
 */
private fun ImageProxy.scan(): Scanned? = try {
    val plane = planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    BarcodeReader.readAnyOrientation(bytes, width, height, plane.rowStride)
} finally {
    close()
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
