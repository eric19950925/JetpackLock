package com.sunion.ikeyconnect.add_lock.installation

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.sunion.ikeyconnect.R

@HiltViewModel
class InstallationInstructionsViewModel @Inject constructor(application: Application) :
    ViewModel() {
    private val instructionsList = listOf(
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_1,
            descResId = R.string.install_demo_content_1,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_01}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_2,
            descResId = R.string.install_demo_content_2,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_02}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_3,
            descResId = R.string.install_demo_content_3,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_03}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_4,
            descResId = R.string.install_demo_content_4,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_04}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_5,
            descResId = R.string.install_demo_content_5,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_05}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_6,
            descResId = R.string.install_demo_content_6,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_06}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_7,
            descResId = R.string.install_demo_content_7,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_07}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_8,
            descResId = R.string.install_demo_content_8,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_08}"
        ),
        Instructions(
            titleResId = R.string.toolbar_title_lock_install_demo_9,
            descResId = R.string.install_demo_content_9,
            videoPath = "android.resource://${application.packageName}/${R.raw.installation_instructions_09}"
        )
    )

    private var currentPosition = 0

    private val _uiState =
        MutableStateFlow(InstallationInstructionsUiState(instructions = instructionsList[currentPosition]))
    val uiState: StateFlow<InstallationInstructionsUiState> = _uiState

    fun previous() {
        if (currentPosition == 0)
            return

        currentPosition -= 1
        _uiState.update {
            it.copy(
                step = (currentPosition + 1).toString(),
                instructions = instructionsList[currentPosition],
                canGoPrevious = currentPosition > 0
            )
        }
    }

    fun next() {
        if (currentPosition == 8) {
            _uiState.update { it.copy(isLastStep = true) }
            return
        }

        currentPosition += 1
        _uiState.update {
            it.copy(
                step = (currentPosition + 1).toString(),
                instructions = instructionsList[currentPosition],
                canGoPrevious = currentPosition < instructionsList.lastIndex
            )
        }
    }
}

data class InstallationInstructionsUiState(
    val step: String = "1",
    val instructions: Instructions,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val isLastStep: Boolean = false
)

data class Instructions(
    @StringRes val titleResId: Int,
    @StringRes val descResId: Int,
    val videoPath: String
)