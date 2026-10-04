package com.example.saferoute.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("v1/safe-places/nearby")
    Call<List<SafePlace>> getNearbySafePlaces(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("radius_m") double radius
    );

    @POST("v1/route")
    Call<RouteResponse> createRoute(@Body RouteRequest request);
}