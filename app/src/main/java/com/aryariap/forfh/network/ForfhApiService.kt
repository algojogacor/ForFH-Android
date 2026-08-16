package com.aryariap.forfh.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ForfhApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/schedules")
    suspend fun schedules(): Response<SchedulesResponse>

    @GET("api/tasks")
    suspend fun tasks(): Response<TasksResponse>

    @PUT("api/tasks/{id}")
    suspend fun markDone(@Path("id") id: String, @Body body: MarkDoneRequest): Response<SuccessResponse>

    /** Info kampus + rekap presensi sekaligus (campusData web) — cookie-jar session auth. */
    @GET("api/campus/info")
    suspend fun campusInfo(): Response<KampusInfoEnvelopeDto>
}
