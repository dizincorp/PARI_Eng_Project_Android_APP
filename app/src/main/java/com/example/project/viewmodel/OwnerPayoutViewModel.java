package com.example.project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.database.entity.OwnerBalanceEntity;
import com.example.project.database.entity.WithdrawalEntity;
import com.example.project.network.Resource;
import com.example.project.network.model.OwnerBalanceResponse;
import com.example.project.network.model.WithdrawalResponse;
import com.example.project.repository.OwnerPayoutRepository;

import java.util.List;

/**
 * ViewModel managing spot owner wallet balance, payouts history, and withdrawal submissions.
 * Follows MVVM architecture with Room Database as Single Source of Truth.
 */
public class OwnerPayoutViewModel extends AndroidViewModel {

    private final OwnerPayoutRepository repository;
    private final LiveData<OwnerBalanceEntity> balanceLiveData;
    private final LiveData<List<WithdrawalEntity>> withdrawalsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Resource<WithdrawalResponse>> withdrawalActionLiveData = new MutableLiveData<>();

    public OwnerPayoutViewModel(@NonNull Application application) {
        super(application);
        this.repository = OwnerPayoutRepository.getInstance(application);
        this.balanceLiveData = repository.getCachedBalance();
        this.withdrawalsLiveData = repository.getCachedWithdrawals();

        // Initial sync from backend
        refreshPayoutData();
    }

    public LiveData<OwnerBalanceEntity> getBalanceLiveData() {
        return balanceLiveData;
    }

    public LiveData<List<WithdrawalEntity>> getWithdrawalsLiveData() {
        return withdrawalsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<Resource<WithdrawalResponse>> getWithdrawalActionLiveData() {
        return withdrawalActionLiveData;
    }

    /**
     * Refreshes both balance and withdrawal transaction history from REST API.
     */
    public void refreshPayoutData() {
        isLoadingLiveData.setValue(true);
        repository.fetchBalance();
        repository.fetchWithdrawals().observeForever(new androidx.lifecycle.Observer<Resource<List<WithdrawalResponse>>>() {
            @Override
            public void onChanged(Resource<List<WithdrawalResponse>> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    isLoadingLiveData.setValue(false);
                    if (resource.status == Resource.Status.ERROR) {
                        errorMessageLiveData.setValue(resource.message);
                    } else {
                        errorMessageLiveData.setValue(null);
                    }
                }
            }
        });
    }

    /**
     * Submits a withdrawal request via POST /api/owner/payouts/withdraw.
     *
     * @param amount Withdrawal amount in PLN
     * @param iban   Target IBAN string
     */
    public void submitWithdrawal(double amount, @NonNull String iban) {
        withdrawalActionLiveData.setValue(Resource.loading(null));
        LiveData<Resource<WithdrawalResponse>> source = repository.requestWithdrawal(amount, iban);
        source.observeForever(new androidx.lifecycle.Observer<Resource<WithdrawalResponse>>() {
            @Override
            public void onChanged(Resource<WithdrawalResponse> resource) {
                if (resource != null && resource.status != Resource.Status.LOADING) {
                    withdrawalActionLiveData.setValue(resource);
                    if (resource.status == Resource.Status.SUCCESS) {
                        refreshPayoutData();
                    }
                    source.removeObserver(this);
                }
            }
        });
    }
}
