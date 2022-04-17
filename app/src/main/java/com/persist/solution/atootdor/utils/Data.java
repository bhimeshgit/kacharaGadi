package com.persist.solution.atootdor.utils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Data{
    @SerializedName("vehicle_number")
    public String vehicle_number;
    @SerializedName("device_id")
    public String device_id;
    @SerializedName("type")
    public String type;
    @SerializedName("cordinate")
    public ArrayList<String> cordinate;
    @SerializedName("address")
    public String address;
    @SerializedName("city")
    public String city;
    @SerializedName("state")
    public String state;
    @SerializedName("motion_status")
    public String motion_status;
    @SerializedName("speed")
    public double speed;
    @SerializedName("orientation")
    public String orientation;
    @SerializedName("share_link")
    public String share_link;
    @SerializedName("data")
    public Data data;
}