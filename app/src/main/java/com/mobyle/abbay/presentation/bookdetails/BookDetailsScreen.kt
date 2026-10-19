package com.mobyle.abbay.presentation.bookdetails

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.player.chapterTitle
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.BookDetails
import com.model.BookOverrides
import com.model.CoverCandidate

/** Book details: metadata overrides, cover, track order, chapters and unplayable files. */
@Composable
fun BookDetailsScreen(viewModel: BookDetailsViewModel = hiltViewModel(), onConvert: ((bookId: String) -> Unit)? = null, close: () -> Unit) {
    val details by viewModel.details.collectAsState()
    val candidates by viewModel.coverCandidates.collectAsState()

    AbbayScreen(title = stringResource(R.string.details_title), onClose = close) {
        details?.let { bookDetails ->
            BookDetailsContent(
                details = bookDetails,
                coverCandidates = candidates,
                onSave = viewModel::saveOverrides,
                onSortByTrackNumber = viewModel::changeSortByTrackNumber,
                onOpenCoverChooser = viewModel::loadCoverCandidates,
                onChooseCover = viewModel::chooseCover,
                onSearchWeb = viewModel::prepareWebCoverSearch,
                onConvert = onConvert?.let { convert -> { convert(bookDetails.id) } },
            )
        }
    }
}

@Composable
fun BookDetailsContent(
    details: BookDetails,
    coverCandidates: List<CoverCandidate>,
    onSave: (BookOverrides) -> Unit,
    onSortByTrackNumber: (Boolean) -> Unit,
    onOpenCoverChooser: () -> Unit,
    onChooseCover: (String) -> Unit,
    onSearchWeb: () -> Unit,
    onConvert: (() -> Unit)? = null,
) {
    var showCoverChooser by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            DetailsCover(details.coverUri) {
                onOpenCoverChooser()
                showCoverChooser = true
            }
        }
        item { MetadataEditor(details, onSave) }
        // Only books with MP3 files and a converter-capable device.
        onConvert?.let { convert -> item { AbbayTextButton(onClick = convert) { Text(stringResource(R.string.details_convert)) } } }
        fileSections(details, onSortByTrackNumber)
    }

    if (showCoverChooser) {
        CoverChooser(
            bookTitle = details.title,
            candidates = coverCandidates,
            onChoose = { uri ->
                showCoverChooser = false
                onChooseCover(uri)
            },
            onSearchWeb = {
                showCoverChooser = false
                onSearchWeb()
            },
            onDismiss = { showCoverChooser = false },
        )
    }
}

@Composable
private fun DetailsCover(coverUri: String?, onClick: () -> Unit) {
    val coverLabel = stringResource(R.string.details_cover)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(coverUri)
            // Exact size: otherwise Coil's memory cache may hand back the small decode of the list thumbnail.
            .precision(Precision.EXACT)
            .fallback(R.drawable.file_music).error(R.drawable.file_music).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        // The fallback icon follows the theme (it was white on the light theme).
        colorFilter = if (coverUri == null) ColorFilter.tint(AbbayColors.content) else null,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .size(COVER_SIZE.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = coverLabel }
            .clickable(onClick = onClick),
    )
}

/** Title, author, narrator and description; saved as overrides, audio files stay untouched. */
@Composable
private fun MetadataEditor(details: BookDetails, onSave: (BookOverrides) -> Unit) {
    var title by rememberSaveable(details.id) { mutableStateOf(details.title) }
    var author by rememberSaveable(details.id) { mutableStateOf(details.author.orEmpty()) }
    var narrator by rememberSaveable(details.id) { mutableStateOf(details.narrator.orEmpty()) }
    var description by rememberSaveable(details.id) { mutableStateOf(details.description.orEmpty()) }

    Column {
        DetailsField(R.string.details_title_field, title) { title = it }
        DetailsField(R.string.details_author, author) { author = it }
        DetailsField(R.string.details_narrator, narrator) { narrator = it }
        DetailsField(R.string.details_description, description, singleLine = false) { description = it }
        AbbayTextButton(onClick = { onSave(BookOverrides(title, author, narrator, description)) }) {
            Text(stringResource(R.string.details_save), color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/** Duration, files, track order, unplayable files and chapters. */
private fun LazyListScope.fileSections(details: BookDetails, onSortByTrackNumber: (Boolean) -> Unit) {
    item { InfoRow(R.string.details_duration, details.durationMs.toHHMMSS()) }
    item { InfoRow(R.string.details_files, details.fileCount.toString()) }
    item { InfoRow(R.string.details_location, details.location) }
    if (details.fileCount > 1) {
        item { SortByTrackNumberRow(details.sortByTrackNumber, onSortByTrackNumber) }
    }
    if (details.unplayableFiles.isNotEmpty()) {
        item { SectionTitle(R.string.details_unplayable) }
        items(details.unplayableFiles) { file ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                if (file.isUnsupportedFormat) {
                    Text(stringResource(R.string.details_unsupported_format), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (details.chapters.isNotEmpty()) {
        item { SectionTitle(R.string.details_chapters) }
        items(details.chapters) { chapter ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${chapter.index + 1}. ${chapterTitle(chapter)}", modifier = Modifier.weight(1f))
                Text(chapter.startMs.toHHMMSS(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SortByTrackNumberRow(isEnabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.details_sort_by_track_number), modifier = Modifier.weight(1f))
        Switch(
            checked = isEnabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}

/** Folder images, the embedded picture, the photo picker, or a browser search shared back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverChooser(
    bookTitle: String,
    candidates: List<CoverCandidate>,
    onChoose: (String) -> Unit,
    onSearchWeb: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val searchQuery = stringResource(R.string.details_cover_search_query, bookTitle)
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onChoose(it.toString()) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.details_cover_sheet_title), style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                items(candidates) { candidate ->
                    val label = if (candidate.isEmbedded) {
                        stringResource(
                            R.string.details_cover_embedded,
                            candidate.name
                        )
                    } else {
                        candidate.name
                    }
                    AsyncImage(
                        model = candidate.uri,
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(CANDIDATE_SIZE.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onChoose(candidate.uri) },
                    )
                }
            }
            AbbayTextButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(stringResource(R.string.details_cover_from_gallery))
            }
            AbbayTextButton(onClick = {
                onSearchWeb()
                val searchUri = Uri.parse("https://www.google.com/search?tbm=isch&q=" + Uri.encode(searchQuery))
                context.startActivity(Intent(Intent.ACTION_VIEW, searchUri))
            }) {
                Text(stringResource(R.string.details_cover_search_web))
            }
        }
    }
}

@Composable
private fun DetailsField(labelResource: Int, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelResource)) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun InfoRow(labelResource: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelResource), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun SectionTitle(labelResource: Int) {
    Text(
        stringResource(labelResource),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

private const val COVER_SIZE = 180
private const val CANDIDATE_SIZE = 96
