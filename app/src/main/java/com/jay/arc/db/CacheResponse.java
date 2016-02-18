package com.jay.arc.db;

import com.orm.SugarRecord;

/**
 * Created by Jay on 18-02-2016.
 */
public class CacheResponse extends SugarRecord{

    public String url;
    public String postData;
    public String response;
    public long time;
    public int count;

    public CacheResponse() {
        super();
    }

    public CacheResponse(String url,String postData, String response, long time, int count) {
        this.postData = postData;
        this.response = response;
        this.url = url;
        this.time = time;
        this.count = count;
    }

    public String getPostData() {
        return postData;
    }

    public void setPostData(String postData) {
        this.postData = postData;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
