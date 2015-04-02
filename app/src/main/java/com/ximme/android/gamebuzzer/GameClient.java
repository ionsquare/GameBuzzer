package com.ximme.android.gamebuzzer;

import android.os.Handler;

/**
 * Created by matthew on 2015-03-26.
 */
public class GameClient {
    private static final String TAG = GameClient.class.getSimpleName();

    private int server_port;
    private Handler mHandler;

    public GameClient(int server_port, Handler handler) {
        this.server_port = server_port;
        mHandler = handler;
    }

    public void startBroadcastListener(){

    }
}
