package com.example.project.network;

import com.example.project.network.model.AvailabilityRuleRequest;
import com.example.project.network.model.AvailabilityRuleResponse;
import com.example.project.network.model.BlockedPeriodResponse;
import com.example.project.network.model.BookedSlotResponse;
import com.example.project.network.model.CreateBlockRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit API interface for spot availability rules and calendar blocked periods (F03).
 */
public interface AvailabilityApiService {

    /**
     * Retrieves the list of weekly availability rules for a spot.
     *
     * @param spotId Unique spot identifier
     * @return Call delivering list of {@link AvailabilityRuleResponse}
     */
    @GET("api/spots/{spotId}/availability-rules")
    Call<List<AvailabilityRuleResponse>> getAvailabilityRules(@Path("spotId") String spotId);

    /**
     * Replaces the whole weekly availability schedule for a spot.
     * An empty list means the space is open 24/7.
     *
     * @param spotId Unique spot identifier
     * @param rules  List of new {@link AvailabilityRuleRequest} rules
     * @return Call delivering updated list of {@link AvailabilityRuleResponse}
     */
    @PUT("api/spots/{spotId}/availability-rules")
    Call<List<AvailabilityRuleResponse>> replaceAvailabilityRules(
            @Path("spotId") String spotId,
            @Body List<AvailabilityRuleRequest> rules
    );

    /**
     * Retrieves active blocked periods for a spot via GET /api/spots/{spotId}/blocked-periods.
     *
     * @param spotId Unique spot identifier
     * @return Call delivering list of {@link BlockedPeriodResponse}
     */
    @GET("api/spots/{spotId}/blocked-periods")
    Call<List<BlockedPeriodResponse>> getBlockedPeriods(@Path("spotId") String spotId);

    /**
     * Retrieves calendar blocked blocks for a spot via GET /api/spots/{spotId}/calendar/blocks.
     *
     * @param spotId Unique spot identifier
     * @return Call delivering list of {@link BlockedPeriodResponse}
     */
    @GET("api/spots/{spotId}/calendar/blocks")
    Call<List<BlockedPeriodResponse>> getCalendarBlocks(@Path("spotId") String spotId);

    /**
     * Retrieves occupied intervals (reservations and owner blocked periods)
     * via GET /api/spots/{spotId}/booked-slots?from={from}&amp;to={to}.
     *
     * @param spotId  Unique spot identifier
     * @param fromIso Range start in ISO-8601 (e.g. 2026-11-01T00:00:00Z)
     * @param toIso   Range end in ISO-8601 (e.g. 2026-12-01T23:59:59Z)
     * @return Call delivering list of {@link BookedSlotResponse}
     */
    @GET("api/spots/{spotId}/booked-slots")
    Call<List<BookedSlotResponse>> getBookedSlots(
            @Path("spotId") String spotId,
            @Query("from") String fromIso,
            @Query("to") String toIso
    );

    /**
     * Blocks a period in the spot calendar via POST /api/spots/{spotId}/calendar/blocks.
     *
     * @param spotId  Unique spot identifier
     * @param request Block parameters (start, end, reason)
     * @return Call delivering created {@link BlockedPeriodResponse}
     */
    @POST("api/spots/{spotId}/calendar/blocks")
    Call<BlockedPeriodResponse> createCalendarBlock(
            @Path("spotId") String spotId,
            @Body CreateBlockRequest request
    );

    /**
     * Blocks a period in the spot calendar via POST /api/spots/{spotId}/blocked-periods.
     *
     * @param spotId  Unique spot identifier
     * @param request Block parameters (start, end, reason)
     * @return Call delivering created {@link BlockedPeriodResponse}
     */
    @POST("api/spots/{spotId}/blocked-periods")
    Call<BlockedPeriodResponse> createBlockedPeriod(
            @Path("spotId") String spotId,
            @Body CreateBlockRequest request
    );

    /**
     * Removes/unblocks a blocked period via DELETE /api/spots/{spotId}/calendar/blocks/{periodId}.
     *
     * @param spotId   Unique spot identifier
     * @param periodId Block period identifier
     * @return Call delivering empty 204 No Content response
     */
    @DELETE("api/spots/{spotId}/calendar/blocks/{periodId}")
    Call<Void> deleteCalendarBlock(
            @Path("spotId") String spotId,
            @Path("periodId") String periodId
    );

    /**
     * Removes/unblocks a blocked period via DELETE /api/spots/{spotId}/blocked-periods/{periodId}.
     *
     * @param spotId   Unique spot identifier
     * @param periodId Block period identifier
     * @return Call delivering empty 204 No Content response
     */
    @DELETE("api/spots/{spotId}/blocked-periods/{periodId}")
    Call<Void> deleteBlockedPeriod(
            @Path("spotId") String spotId,
            @Path("periodId") String periodId
    );
}
