package com.persist.solution.atootdor.service;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.google.gson.Gson;
import com.persist.solution.atootdor.MapFragment;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.DataResponse;
import com.persist.solution.atootdor.utils.JsonParserVolley;
import com.persist.solution.atootdor.utils.WebUrl;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetAllVehicleLocationWorker extends Worker {
    public static boolean workedStopped = false;

    public GetAllVehicleLocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    void updateLocation() throws InterruptedException {
        while(!workedStopped){
            Thread.sleep(5000);
            final JsonParserVolley jsonParserVolley = new JsonParserVolley(getApplicationContext());
            jsonParserVolley.addHeader("Authorization",  "Cw87c8ewHiR5AifXsWVW");
            jsonParserVolley.executeRequest(Request.Method.GET, WebUrl.GET_ALL_VEHICLE_LOCATION,new JsonParserVolley.VolleyCallback() {
                    @Override
                    public void getResponse(String response) throws InterruptedException {
                        try {
                            AppSettingSharePref.getInstance(getApplicationContext()).setDeviceList(response);
                            if(AppSettingSharePref.getInstance(getApplicationContext()).getOldDeviceList() == null || AppSettingSharePref.getInstance(getApplicationContext()).getOldDeviceList().equals("") ){
                                AppSettingSharePref.getInstance(getApplicationContext()).setOldDeviceList(AppSettingSharePref.getInstance(getApplicationContext()).getDeviceList());
                            }
                            DataResponse locoNavPojo =   new Gson().fromJson( response, DataResponse.class );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            );
        }

    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            updateLocation();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.success();
    }
}

