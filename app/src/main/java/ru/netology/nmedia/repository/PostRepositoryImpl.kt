package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException



class PostRepositoryImpl(
    private val dao: PostDao,
): PostRepository {
    override val data: LiveData<List<Post>> = dao.getAll().map { it.map (PostEntity::toDto) }

    override suspend fun getAll() {
        try {
            val response = PostsApi.retrofitService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.retrofitService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun removeById(id: Long) {
       try {
           dao.removeById(id)
           val response = PostsApi.retrofitService.removeById(id)
           if (!response.isSuccessful) {
               throw ApiError(response.code(), response.message())
           }
       } catch (e: IOException) {
           throw NetworkError
       } catch (e: Exception) {
           getAll()
           throw UnknownError
       }
    }

    override suspend fun likeById(id: Long) {
       try {
           dao.likeById(id)
           val response = PostsApi.retrofitService.likeById(id)
           if (!response.isSuccessful) {
               throw ApiError(response.code(), response.message())
           }
           val body = response.body() ?: throw ApiError(response.code(), response.message())
           val postEntity = PostEntity.fromDto(body)
           dao.insert(postEntity)
       } catch (e: IOException) {
           throw UnknownError
       }
    }

    override suspend fun unlikeById(id: Long) {
        return likeById(id)
    }
}