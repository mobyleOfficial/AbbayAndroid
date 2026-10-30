package com.mobyle.abbay.infra.diagnostics

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DiagnosticsCollection @Inject constructor(@ApplicationContext private val context: Context) {
    fun initialize() {
        FirebaseApp.initializeApp(context)
    }

    fun apply(isEnabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isEnabled)
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(isEnabled)
    }
}
