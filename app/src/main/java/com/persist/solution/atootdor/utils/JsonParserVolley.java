package com.persist.solution.atootdor.utils;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class JsonParserVolley {
    final String contentType = "application/json; charset=utf-8";
    Context context;
    String jsonresponse;

    private Map<String, String> params;

    private Map<String, String> header;

    public JsonParserVolley(Context context) {
        this.context = context;
        params = new HashMap<>();
        header = new HashMap<>();
    }

    public void addParameter(String key, String value) {
        params.put(key, value);
    }

    public void addHeader(String key, String value) {
        header.put(key, value);
    }

    public void executeRequest(int method, final String apiURL, final VolleyCallback callback ) {
        StringRequest stringRequest = new StringRequest(method, apiURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                jsonresponse = response;
                try {
                    callback.getResponse(jsonresponse);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", "error=" + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return header;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getMyInstance(context).addToRequestQueue(stringRequest);
    }

    public interface VolleyCallback
    {
        public void getResponse(String response) throws InterruptedException;
    }
}
