package com.jay.arc;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jay.arc.HttpCall.HttpCall;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "ARC";
    private TextView tvJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvJson = (TextView) findViewById(R.id.tvJson);
    }

    public void postApi(View view) {
        final String url = "http://api.noteshareapp.com/notification/countNoti";
        final String json = String.valueOf(getJson("56b19d042f9f01aa296810de"));
        final String[] response = new String[1];

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                response[0] = HttpCall.getDataPost(url, json);

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (!response[0].equals("")) {
                    tvJson.setText(response[0]);
                } else {
                    tvJson.setText("No response found");
                }
            }
        }.execute(null, null, null);
    }

    public void getApi(View view) {

        final String url = "http://www.jaipurpinkpanthers.com/admin/index.php/json/getallpoint";
        final String[] response = new String[1];

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                response[0] = HttpCall.getDataGet(url);
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (!response[0].equals("")) {
                    tvJson.setText(response[0]);
                } else {
                    tvJson.setText("No response found");
                }

                /*List<CacheResponse> cacheResponses = CacheResponse.listAll(CacheResponse.class);
                if (cacheResponses.size() > 0) {
                    for (int i = 0; i < cacheResponses.size(); i++) {
                        Log.e(TAG, "###");
                        Log.e(TAG, "ID: " + String.valueOf(cacheResponses.get(i).getId()));
                        Log.e(TAG, "URL: " + cacheResponses.get(i).getUrl());
                        Log.e(TAG, "Count: " + String.valueOf(cacheResponses.get(i).getCount()));
                    }
                }*/
            }
        }.execute(null, null, null);

    }

    public JSONObject getJson(String user) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user", user);
        } catch (JSONException je) {
            Log.e(TAG, Log.getStackTraceString(je));
        }
        return jsonObject;
    }
}
