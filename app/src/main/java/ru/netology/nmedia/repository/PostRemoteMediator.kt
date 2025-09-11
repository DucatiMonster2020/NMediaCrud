package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val abbDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val result = when (loadType) {
                LoadType.REFRESH -> {
                    val latestId = postDao.getLatestPostId() ?: 0L
                    apiService.getAfter(latestId, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(true)
                    apiService.getBefore(id, state.config.pageSize)
                }

                LoadType.PREPEND ->
                    return MediatorResult.Success(true)
            }
            if (!result.isSuccessful) {
                throw ApiError(result.code(), result.message())
            }
            val body = result.body() ?: throw ApiError(
                result.code(),
                result.message(),
            )

            abbDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (body.isNotEmpty()) {
                            postDao.insert(body.map { PostEntity.fromDto(it) }
                            )
                            val existingKeys = postRemoteKeyDao.min()
                            if (existingKeys == null) {
                                postRemoteKeyDao.insert(
                                    listOf(
                                        PostRemoteKeyEntity(
                                            PostRemoteKeyEntity.KeyType.AFTER,
                                            body.first().id,
                                        ),
                                        PostRemoteKeyEntity(
                                            PostRemoteKeyEntity.KeyType.BEFORE,
                                            body.last().id
                                        )
                                    )
                                )
                            } else {
                                postRemoteKeyDao.insert(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id
                                    )
                                )
                            }
                        }
                    }

                    LoadType.PREPEND -> {}
                    LoadType.APPEND -> {
                        if (body.isNotEmpty()) {
                            postDao.insert(body.map { PostEntity.fromDto(it) })
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    body.last().id
                                )
                            )
                        }
                    }
                }
            }
            MediatorResult.Success(body.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}
