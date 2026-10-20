package com.mobyle.abbay.infra.ads

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.usecase.ObserveAppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the library banner shows during this run of the app. "Show ads" is read once, at the first start of the
 * main activity: turning it off or on applies the next time the app is opened, as Settings says. With ads on, the
 * consent form is gathered first where the law requires one (EEA, UK); the SDK starts only when ads may be requested.
 */
@Singleton
class AdsSession @Inject constructor(private val observeAppSettings: ObserveAppSettings) {
    private val isStarted = AtomicBoolean(false)
    private val isSdkStarted = AtomicBoolean(false)
    private val _isActive = MutableStateFlow(false)
    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    private var consentInformation: ConsentInformation? = null

    /** The banner may load. */
    val isActive: StateFlow<Boolean> get() = _isActive.asStateFlow()

    /** Settings → Privacy → Ad privacy options: shown where the consent form applies. */
    val isPrivacyOptionsRequired: StateFlow<Boolean> get() = _isPrivacyOptionsRequired.asStateFlow()

    suspend fun start(activity: Activity) {
        if (!isStarted.compareAndSet(false, true)) return
        if (!observeAppSettings().first().showAds) return

        val information = UserMessagingPlatform.getConsentInformation(activity).also { consentInformation = it }
        information.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onConsentGathered(activity, information) }
            },
            { onConsentGathered(activity, information) },
        )
        // Consent from an earlier run: ads may load while the information updates.
        if (information.canRequestAds()) startSdk(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { consentInformation?.let { onConsentGathered(activity, it) } }
    }

    private fun onConsentGathered(activity: Activity, information: ConsentInformation) {
        _isPrivacyOptionsRequired.value =
            information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (information.canRequestAds()) startSdk(activity)
    }

    private fun startSdk(activity: Activity) {
        if (!isSdkStarted.compareAndSet(false, true)) return
        // OPTIMIZE_INITIALIZATION (manifest) keeps this off the main thread.
        MobileAds.initialize(activity.applicationContext) { _isActive.value = true }
    }
}

/** Provided by the main activity; null in previews and tests, where no banner shows. */
val LocalAdsSession = staticCompositionLocalOf<AdsSession?> { null }
