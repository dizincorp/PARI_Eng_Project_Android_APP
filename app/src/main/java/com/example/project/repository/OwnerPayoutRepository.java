package com.example.project.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.AppDatabase;
import com.example.project.database.dao.OwnerPayoutDao;
import com.example.project.database.entity.OwnerBalanceEntity;
import com.example.project.database.entity.WithdrawalEntity;
import com.example.project.network.ApiClient;
import com.example.project.network.OwnerPayoutApiService;
import com.example.project.network.Resource;
import com.example.project.network.model.OwnerBalanceResponse;
import com.example.project.network.model.WithdrawRequest;
import com.example.project.network.model.WithdrawalResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository managing owner payout balance, withdrawal history, and payout requests.
 * Enforces the Single Source of Truth architecture with Room Database persistent cache.
 */
public class OwnerPayoutRepository {

    private static final String TAG = "OwnerPayoutRepo";
    private static OwnerPayoutRepository instance;

    private final OwnerPayoutApiService apiService;
    private final OwnerPayoutDao dao;
    private final ExecutorService diskExecutor;

    private OwnerPayoutRepository(@NonNull Context context) {
        this.apiService = ApiClient.getOwnerPayoutApiService();
        this.dao = AppDatabase.getInstance(context.getApplicationContext()).ownerPayoutDao();
        this.diskExecutor = Executors.newSingleThreadExecutor();
    }

    public static synchronized OwnerPayoutRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new OwnerPayoutRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Observes the cached owner balance singleton entity from local Room Database.
     *
     * @return LiveData stream of OwnerBalanceEntity
     */
    public LiveData<OwnerBalanceEntity> getCachedBalance() {
        return dao.getBalanceLiveData(OwnerBalanceEntity.SINGLETON_ID);
    }

    /**
     * Observes the cached withdrawal transactions from local Room Database.
     *
     * @return LiveData stream of List of WithdrawalEntity
     */
    public LiveData<List<WithdrawalEntity>> getCachedWithdrawals() {
        return dao.getWithdrawalsLiveData();
    }

    /**
     * Fetches current available balance from remote REST API and saves to Room DB.
     *
     * @return LiveData emitting Resource containing OwnerBalanceResponse
     */
    public LiveData<Resource<OwnerBalanceResponse>> fetchBalance() {
        MutableLiveData<Resource<OwnerBalanceResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getBalance().enqueue(new Callback<OwnerBalanceResponse>() {
            @Override
            public void onResponse(@NonNull Call<OwnerBalanceResponse> call,
                                   @NonNull Response<OwnerBalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OwnerBalanceResponse body = response.body();
                    diskExecutor.execute(() -> {
                        OwnerBalanceEntity entity = new OwnerBalanceEntity(
                                OwnerBalanceEntity.SINGLETON_ID,
                                body.getAvailableBalance(),
                                body.getPendingEscrow(),
                                body.getCurrency(),
                                body.getTotalEarned(),
                                body.getPlatformFeesPaid(),
                                System.currentTimeMillis()
                        );
                        dao.insertOrUpdateBalance(entity);
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = "Błąd pobierania salda: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<OwnerBalanceResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd połączenia z serwerem";
                Log.e(TAG, "fetchBalance failure: " + errorMsg, t);
                resultLiveData.postValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Fetches owner withdrawal transactions from remote REST API and saves to Room DB.
     *
     * @return LiveData emitting Resource containing list of WithdrawalResponse
     */
    public LiveData<Resource<List<WithdrawalResponse>>> fetchWithdrawals() {
        MutableLiveData<Resource<List<WithdrawalResponse>>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        apiService.getWithdrawals().enqueue(new Callback<List<WithdrawalResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<WithdrawalResponse>> call,
                                   @NonNull Response<List<WithdrawalResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WithdrawalResponse> list = response.body();
                    diskExecutor.execute(() -> {
                        List<WithdrawalEntity> entities = new ArrayList<>();
                        for (WithdrawalResponse item : list) {
                            if (item != null && item.getId() != null) {
                                entities.add(new WithdrawalEntity(
                                        item.getId(),
                                        item.getAmount(),
                                        item.getCurrency(),
                                        item.getStatus(),
                                        item.getIban(),
                                        item.getCreatedAt(),
                                        item.getAvailableBalance()
                                ));
                            }
                        }
                        dao.insertWithdrawals(entities);
                        resultLiveData.postValue(Resource.success(list));
                    });
                } else {
                    String errorMsg = "Błąd pobierania historii wypłat: HTTP " + response.code();
                    Log.e(TAG, errorMsg);
                    resultLiveData.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WithdrawalResponse>> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "fetchWithdrawals failure: " + errorMsg, t);
                resultLiveData.postValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }

    /**
     * Submits a withdrawal request to the backend via POST /api/owner/payouts/withdraw
     * and updates local cache.
     *
     * @param amount Withdrawal amount
     * @param iban   Target bank account IBAN
     * @return LiveData emitting Resource containing WithdrawalResponse
     */
    public LiveData<Resource<WithdrawalResponse>> requestWithdrawal(double amount, @NonNull String iban) {
        MutableLiveData<Resource<WithdrawalResponse>> resultLiveData = new MutableLiveData<>();
        resultLiveData.setValue(Resource.loading(null));

        WithdrawRequest request = new WithdrawRequest(amount, iban);
        apiService.withdraw(request).enqueue(new Callback<WithdrawalResponse>() {
            @Override
            public void onResponse(@NonNull Call<WithdrawalResponse> call,
                                   @NonNull Response<WithdrawalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WithdrawalResponse body = response.body();
                    diskExecutor.execute(() -> {
                        WithdrawalEntity entity = new WithdrawalEntity(
                                body.getId(),
                                body.getAmount(),
                                body.getCurrency(),
                                body.getStatus(),
                                body.getIban(),
                                body.getCreatedAt(),
                                body.getAvailableBalance()
                        );
                        dao.insertWithdrawal(entity);

                        // Update current available balance in Room
                        OwnerBalanceEntity balance = dao.getBalanceSync(OwnerBalanceEntity.SINGLETON_ID);
                        if (balance != null) {
                            balance.setAvailableBalance(body.getAvailableBalance());
                            balance.setLastUpdatedTimestamp(System.currentTimeMillis());
                            dao.insertOrUpdateBalance(balance);
                        }
                        resultLiveData.postValue(Resource.success(body));
                    });
                } else {
                    String errorMsg = "Błąd zlecania wypłaty: " + response.code();
                    if (response.code() == 400) {
                        errorMsg = "Kwota wypłaty przekracza dostępne saldo lub numer konta jest niepoprawny.";
                    }
                    Log.e(TAG, errorMsg);
                    resultLiveData.postValue(Resource.error(errorMsg, null, response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<WithdrawalResponse> call, @NonNull Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Błąd sieci";
                Log.e(TAG, "requestWithdrawal failure: " + errorMsg, t);
                resultLiveData.postValue(Resource.error(errorMsg, null, 0));
            }
        });

        return resultLiveData;
    }
}
