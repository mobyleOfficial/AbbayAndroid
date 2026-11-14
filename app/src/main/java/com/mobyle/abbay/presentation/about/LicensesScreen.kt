package com.mobyle.abbay.presentation.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun LicensesScreen(close: () -> Unit) {
    val context = LocalContext.current
    val notices = remember { LicenseNotices.read(context) }

    AbbayScreen(title = stringResource(R.string.settings_licenses), onClose = close) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            noticeSection(R.string.licenses_bundled, notices.bundled)
            noticeSection(R.string.licenses_assets, notices.assets)
            noticeSection(R.string.licenses_libraries, notices.libraries)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.noticeSection(titleResource: Int, entries: List<LicenseNotice>) {
    if (entries.isEmpty()) return
    item {
        Text(
            stringResource(titleResource),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
    }
    items(entries, key = { "$titleResource/${it.name}" }) { entry -> NoticeRow(entry) }
}

@Composable
private fun NoticeRow(entry: LicenseNotice) {
    val context = LocalContext.current
    val openLabel = stringResource(R.string.licenses_open_license, entry.name)
    val clickModifier = entry.url?.let { url ->
        Modifier
            .clickable(role = Role.Button, onClickLabel = openLabel) { openUrl(context, url) }
    } ?: Modifier
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(entry.name, style = MaterialTheme.typography.bodyLarge)
        Text(entry.details, style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider(thickness = 1.dp)
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

data class LicenseNotice(val name: String, val details: String, val url: String?)

data class LicenseNotices(
    val bundled: List<LicenseNotice> = emptyList(),
    val assets: List<LicenseNotice> = emptyList(),
    val libraries: List<LicenseNotice> = emptyList(),
) {
    companion object {
        private const val ASSET = "licenses.json"

        fun read(context: Context): LicenseNotices = runCatching {
            parse(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        }.getOrDefault(LicenseNotices())

        fun parse(json: String): LicenseNotices {
            val root = JSONObject(json)
            return LicenseNotices(
                bundled = root.optJSONArray("bundled").credits(),
                assets = root.optJSONArray("assets").credits(),
                libraries = root.optJSONArray("libraries").objects().map { library ->
                    val version = library.optString("version")
                    LicenseNotice(
                        name = library.getString("name") + if (version.isNotEmpty()) " $version" else "",
                        details = library.optString("license"),
                        url = library.optString("url").takeIf { it.startsWith("http") },
                    )
                },
            )
        }

        private fun JSONArray?.credits(): List<LicenseNotice> = objects().map { credit ->
            LicenseNotice(
                name = credit.getString("name"),
                details = listOf(credit.optString("source"), credit.optString("license")).filter { it.isNotEmpty() }.joinToString(" · "),
                url = null,
            )
        }

        private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
    }
}
