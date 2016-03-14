package com.jay.arc.Http;

import android.util.Log;

import com.jay.arc.Util.Md5;
import com.jay.arc.db.CacheResponse;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Jay on 18-02-2016.
 */
public class HttpSimple {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();
    private static String TAG = "ARC";
    private static final String POST_NULL = "-1";   //Do not change. Will remain -1 in case of Get request
    private static int HTTP_CODE;
    private static final int MAX_LINKS_CACHE = 2;   //Maximum number of links to be cached in the database

    public static String postRequest(String url, String postJson) {

        String response = "";
        if (hasInternet()) { //if user is connected to internet
            try { //get the fresh response
                response = post(url, postJson);
            } catch (IOException ioe) {
                Log.e(TAG, Log.getStackTraceString(ioe));
            }

            if (!response.equals("")) { //if response not recevied //if error //or response is not
                if (HTTP_CODE == 200) {
                    List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
                    if (cacheResponses.size() == 1) { //if already there in cache //overwrite the old response with new response
                        CacheResponse cacheResponse = cacheResponses.get(0);
                        cacheResponse.setResponse(response);
                        cacheResponse.setCount(cacheResponse.getCount() + 1);
                        cacheResponse.save();
                        cacheResponse.setTime(getCurrentTimeLong());

                        response = cacheResponse.getResponse();

                    } else { // if not there in cache make new cache
                        newCache(url, postJson, response);
                    }
                } else {
                    List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
                    if (cacheResponses.size() == 1) {  //if already there in cache //send this cached response
                        CacheResponse cacheResponse = cacheResponses.get(0);
                        cacheResponse.setCount(cacheResponse.getCount() + 1);
                        cacheResponse.save();

                        response = cacheResponse.getResponse();
                    }
                }
            }

        } else {    //if user is not connected to internet

            List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
            if (cacheResponses.size() == 1) {  //if already there in cache //send this cached response
                CacheResponse cacheResponse = cacheResponses.get(0);
                cacheResponse.setCount(cacheResponse.getCount() + 1);
                cacheResponse.save();

                response = cacheResponse.getResponse();
            }
        }
        return response;
    }

    public static String getRequest(String url) {

        String response = "";
        if (hasInternet()) { //if user is connected to internet
            try { //get the fresh response
                response = get(url);
            } catch (IOException ioe) {
                Log.e(TAG, Log.getStackTraceString(ioe));
            }

            if (!response.equals("")) { //if response not recevied //if error //or response is not
                if (HTTP_CODE == 200) {
                    List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, POST_NULL);
                    if (cacheResponses.size() == 1) { //if already there in cache //overwrite the old response with new response
                        CacheResponse cacheResponse = cacheResponses.get(0);
                        cacheResponse.setResponse(response);
                        cacheResponse.setCount(cacheResponse.getCount() + 1);
                        cacheResponse.setTime(getCurrentTimeLong());
                        cacheResponse.save();

                        response = cacheResponse.getResponse();

                    } else { // if not there in cache make new cache
                        newCache(url, POST_NULL, response);
                    }
                }
            } else {
                List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, POST_NULL);
                if (cacheResponses.size() == 1) {  //if already there in cache //send this cached response
                    CacheResponse cacheResponse = cacheResponses.get(0);
                    cacheResponse.setCount(cacheResponse.getCount() + 1);
                    cacheResponse.save();

                    response = cacheResponse.getResponse();
                }
            }

        } else {    //if user is not connected to internet
            List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, POST_NULL);
            if (cacheResponses.size() == 1) {  //if already there in cache //send this cached response
                CacheResponse cacheResponse = cacheResponses.get(0);
                cacheResponse.setCount(cacheResponse.getCount() + 1);
                cacheResponse.save();

                response = cacheResponse.getResponse();
            }
        }
        return response;
    }

    private static String newCache(String url, String postData, String response) {  //add new get/post cache link
        long count = CacheResponse.count(CacheResponse.class);
        if (count == MAX_LINKS_CACHE) {     //if the count is equal to the maximum number of links to be cached is specified

            /*
            Search the least used cached link from the table.
            The links are ordered by count and time
            The link with least count is removed
            Multiple links with same least count are then further ordered by the time they were added and the oldest one from them is deleted
             */
            List<CacheResponse> cacheResponses = CacheResponse.findWithQuery(CacheResponse.class, "SELECT ID FROM CACHE_RESPONSE ORDER BY COUNT DESC, TIME DESC");

            int size = cacheResponses.size();
            long lastId = cacheResponses.get(size - 1).getId();
            CacheResponse cacheResponseOld = CacheResponse.findById(CacheResponse.class, lastId);   //remove the last link from the table
            cacheResponseOld.delete();

            CacheResponse cacheResponseNew = new CacheResponse(url, postData, response, getCurrentTimeLong(), 1, Md5.getHash(response)); //add new link in the table
            cacheResponseNew.save();
            return cacheResponseNew.getResponse();

        } else {        //if count is less than maximum number of links
            CacheResponse cacheResponse = new CacheResponse(url, postData, response, getCurrentTimeLong(), 1, Md5.getHash(response));
            cacheResponse.save();
            return cacheResponse.getResponse();
        }
    }

    private static String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        HTTP_CODE = response.code();
        return response.body().string();
    }

    private static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        HTTP_CODE = response.code();
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

    private static long getCurrentTimeLong() {
        Date date = new Date();
        return date.getTime();
    }
}
