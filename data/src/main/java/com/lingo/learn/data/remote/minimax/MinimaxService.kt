package org.akj.lingo.learn.data.remote.minimax

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MinimaxService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("GroupId") groupId: String?,
        @Body request: MinimaxChatRequest
    ): Response<MinimaxChatResponse>

    @Streaming
    @POST
    suspend fun chatCompletionStream(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("GroupId") groupId: String?,
        @Body request: MinimaxChatRequest
    ): Response<ResponseBody>

    @POST
    suspend fun textToAudio(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("X-Api-Resource-Id") resourceId: String,
        @Query("GroupId") groupId: String?,
        @Body request: MinimaxTtsRequest
    ): Response<ResponseBody>

    @POST
    suspend fun planTextToAudio(
        @Url url: String,
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Api-Resource-Id") resourceId: String,
        @Body request: PlanTtsRequest
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun audioToText(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("X-Api-Resource-Id") resourceId: String,
        @Query("GroupId") groupId: String?,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): Response<MinimaxAsrResponse>
}
