package com.persist.solution.atootdor.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.persist.solution.atootdor.MapFragment;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.JsonParserVolley;
import com.persist.solution.atootdor.utils.WebUrl;

import org.json.JSONObject;

public class GetDriverLocationWorker extends Worker {
    public static boolean workedStopped = false;

    public GetDriverLocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    void updateLocation() throws InterruptedException {
        while(!workedStopped){
            Thread.sleep(5000);
            MapFragment.LAST_DRIVER_LATITUDE =  MapFragment.DRIVER_LATITUDE;
            MapFragment.LAST_DRIVER_LONGITUDE =  MapFragment.DRIVER_LONGITUDE;
            Log.d("worker", "USER = " +MapFragment.DRIVER_LATITUDE + "  " +MapFragment.DRIVER_LONGITUDE);

            final JsonParserVolley jsonParserVolley = new JsonParserVolley(getApplicationContext());
            jsonParserVolley.addParameter("mobile",  AppSettingSharePref.getInstance(getApplicationContext()).getDriverMobNo() );
            jsonParserVolley.executeRequest(Request.Method.POST, WebUrl.GET_DRIVER_LOCATION,new JsonParserVolley.VolleyCallback() {
                        @Override
                        public void getResponse(String response) throws InterruptedException {
                            try {
                                Log.d("iss","response="+response);
                                JSONObject jsonObject=new JSONObject(response);
                                MapFragment.DRIVER_LATITUDE = Double.parseDouble(jsonObject.getString("lat"));
                                MapFragment.DRIVER_LONGITUDE = Double.parseDouble(jsonObject.getString("lang"));

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

