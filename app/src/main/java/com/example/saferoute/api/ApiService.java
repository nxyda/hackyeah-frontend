package com.example.saferoute.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("v1/safe-places/nearby")
    Call<List<SafePlace>> getNearbySafePlaces(
            @Query("longitude") double longitude,
            @Query("latitude") double latitude,
            @Query("radius_m") double radius
    );
}