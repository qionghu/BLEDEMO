package com.hq.blemesh;

import android.util.Log;

public class BleMesh {

    private final String TAG = "BleMesh";

    private static final BleMesh instance = new BleMesh();

    public static synchronized BleMesh getInstance(){
        return instance;
    }

    public void init(){

    }

    public void test(){
        Log.d(TAG, "test: do test!");
    }

}
