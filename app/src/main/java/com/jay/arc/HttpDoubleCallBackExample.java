package com.jay.arc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jay.arc.Http.HttpCallback;
import com.jay.arc.Http.HttpInterface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jay on 12-03-2016.
 */
public class HttpDoubleCallBackExample extends Activity {
    private static String TAG = "ARC";
    private static TextView tvJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvJson = (TextView) findViewById(R.id.tvJson);

        getExample();
    }

    public void getExample(){
        //Get Example
        HttpCallback.get(new HttpInterface() {
            @Override
            public void refreshView(String response) {
                refresh(response);
            }

            @Override
            public void noInternet() {
                Toast.makeText(HttpDoubleCallBackExample.this, "No Internet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void error() {
                Toast.makeText(HttpDoubleCallBackExample.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }

        }, "http://192.168.0.200:81/callApi/blazen/test/new");
    }

    public void postExample(){
        //Post Example
        HttpCallback.post(new HttpInterface() {
            @Override
            public void refreshView(String response) {
                refresh(response);
            }

            @Override
            public void noInternet() {
                Toast.makeText(HttpDoubleCallBackExample.this, "No Internet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void error() {
                Toast.makeText(HttpDoubleCallBackExample.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }

        }, "http://192.168.0.126:80/job/test", String.valueOf(getJson("2")));
    }

    public void refresh(String response) {
        tvJson.setText(response);
    }

    public JSONObject getJson(String id) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("_id", id);
        } catch (JSONException je) {
            Log.e(TAG, Log.getStackTraceString(je));
        }
        return jsonObject;
    }
}
