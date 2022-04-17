package com.persist.solution.atootdor.utils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;



public class DataResponse{
    @SerializedName("count")
    public int count;
    @SerializedName("data")
    public ArrayList<Data> data;

}





