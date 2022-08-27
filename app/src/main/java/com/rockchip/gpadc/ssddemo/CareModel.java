package com.rockchip.gpadc.ssddemo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CareModel {
    @SerializedName("msg")
    @Expose
    private String msg;

    public String getMsg(){
        return msg;
    }
}
