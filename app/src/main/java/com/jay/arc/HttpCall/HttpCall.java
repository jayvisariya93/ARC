package com.jay.arc.HttpCall;

import android.util.Log;

import com.jay.arc.db.CacheResponse;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Jay on 18-02-2016.
 */
public class HttpCall {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();
    private static String TAG = "ARC";

    public static String getDataPost(String url, String postJson) {

        String response = "";
        if (hasInternet()) { //if user is connected to internet
            try { //get the fresh response
                response = post(url, postJson);
            } catch (IOException ioe) {
                Log.e(TAG, Log.getStackTraceString(ioe));
            }

            List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
            if (cacheResponses.size() == 1) { //if already there in cache //overwrite the old response with new response
                CacheResponse cacheResponse = cacheResponses.get(0);
                cacheResponse.setPostData(response);
            } else { // if not there in cache make new cache
                CacheResponse cacheResponse = new CacheResponse(url, postJson, response);
                cacheResponse.save();
            }

        } else {    //if user is not connected to internet

            List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
            if (cacheResponses.size() == 1) {  //if already there in cache //send this cached response
                response = cacheResponses.get(0).getResponse();
            } else {
                response = "";
            }
        }
        return response;
    }

    private static String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private static boolean hasInternet() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }
}
