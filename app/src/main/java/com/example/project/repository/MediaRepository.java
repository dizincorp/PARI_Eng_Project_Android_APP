package com.example.project.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.ParkingPlaceDao;
import com.example.project.database.dao.SpotPhotoDao;
import com.example.project.database.dao.UserDao;
import com.example.project.database.entity.ParkingPlaceEntity;
import com.example.project.database.entity.SpotPhotoEntity;
import com.example.project.database.entity.UserEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.MediaApiService;
import com.example.project.network.Resource;
import com.example.project.network.SpotApiService;
import com.example.project.network.model.MediaUploadResponse;
import com.example.project.network.model.SpotPhotoResponse;
import com.example.project.utils.FileUtils;
import com.example.project.utils.SessionManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating media uploads (spot photos, user avatars) between
 * the Retrofit REST backend API and the local Room Database cache.
 */
public class MediaRepository {

    private static final String TAG = "MediaRepository";
    private static volatile MediaRepository instance;

    private final Context context;
    private final MediaApiService mediaApiService;
    private final SpotApiService spotApiService;
    private final SpotPhotoDao spotPhotoDao;
    private final ParkingPlaceDao parkingPlaceDao;
    private final UserDao userDao;
    private final ExecutorService diskExecutor;

    private MediaRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mediaApiService = ApiClient.getMediaApiService();
        this.spotApiService = ApiClient.getSpotApiService();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.spotPhotoDao = db.spotPhotoDao();
        this.parkingPlaceDao = db.parkingPlaceDao();
        this.userDao = db.userDao();
        this.diskExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Gets singleton MediaRepository instance.
     *
     * @param context Application or Activity context
     * @return Singleton MediaRepository instance
     */
    public static synchronized MediaRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new MediaRepository(context);
        }
        return instance;
    }

    /**
     * Uploads and attaches a photo to a parking spot via POST /api/spots/{id}/photos.
     * Persists the photo entity into Room and updates the spot's primary image URI.
     *
     * @param spotId   Parking spot identifier
     * @param photoUri Content URI of selected image
     * @return LiveData delivering Resource of {@link SpotPhotoResponse}
     */
    public LiveData<Resource<SpotPhotoResponse>> uploadSpotPhoto(@NonNull String spotId, @NonNull Uri photoUri) {
        MutableLiveData<Resource<SpotPhotoResponse>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        diskExecutor.execute(() -> {
            MultipartBody.Part part = FileUtils.prepareImagePart(context, "file", photoUri);
            if (part == null) {
                result.postValue(Resource.error("Nie udało się odczytać pliku zdjęcia", null));
                return;
            }

            spotApiService.uploadSpotPhoto(spotId, part).enqueue(new Callback<SpotPhotoResponse>() {
                @Override
                public void onResponse(@NonNull Call<SpotPhotoResponse> call, @NonNull Response<SpotPhotoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SpotPhotoResponse photoResp = response.body();
                        diskExecutor.execute(() -> {
                            SpotPhotoEntity entity = new SpotPhotoEntity(
                                    photoResp.getId() != null && !photoResp.getId().isEmpty()
                                            ? photoResp.getId() : ("photo_" + System.currentTimeMillis()),
                                    spotId,
                                    photoResp.getStorageKey() != null ? photoResp.getStorageKey() : "",
                                    photoResp.getUrl() != null ? photoResp.getUrl() : "",
                                    photoResp.getPosition()
                            );
                            spotPhotoDao.insert(entity);

                            // Update primary image URI of spot in Room
                            ParkingPlaceEntity place = parkingPlaceDao.getPlaceById(spotId);
                            if (place != null) {
                                String remoteUrl = photoResp.getUrl() != null && !photoResp.getUrl().isEmpty()
                                        ? photoResp.getUrl() : photoResp.getStorageKey();
                                place.setImageUri(remoteUrl);
                                parkingPlaceDao.insertOrUpdate(place);
                            }
                            result.postValue(Resource.success(photoResp));
                        });
                    } else {
                        String msg = "Błąd wysyłania zdjęcia: HTTP " + response.code();
                        Log.e(TAG, msg);
                        result.postValue(Resource.error(msg, null, response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SpotPhotoResponse> call, @NonNull Throwable t) {
                    String msg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                    Log.e(TAG, "uploadSpotPhoto failure: " + msg, t);
                    result.postValue(Resource.error(msg, null, 0));
                }
            });
        });

        return result;
    }

    /**
     * Uploads and updates user avatar via POST /api/users/me/avatar.
     * Persists the remote avatar URL into SessionManager and local Room database.
     *
     * @param avatarUri Content URI of selected avatar image
     * @return LiveData delivering Resource of {@link MediaUploadResponse}
     */
    public LiveData<Resource<MediaUploadResponse>> uploadAvatar(@NonNull Uri avatarUri) {
        MutableLiveData<Resource<MediaUploadResponse>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        diskExecutor.execute(() -> {
            MultipartBody.Part part = FileUtils.prepareImagePart(context, "file", avatarUri);
            if (part == null) {
                result.postValue(Resource.error("Nie udało się odczytać pliku awatara", null));
                return;
            }

            mediaApiService.uploadAvatar(part).enqueue(new Callback<MediaUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<MediaUploadResponse> call, @NonNull Response<MediaUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        MediaUploadResponse uploadResp = response.body();
                        diskExecutor.execute(() -> {
                            String url = uploadResp.getUrl() != null && !uploadResp.getUrl().isEmpty()
                                    ? uploadResp.getUrl() : uploadResp.getStorageKey();

                            SessionManager sm = SessionManager.getInstance(context);
                            if (sm != null) {
                                sm.updateAvatarUri(url);
                            }
                            String currentEmail = sm != null ? sm.getUserEmail() : "";
                            if (!currentEmail.isEmpty()) {
                                UserEntity user = userDao.getUserByEmail(currentEmail);
                                if (user != null) {
                                    user.setAvatarUri(url);
                                    userDao.insertOrUpdate(user);
                                }
                            }
                            result.postValue(Resource.success(uploadResp));
                        });
                    } else {
                        String msg = "Błąd aktualizacji awatara: HTTP " + response.code();
                        Log.e(TAG, msg);
                        result.postValue(Resource.error(msg, null, response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<MediaUploadResponse> call, @NonNull Throwable t) {
                    String msg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                    Log.e(TAG, "uploadAvatar failure: " + msg, t);
                    result.postValue(Resource.error(msg, null, 0));
                }
            });
        });

        return result;
    }

    /**
     * Deletes a photo from a spot via DELETE /api/spots/{id}/photos/{photoId}.
     *
     * @param spotId  Parking spot identifier
     * @param photoId Photo identifier
     * @return LiveData delivering Resource of operation state
     */
    public LiveData<Resource<Void>> deleteSpotPhoto(@NonNull String spotId, @NonNull String photoId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.postValue(Resource.loading(null));

        spotApiService.deleteSpotPhoto(spotId, photoId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    diskExecutor.execute(() -> {
                        spotPhotoDao.deleteById(photoId);
                        result.postValue(Resource.success(null));
                    });
                } else {
                    String msg = "Błąd usuwania zdjęcia: HTTP " + response.code();
                    Log.e(TAG, msg);
                    result.postValue(Resource.error(msg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "deleteSpotPhoto failure: " + msg, t);
                result.postValue(Resource.error(msg, null, 0));
            }
        });

        return result;
    }

    /**
     * Retrieves observable LiveData list of photos for a spot.
     *
     * @param spotId Spot identifier
     * @return LiveData list of {@link SpotPhotoEntity}
     */
    public LiveData<List<SpotPhotoEntity>> getPhotosForSpotLiveData(@NonNull String spotId) {
        return spotPhotoDao.getPhotosForSpotLiveData(spotId);
    }
}
