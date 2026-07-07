package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*

interface GoogleDriveApiService {

    @POST
    @FormUrlEncoded
    suspend fun exchangeCode(
        @Url url: String = "https://oauth2.googleapis.com/token",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUri: String
    ): Response<ResponseBody>

    @POST
    @FormUrlEncoded
    suspend fun refreshToken(
        @Url url: String = "https://oauth2.googleapis.com/token",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<ResponseBody>

    @GET("https://www.googleapis.com/oauth2/v2/userinfo")
    suspend fun fetchUserInfo(
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    @GET("https://www.googleapis.com/drive/v3/files")
    suspend fun searchFiles(
        @Header("Authorization") authHeader: String,
        @Query("spaces") spaces: String,
        @Query("orderBy") orderBy: String,
        @Query("q") query: String,
        @Query("fields") fields: String
    ): Response<ResponseBody>

    @POST("https://www.googleapis.com/drive/v3/files")
    suspend fun createFileMetadata(
        @Header("Authorization") authHeader: String,
        @Body metadata: RequestBody
    ): Response<ResponseBody>

    @PATCH
    suspend fun uploadFileMedia(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body content: RequestBody
    ): Response<ResponseBody>

    @GET
    suspend fun downloadFile(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    @PATCH
    suspend fun updateFileMetadata(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body metadata: RequestBody
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteFile(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    companion object {
        fun create(): GoogleDriveApiService {
            return Retrofit.Builder()
                .baseUrl("https://www.googleapis.com/")
                .build()
                .create(GoogleDriveApiService::class.java)
        }
    }
}
