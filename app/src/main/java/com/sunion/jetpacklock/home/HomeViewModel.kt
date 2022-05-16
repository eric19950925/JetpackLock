package com.sunion.jetpacklock.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunion.jetpacklock.data.PreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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