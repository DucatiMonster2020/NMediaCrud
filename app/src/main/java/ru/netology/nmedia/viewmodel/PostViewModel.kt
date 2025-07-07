package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    private val _singleError = SingleLiveEvent<Unit>()
    val singleError: LiveData<Unit>
        get() = _singleError

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)
        repository.getAll(
            object : PostRepository.Callback<List<Post>> {
                override fun onSuccess(data: List<Post>) {
                    _data.postValue(FeedModel(posts = data, empty = data.isEmpty()))
                }
                override fun onError(error: Throwable) {
                    _data.postValue(FeedModel(error = true))
                }
            }
        )
    }

    fun save() {
        edited.value?.let {
            _data.postValue(FeedModel(loading = true))
            repository.save(
                it,
                object : PostRepository.Callback<Post> {
                    override fun onSuccess(data: Post) {
                        _postCreated.postValue(Unit)
                    }

                    override fun onError(error: Throwable) {
                        _singleError.postValue(Unit)
                        _data.postValue(FeedModel(error = true))
                    }
                }
            )
        }
        edited.value = empty
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

    fun likeById(post: Post) {
        val callback = object : PostRepository.Callback<Post> {
            override fun onSuccess(data: Post) {
                val updatedPosts = _data.value?.posts?.map {
                    if (it.id == data.id) {
                        data
                    } else {
                        it
                    }
                }
                _data.postValue(_data.value?.copy(posts = updatedPosts.orEmpty()))
            }
            override fun onError(error: Throwable) {
                _singleError.postValue(Unit)
                _data.postValue(FeedModel(error = true))
            }
        }
        if (post.likedByMe) {
            repository.unlikeById(post.id, callback)
        } else {
            repository.likeById(post.id, callback)
        }
    }

    fun removeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(posts = _data.value?.posts.orEmpty()
                .filter { it.id != id }
            )
        )

        repository.removeById(id, object : PostRepository.Callback<Unit> {
            override fun onSuccess(data: Unit) {}

            override fun onError(error: Throwable) {
                _singleError.postValue(Unit)
                _data.postValue(_data.value?.copy(posts = old))
            }
        })
    }
}
