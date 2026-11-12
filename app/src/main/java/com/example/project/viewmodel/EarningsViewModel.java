package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.entity.OwnerAnalyticsEntity;
import com.example.project.database.entity.OwnerBalanceEntity;
import com.example.project.database.entity.WithdrawalEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.OwnerAnalyticsResponse;
import com.example.project.network.model.WithdrawalResponse;
import com.example.project.repository.OwnerAnalyticsRepository;
import com.example.project.repository.OwnerPayoutRepository;

import java.util.List;

/**
 * ViewModel managing the presentation state for owner financial analytics, KPI metrics,
 * daily revenue breakdowns (GET /api/owner/analytics), and wallet payouts (GET /api/owner/payouts/*).
 * Enforces the Single Source of Truth architecture backed by Room Database.
 */
public class EarningsViewModel extends AndroidViewModel {

    private final OwnerAnalyticsRepository repository;
    private final OwnerPayoutRepository payoutRepository;

    private final LiveData<OwnerAnalyticsEntity> analyticsLiveData;
    private final LiveData<OwnerBalanceEntity> balanceLiveData;
    private final LiveData<List<WithdrawalEntity>> withdrawalsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Resource<WithdrawalResponse>> withdrawalActionLiveData = new MutableLiveData<>();

    /**
     * Parameterized constructor providing application context.
     *
     * @param application Application context
     */
    public EarningsViewModel(@NonNull Application application) {
        super(application);
        this.repository = OwnerAnalyticsRepository.getInstance(application);
        this.payoutRepository = OwnerPayoutRepository.getInstance(application);
        this.analyticsLiveData = repository.getCachedAnalytics();
        this.balanceLiveData = payoutRepository.getCachedBalance();
        this.withdrawalsLiveData = payoutRepository.getCachedWithdrawals();

        // Initial fetch from remote backend
        refreshAnalytics();
    }

    /**
     * Exposes the observed LiveData from Room for owner analytics (SSOT).
     *
     * @return LiveData stream of OwnerAnalyticsEntity
     */
    public LiveData<OwnerAnalyticsEntity> getAnalyticsLiveData() {
        return analyticsLiveData;
    }

    public LiveData<OwnerBalanceEntity> getBalanceLiveData() {
        return balanceLiveData;
    }

    public LiveData<List<WithdrawalEntity>> getWithdrawalsLiveData() {
        return withdrawalsLiveData;
    }

    public LiveData<Resource<WithdrawalResponse>> getWithdrawalActionLiveData() {
        return withdrawalActionLiveData;
    }

    /**
     * Submits an owner withdrawal request via POST /api/owner/payouts/withdraw.
     *
     * @param amount Amount to withdraw
     * @param iban   Bank account IBAN
     */
    public void submitWithdrawal(double amount, @NonNull String iban) {
        withdrawalActionLiveData.setValue(Resource.loading(null));
        LiveData<Resource<WithdrawalResponse>> source = payoutRepository.requestWithdrawal(amount, iban);
        source.observeForever(new androidx.lifecycle.Observer<Resource<WithdrawalResponse>>() {
            @Override
            public void onChanged(Resource<WithdrawalResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    withdrawalActionLiveData.setValue(resource);
                    if (resource.status == Resource.Status.SUCCESS) {
                        payoutRepository.fetchBalance();
                        payoutRepository.fetchWithdrawals();
                    }
                    source.removeObserver(this);
                }
            }
        });
    }

    /**
     * Refreshes analytics data from the remote backend via GET /api/owner/analytics
     * and payout data via GET /api/owner/payouts/*.
     */
    public void refreshAnalytics() {
        isLoadingLiveData.setValue(true);
        payoutRepository.fetchBalance();
        payoutRepository.fetchWithdrawals();
        LiveData<Resource<OwnerAnalyticsResponse>> remoteSource = repository.fetchOwnerAnalytics();
        remoteSource.observeForever(new androidx.lifecycle.Observer<Resource<OwnerAnalyticsResponse>>() {
            @Override
            public void onChanged(Resource<OwnerAnalyticsResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isLoadingLiveData.setValue(false);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    } else {
                        errorMessageLiveData.setValue(null);
                    }
                    remoteSource.removeObserver(this);
                }
            }
        });
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
}
