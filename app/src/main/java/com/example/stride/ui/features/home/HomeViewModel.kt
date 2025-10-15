package com.example.stride.ui.features.home

import androidx.lifecycle.ViewModel
import com.example.stride.data.model.ConnectionState
import com.example.stride.data.repository.GaitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GaitRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

}

