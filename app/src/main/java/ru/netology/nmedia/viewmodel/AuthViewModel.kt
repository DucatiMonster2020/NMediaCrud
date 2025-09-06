package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AppAuth,
    private val postRepository: PostRepository
) : ViewModel() {
    val data: LiveData<AppAuth.AuthState> = auth
        .authStateFlow
        .asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = auth.authStateFlow.value.id != 0L

    fun signIn(id: Long, token: String) {
        auth.setAuth(id, token)
        postRepository.invalidatePagingSource()
    }
    fun signUp(id: Long, token: String) {
        auth.setAuth(id, token)
        postRepository.invalidatePagingSource()
    }
    fun signOut() {
        auth.removeAuth()
        postRepository.invalidatePagingSource()
    }
}