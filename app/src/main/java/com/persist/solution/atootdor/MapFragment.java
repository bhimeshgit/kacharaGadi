package com.persist.solution.atootdor;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.persist.solution.atootdor.service.GetAllVehicleLocationWorker;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.Data;
import com.persist.solution.atootdor.utils.DataResponse;
import com.persist.solution.atootdor.utils.WebUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    boolean locationPermission = false;

    float Bearing = 0;
    boolean AnimationStatus = false;

    Bitmap BitMapMarker;
    private Button startRideBtn;
    private Thread thread;
    private MainActivity mainActivity;
    private List<String> veh_List = new ArrayList<>();
    private boolean isVehList = false;
    public MapFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        // Required empty public constructor
    }

    private ArrayList<Marker> carMarkerList= new ArrayList<>();
    private HashMap<String, Marker> carMap = new HashMap<>();
    private HashMap<String, LatLng> carLatLngMap = new HashMap<>();
    Spinner spino ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.car_marker);
        Bitmap b = bitmapdraw.getBitmap();
        BitMapMarker = Bitmap.createScaledBitmap(b, 110, 60, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        spino = view.findViewById(R.id.vehicle_spinner);
        veh_List.add("All");
        setSpinner();
    }

    private void setSpinner(){

        spino.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(carLatLngMap.containsKey(veh_List.get(i))){
                    for (String vehNo: carMap.keySet()) {
                        if(!vehNo.equals(veh_List.get(i))){
                            carMap.get(vehNo).remove();
                        }
                    }

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                            carLatLngMap.get(veh_List.get(i)), 17f);
                    mMap.animateCamera(cameraUpdate);
                }else{
                    for (String vehNo: carMap.keySet()) {
                        carMap.get(vehNo).remove();
                    }
                    for (String vehNo: carLatLngMap.keySet()) {
                        mMap.addMarker(new MarkerOptions().position(carLatLngMap.get(vehNo)).
                                flat(true).icon(BitmapDescriptorFactory.fromBitmap(BitMapMarker)));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ArrayAdapter ad = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, veh_List);
        ad.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item);
        spino.setAdapter(ad);
    }


    
    float getBearing(double old_longitude,double new_longitude,double old_lat, double new_lat){
        double dLon = (new_longitude-old_longitude);
        double y = Math.sin(dLon) * Math.cos(new_lat);
        double x = Math.cos(old_lat)*Math.sin(new_lat) - Math.sin(old_lat)*Math.cos(new_lat)*Math.cos(dLon);
        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (360 - ((brng + 360) % 360));
        return (float)brng;
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        thread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        if (MapFragment.this.isVisible() && getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    allVehicleTracking();
                                }
                            });
                            AppSettingSharePref.getInstance(getContext()).setOldDeviceList(AppSettingSharePref.getInstance(getContext()).getDeviceList());



                            Thread.sleep(5000);   //1000ms = 1 sec
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();

    }

    private void setVehList(){
        if(!isVehList){
            Set<String> key = carMap.keySet();
            for (String k: key) {
                veh_List.add(k);
            }
            isVehList= true;
            setSpinner();
        }
    }

    void changePositionSmoothly(final Marker myMarker, final LatLng finalPosition, final Float bearing, final LatLng startPosition) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                myMarker.setRotation(bearing);
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                        startPosition.longitude * (1 - t) + finalPosition.longitude * t);

                myMarker.setPosition(currentPosition);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        myMarker.setVisible(false);
                    } else {
                        myMarker.setVisible(true);
                    }
                }

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private  void allVehicleTracking(){
        try {
            if (MapFragment.this.isVisible()) {
//                                            if(rotation_temp == 0){
//                                                AppSettingSharePref.getInstance(getContext()).setDeviceList(WebUrl.RESP1);
//                                                AppSettingSharePref.getInstance(getContext()).setOldDeviceList(WebUrl.RESP1);
//                                            } else{
//                                                AppSettingSharePref.getInstance(getContext()).setOldDeviceList(WebUrl.RESP1);
//                                                AppSettingSharePref.getInstance(getContext()).setDeviceList(WebUrl.RESP2);
//                                            }


                String newData = AppSettingSharePref.getInstance(getContext()).getDeviceList();
                String oldData = AppSettingSharePref.getInstance(getContext()).getOldDeviceList();


                if (newData != null && oldData != null) {
                    DataResponse newDataObj = new Gson().fromJson(newData, DataResponse.class);
                    DataResponse oldDataObj = new Gson().fromJson(oldData, DataResponse.class);
                    if (newDataObj != null) {
                        for (int i = 0; i < newDataObj.count; i++) {
                            Data data = newDataObj.data.get(i).data;
                            double newLat = Double.parseDouble(data.cordinate.get(0));
                            double newLong = Double.parseDouble(data.cordinate.get(1));
                            LatLng newLatLong = new LatLng(newLat, newLong);
                            carLatLngMap.put(data.vehicle_number,newLatLong);
                            for (Data oldDataListObj : oldDataObj.data) {
                                if (data.vehicle_number.equals(oldDataListObj.data.vehicle_number)) {
                                    double oldLat = Double.parseDouble(oldDataListObj.data.cordinate.get(0));
                                    double oldLong = Double.parseDouble(oldDataListObj.data.cordinate.get(1));
                                    LatLng oldLatLong = new LatLng(oldLat, oldLong);
                                    Marker carMarker = null;
                                    if (!carMap.containsKey(oldDataListObj.data.vehicle_number)) {
                                        carMarker = mMap.addMarker(new MarkerOptions().position(newLatLong).
                                                flat(true).icon(BitmapDescriptorFactory.fromBitmap(BitMapMarker)));
                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                                                newLatLong, 17f);
                                        mMap.animateCamera(cameraUpdate);
                                    } else {
                                        carMarker = carMap.get(oldDataListObj.data.vehicle_number);
                                    }
                                    if (!carMap.containsKey(oldDataListObj.data.vehicle_number)) {
                                        carMap.put(oldDataListObj.data.vehicle_number, carMarker);
                                    }
                                    Bearing = getBearing(oldLong, newLong, oldLat, newLat);

//                                                        Log.d("iss", "lst=" + WebUrl.LAST_DRIVER_LATITUDE + " lat=" + WebUrl.DRIVER_LATITUDE);
                                    changePositionSmoothly(carMarker, newLatLong, Bearing, oldLatLong);
                                    break;
                                }
                            }
                        }
                        setVehList();
                    }

                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}