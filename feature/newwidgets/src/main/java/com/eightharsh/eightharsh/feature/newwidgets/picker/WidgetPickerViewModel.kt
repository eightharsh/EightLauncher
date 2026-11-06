package com.eightharsh.eightharsh.feature.newwidgets.picker

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class WidgetPickerViewModel @Inject constructor(
) : ViewModel() {
    fun logIconError(packageName: String, e: Exception) {
        Log.w("WidgetPicker", "Could not load icon for $packageName", e)
    }
}
