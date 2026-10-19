package com.mobyle.abbay.infra.common

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.mobyle.abbay.infra.diagnostics.DiagnosticsCollection
import com.mobyle.abbay.infra.language.AppLanguageStore
import com.usecase.ObserveAppSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class AbbayApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var diagnosticsCollection: DiagnosticsCollection

    @Inject
    lateinit var observeAppSettings: ObserveAppSettings

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var loader: ImageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        AppLanguageStore.applyToApplication(this)

        diagnosticsCollection.initialize()
        observeAppSettings()
            .map { it.shareDiagnostics }
            .distinctUntilChanged()
            .onEach(diagnosticsCollection::apply)
            .launchIn(applicationScope)

        loader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    override fun newImageLoader(): ImageLoader = loader
}
