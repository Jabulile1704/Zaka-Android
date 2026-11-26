package com.jabulile.zaka.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.jabulile.zaka.core.CsvFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hands a finished export to the system share sheet, once, when one appears.
 *
 * **As a file, not as text.** The iOS app shipped this as raw data first and
 * most share targets simply refused to appear, because anything that attaches
 * a document wants a file. The Android equivalent of that mistake is
 * `EXTRA_TEXT`: it would paste a thousand lines of CSV into an email body.
 * So the CSV is written to the cache and shared as a content URI with a
 * one-shot read grant.
 */
@Composable
fun ShareCsvWhenRequested(
    file: CsvFile?,
    chooserTitle: String,
    onShared: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(file) {
        if (file == null) return@LaunchedEffect
        val uri = withContext(Dispatchers.IO) { write(context, file) }

        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                chooserTitle,
            ),
        )
        onShared()
    }
}

private fun write(context: Context, file: CsvFile) = run {
    val folder = File(context.cacheDir, "exports").apply { mkdirs() }
    // One file per name, overwritten: yesterday's export is not worth keeping
    // in a cache the system may clear at any moment anyway.
    val target = File(folder, file.name)
    target.writeText(file.text)

    FileProvider.getUriForFile(context, "${context.packageName}.exports", target)
}
