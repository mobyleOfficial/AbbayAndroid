package com.mobyle.abbay.presentation.converter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.converter.ConversionPlan
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.model.LibraryRoot
import com.settings.AppSettings
import com.usecase.CancelConversion
import com.usecase.GetOpusEncoder
import com.usecase.ObserveAppSettings
import com.usecase.ObserveConversion
import com.usecase.ObserveLibraryRoots
import com.usecase.PlanConversion
import com.usecase.StartConversion
import com.usecase.UpdateAppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The MP3 → Opus converter for one book, or for a library folder chosen on the screen. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConverterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getOpusEncoder: GetOpusEncoder,
    observeConversion: ObserveConversion,
    observeLibraryRoots: ObserveLibraryRoots,
    observeAppSettings: ObserveAppSettings,
    private val planConversion: PlanConversion,
    private val startConversion: StartConversion,
    private val cancelConversion: CancelConversion,
    private val updateAppSettings: UpdateAppSettings,
) : ViewModel() {
    val encoderName: String? = getOpusEncoder()

    private val bookScope = savedStateHandle.get<String>(BOOK_ID_ARGUMENT)?.takeIf(String::isNotEmpty)?.let(ConversionScope::Book)
    private val mutableScope = MutableStateFlow<ConversionScope?>(bookScope)
    val scope: StateFlow<ConversionScope?> = mutableScope.asStateFlow()
    val isBookScope: Boolean = bookScope != null

    private val mutableOptions = MutableStateFlow(ConverterOptions())
    val options: StateFlow<ConverterOptions> = mutableOptions.asStateFlow()

    val roots: StateFlow<List<LibraryRoot>> = observeLibraryRoots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val progress: StateFlow<ConversionProgress> = observeConversion.progress().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ConversionProgress()
    )
    val totalSaved: StateFlow<Long> = observeConversion.totalSaved().stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val settings: StateFlow<AppSettings> = observeAppSettings().stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Null while counting, or when no scope is chosen. */
    val plan: StateFlow<ConversionPlan?> = combine(mutableScope, mutableOptions) { scope, options -> scope to options }
        .flatMapLatest { (scope, options) ->
            flow {
                emit(null)
                scope?.let { emit(planConversion(it, options)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // One library folder: nothing to choose.
        viewModelScope.launch {
            roots.collect { list ->
                if (mutableScope.value == null &&
                    list.size == 1
                ) {
                    mutableScope.value = ConversionScope.Root(list.single().id)
                }
            }
        }
    }

    fun chooseRoot(root: LibraryRoot) {
        mutableScope.value = ConversionScope.Root(root.id)
    }

    fun changeOptions(transform: (ConverterOptions) -> ConverterOptions) {
        mutableOptions.value = transform(mutableOptions.value)
    }

    fun changeSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { updateAppSettings(transform) }

    fun start() {
        val current = mutableScope.value ?: return
        startConversion(current, mutableOptions.value)
    }

    fun cancel() = cancelConversion()

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
        val BATTERY_MINIMUMS = listOf(20, 50, 80)
    }
}
