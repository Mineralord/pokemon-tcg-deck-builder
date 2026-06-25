package com.mineralord.tcg.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** Puente entre la pantalla de Perfil y el [CloudSyncRepository] del proceso. */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val cloud = (app as TcgApplication).cloudSync

    val authState = cloud.authState
    val syncStatus = cloud.syncStatus
    val lastSynced = cloud.lastSynced

    fun signIn(activity: Activity, launch: (IntentSenderRequest) -> Unit) {
        viewModelScope.launch { cloud.signIn(activity, launch) }
    }

    /** Lo invoca el callback del launcher de consentimiento de Drive. */
    fun onAuthorizationResult(data: Intent?) = cloud.onAuthorizationResult(data)

    fun signOut() {
        viewModelScope.launch { cloud.signOut() }
    }

    fun retry() {
        viewModelScope.launch { cloud.syncNow(preferCloud = false) }
    }
}
