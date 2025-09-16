package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject
private val empty = Post(
    id = 0,
    content = "",
    authorId = 0,
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = 0,
)

private val noPhoto = PhotoModel()

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    auth: AppAuth,
) : ViewModel() {
    private val cached = repository
        .data
        .cachedIn(viewModelScope)

    val data: Flow<PagingData<FeedItem>> = auth
        .authStateFlow
        .flatMapLatest { (myId, _) ->
            cached.map { pagingData ->
                pagingData.map { post ->
                    if (post is Post) {
                        post.copy(ownedByMe = post.authorId == myId)
                    } else {
                        post
                    }
                }
            }
        }

    //   val newerCount = data.switchMap {
    //     repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
    //       .catch { _dataState.postValue(FeedModelState(error = true)) }
    // .catch { e -> e.printStackTrace() }
    //     .asLiveData(Dispatchers.Default)
//    }


    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    private val _refreshState = MutableLiveData(false)
    val refreshState: LiveData<Boolean>
        get() = _refreshState
    private val _refreshResult = SingleLiveEvent<Boolean>()
    val refreshResult: LiveData<Boolean>
        get() = _refreshResult

    init {
        viewModelScope.launch {
            auth.authEvents.collect { event ->
                when (event) {
                    AppAuth.AuthEvent.LOGIN,
                    AppAuth.AuthEvent.LOGOUT -> {
                        repository.invalidatePagingSource()
                    }
                }
            }
        }
    }
    fun refreshPosts() {
        _refreshState.value = true
        viewModelScope.launch {
            try {
                val success = repository.loadNewPosts()
                _refreshResult.value = success
                if (success) {
                    _dataState.value = FeedModelState(true)
                } else {
                    _dataState.value = FeedModelState(true)
                }
            } catch (e: Exception) {
                _dataState.value = FeedModelState(true)
                _refreshResult.value = false
            } finally {
                _refreshState.value = false
            }
        }
    }
    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    repository.save(
                        it, _photo.value?.uri?.let { MediaUpload(it.toFile()) }
                    )
                    _postCreated.value = Unit
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        edited.value = empty
        _photo.value = noPhoto
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun changePhoto(uri: Uri?) {
        _photo.value = PhotoModel(uri)
    }

    private val _singleError = SingleLiveEvent<Unit>()
    val singleError: LiveData<Unit>
        get() = _singleError

    fun likeById(post: Post) {
        viewModelScope.launch {
            _dataState.value = FeedModelState(loading = true)
            try {
                if (post.likedByMe)
                    repository.unlikeById(post.id)
                else repository.likeById(post.id)
                _dataState.value = FeedModelState()
            } catch (e: NetworkError) {
                _dataState.value = FeedModelState(error = true, needRetry = true)
                _singleError.postValue(Unit)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
                _singleError.postValue(Unit)
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            _dataState.value = FeedModelState(loading = true)
            try {
                repository.removeById(id)
                _dataState.value = FeedModelState()
            } catch (e: NetworkError) {
                _dataState.value = FeedModelState(error = true, needRetry = true)
                _singleError.postValue(Unit)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
                _singleError.postValue(Unit)
            }
        }
    }
}