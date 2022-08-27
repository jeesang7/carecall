package com.rockchip.gpadc.ssddemo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface CareInterface {
    @GET("/care/home")
    Call<CareModel> doGetHomeMsg();

    @POST("/care/emergency")
    Call<CareModel> doPostEmergency(@Body CareModel careModel);
}
