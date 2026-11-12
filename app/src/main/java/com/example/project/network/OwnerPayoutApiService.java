package com.example.project.network;

import com.example.project.network.model.OwnerBalanceResponse;
import com.example.project.network.model.WithdrawRequest;
import com.example.project.network.model.WithdrawalResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit REST API interface for spot owner wallet balance and payout withdrawals.
 */
public interface OwnerPayoutApiService {

    /**
     * Retrieves current available balance, pending escrow, and earnings summary via GET /api/owner/payouts/balance.
     *
     * @return Retrofit Call delivering {@link OwnerBalanceResponse}
     */
    @GET("api/owner/payouts/balance")
    Call<OwnerBalanceResponse> getBalance();

    /**
     * Retrieves full withdrawal history, newest first, via GET /api/owner/payouts/history.
     *
     * @return Retrofit Call delivering a list of {@link WithdrawalResponse}
     */
    @GET("api/owner/payouts/history")
    Call<List<WithdrawalResponse>> getHistory();

    /**
     * Retrieves withdrawal transactions via GET /api/owner/payouts/withdrawals.
     *
     * @return Retrofit Call delivering a list of {@link WithdrawalResponse}
     */
    @GET("api/owner/payouts/withdrawals")
    Call<List<WithdrawalResponse>> getWithdrawals();

    /**
     * Submits a new payout withdrawal request via POST /api/owner/payouts/withdraw.
     *
     * @param request Withdrawal parameters including withdrawal amount and destination IBAN
     * @return Retrofit Call delivering created {@link WithdrawalResponse}
     */
    @POST("api/owner/payouts/withdraw")
    Call<WithdrawalResponse> withdraw(@Body WithdrawRequest request);
}
