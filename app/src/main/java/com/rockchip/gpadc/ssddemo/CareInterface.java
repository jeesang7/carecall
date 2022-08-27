package com.rockchip.gpadc.ssddemo;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CareInterface {
    @GET("/care/home")
    Call<CareModel> doGetHomeMsg();
}
