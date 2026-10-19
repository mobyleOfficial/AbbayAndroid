package com.mobyle.abbay.infra.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mobyle.abbay.BuildConfig

/**
 * The anchored adaptive banner at the bottom of the library: the only ad in the app (no full-screen ads). Nothing,
 * and no room taken, while ads are off for this run (see [AdsSession]). Ads are never personalized.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val session = LocalAdsSession.current ?: return
    val isActive by session.isActive.collectAsState()
    if (!isActive) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        // A new width (rotation, window resize) needs a banner of the new size.
        key(widthDp) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
                        loadAd(nonPersonalizedRequest())
                    }
                },
                onRelease = AdView::destroy,
            )
        }
    }
}

private fun nonPersonalizedRequest(): AdRequest = AdRequest.Builder()
    .addNetworkExtrasBundle(AdMobAdapter::class.java, bundleOf("npa" to "1"))
    .build()
