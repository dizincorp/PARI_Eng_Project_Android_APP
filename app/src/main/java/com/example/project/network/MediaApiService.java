package com.example.project.network;

import com.example.project.network.model.MediaUploadResponse;
import com.example.project.network.model.UpdateAvatarRequest;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit REST API interface for general media upload and user avatar management.
 */
public interface MediaApiService {

    /**
     * Uploads an image or file to the server via POST /api/media/upload.
     *
     * @param file Multipart file part
     * @return Call delivering {@link MediaUploadResponse}
     */
    @Multipart
    @POST("api/media/upload")
    Call<MediaUploadResponse> uploadMedia(@Part MultipartBody.Part file);

    /**
     * Uploads and assigns a new profile avatar for the current user via POST /api/users/me/avatar.
     *
     * @param file Multipart avatar image file part
     * @return Call delivering {@link MediaUploadResponse}
     */
    @Multipart
    @POST("api/users/me/avatar")
    Call<MediaUploadResponse> uploadAvatar(@Part MultipartBody.Part file);

    /**
     * Sets user profile avatar using an existing storageKey or URL via POST /api/users/me/avatar.
     *
     * @param request UpdateAvatarRequest payload
     * @return Call delivering {@link MediaUploadResponse}
     */
    @POST("api/users/me/avatar")
    Call<MediaUploadResponse> setAvatar(@Body UpdateAvatarRequest request);
}
