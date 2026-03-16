package com.twinmind.recorder.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.twinmind.recorder.BuildConfig
import com.twinmind.recorder.data.remote.WhisperApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Standard OkHttp client for Whisper API (multipart file uploads).
     * 120s read timeout because Whisper transcription can be slow.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.HEADERS
            else
                HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideWhisperApi(retrofit: Retrofit): WhisperApi =
        retrofit.create(WhisperApi::class.java)

    /**
     * Separate OkHttp client for GPT-4o Server-Sent Events (SSE) streaming.
     * readTimeout = 0 means INFINITE — mandatory for streaming responses
     * that never "complete" from OkHttp's perspective until the stream closes.
     */
    @Provides
    @Singleton
    @Named("streaming")
    fun provideStreamingOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                    .build()
            )
        }
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
