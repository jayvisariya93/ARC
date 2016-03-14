package com.jay.arc.Http;

import android.os.AsyncTask;
import android.util.Log;

import com.jay.arc.Util.Md5;
import com.jay.arc.db.CacheResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Jay on 12-03-2016.
 */
public class HttpCallback {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();
    private static String TAG = "ARC";
    private static final String POST_NULL = "-1";
    private static int HTTP_CODE;
    private static final int MAX_LINKS_CACHE = 2;   //Maximum number of links to be cached in the database
    private static boolean cachedPresent = false;


    public static void get(final HttpInterface httpInterface, final String url) {
        String response = "";
        //Send the Cached copy initially
        List<CacheResponse> oldResponsesInitial = getCacheResponses(url, POST_NULL);
        if (oldResponsesInitial.size() == 1) {  //if already there in cache //send this cached response
            CacheResponse cacheResponse = oldResponsesInitial.get(0);
            cacheResponse.setCount(cacheResponse.getCount() + 1);
            cacheResponse.save();

            response = cacheResponse.getResponse();

            cachedPresent = true;
            httpInterface.refreshView(response);
        }

        //Get the fresh copy from server
        if (hasInternet()) { //if user is connected to internet
            final String[] a = new String[1];      //response
            a[0] = "";
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try { //get the fresh response
                        a[0] = HttpCallback.viaGet(url);
                    } catch (IOException ioe) {
                        Log.e(TAG, Log.getStackTraceString(ioe));
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(String s) {
                    if (!a[0].isEmpty()) {      //response is not empty
                        HttpCallback.processJson(url, a[0], POST_NULL, httpInterface);
                    } else {                    //response is empty
                        if (!cachedPresent)     //reponse is empty and even cache is not availabe
                            httpInterface.error();
                    }
                }
            }.execute(null, null, null);
        } else {
            if (!cachedPresent)
                httpInterface.noInternet();
        }
    }

    public static void post(final HttpInterface httpInterface, final String url, final String postData) {
        String response = "";
        //Send the Cached copy initially
        List<CacheResponse> oldResponsesInitial = getCacheResponses(url, postData);
        if (oldResponsesInitial.size() == 1) {  //if already there in cache //send this cached response
            CacheResponse cacheResponse = oldResponsesInitial.get(0);
            cacheResponse.setCount(cacheResponse.getCount() + 1);
            cacheResponse.save();

            response = cacheResponse.getResponse();

            cachedPresent = true;
            httpInterface.refreshView(response);
        }


        //Get the fresh copy from server
        if (hasInternet()) { //if user is connected to internet

            final String[] a = new String[1];      //response
            a[0] = "";
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try { //get the fresh response
                        a[0] = HttpCallback.viaPost(url, postData);
                    } catch (IOException ioe) {
                        Log.e(TAG, Log.getStackTraceString(ioe));
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(String s) {
                    if (!a[0].isEmpty()) {      //response is not empty
                        HttpCallback.processJson(url, a[0], POST_NULL, httpInterface);
                    } else {                    //response is empty
                        if (!cachedPresent)     //reponse is empty and even cache is not availabe
                            httpInterface.error();
                    }
                }
            }.execute(null, null, null);
        } else {
            if (!cachedPresent)
                httpInterface.noInternet();
        }
    }

    private static void processJson(String url, String response, String postData, HttpInterface httpInterface) {

        if (!response.equals("")) { //if response not recevied //if error //or response is not

            if (HTTP_CODE == 200) {
                JSONObject fullResponse;
                String data = "";
                boolean value = false;
                try {
                    fullResponse = new JSONObject(response);

                    if (fullResponse.optBoolean("value")) {   //value is true
                        data = fullResponse.optString("data");
                        value = true;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (value) {    //value is true

                    List<CacheResponse> oldResponsesCheck = getCacheResponses(url, postData);
                    if (oldResponsesCheck.size() == 1) { //if already there in cache
                        CacheResponse cacheResponse = oldResponsesCheck.get(0);
                        String newHash = Md5.getHash(data);
                        if (!cacheResponse.getHash().equals(newHash)) {       //hash is different //overwrite the old response with new response

                            cacheResponse.setResponse(data);
                            cacheResponse.setCount(cacheResponse.getCount() + 1);
                            cacheResponse.setTime(getCurrentTimeLong());
                            cacheResponse.setHash(newHash);
                            cacheResponse.save();

                            data = cacheResponse.getResponse();

                            httpInterface.refreshView(data);    //response is different so refresh the view
                        } else {    //hash is same //do not overwrite the response //only chance the time and count

                            cacheResponse.setCount(cacheResponse.getCount() + 1);
                            cacheResponse.setTime(getCurrentTimeLong());
                            cacheResponse.save();
                            // response is same so no need to refresh the view
                        }
                    } else { // if not there in cache make new cache

                        newCache(url, postData, data);
                        httpInterface.refreshView(data);    //new data added, refresh the view
                    }

                } else {    //value is false
                    if (!cachedPresent)
                        httpInterface.error();
                }
            } else {
                httpInterface.error();
            }
        } else {
            //if response is null
            httpInterface.error();
        }
    }

    private static List<CacheResponse> getCacheResponses(String url, String postJson) {
        return CacheResponse.findWithQuery(CacheResponse.class, "SELECT * FROM CACHE_RESPONSE where URL = ? AND POST_DATA = ?", url, postJson);
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

    private static String viaPost(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        HTTP_CODE = response.code();
        return response.body().string();
    }

    private static String viaGet(String url) throws IOException {
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
