package com.sunion.ikeyconnect.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.sunion.ikeyconnect.data.PreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val preferenceStore: PreferenceStore) :
    ViewModel() {
    private val _showGuide = mutableStateOf(false)
    val showGuide: State<Boolean> = _showGuide

    init {
        _showGuide.value = !preferenceStore.isGuidePopupMenuPressed
    }

    fun setGuideHasBeenSeen() {
        preferenceStore.isGuidePopupMenuPressed = true
        _showGuide.value = false
    }
}