package com.ephemeral.artify

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MLExecutionViewModel : ViewModel() {

    private val _styledBitmap = MutableLiveData<ModelExecutionResult>()

    val styledBitmap: LiveData<ModelExecutionResult>
        get() = _styledBitmap

    private val viewModelJob = Job()
    private val viewModelScope = CoroutineScope(viewModelJob)

    fun onApplyStyle(
        context: Context,
        contentFilePath: String,
        styleFilePath: String,
        styleTransferModelExecutor: StyleTransferModelExecutor,
        inferenceThread: ExecutorCoroutineDispatcher,
        exif: ExifInterface,
        bitmap: Bitmap,
        bitmap_style: Bitmap?
    ) {
        viewModelScope.launch(inferenceThread) {
            val result =
                styleTransferModelExecutor.execute(contentFilePath, styleFilePath, context,exif,bitmap,bitmap_style)
            _styledBitmap.postValue(result)
        }
    }
}
