package nz.co.mmltonga.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import nz.co.mmltonga.app.MmlApp
import nz.co.mmltonga.app.data.Repository

abstract class BaseViewModel(app: Application) : AndroidViewModel(app) {
    protected val repo: Repository get() = getApplication<MmlApp>().repo

    /**
     * Safety net for every coroutine a screen starts: an unexpected error is logged
     * instead of closing the app. Use as `viewModelScope.launch(safe) { … }`.
     */
    protected val safe = CoroutineExceptionHandler { _, e -> Log.e("MML", "Unexpected error", e) }
}
