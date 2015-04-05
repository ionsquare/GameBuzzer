package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Set;

/**
 * TODO List of things that need to be done
 * Update number of connected contestants as users join or drop
 * Listen for buzzers
 * When a buzzer is detected, disable all buzzers; notify successful contestant and tell it to buzz
 */

public class HostFragment extends Fragment {
    private static final String TAG = HostFragment.class.getSimpleName();

    // Layout elements
    private Button mEnableBuzzers;
    private ToggleButton mDiscovery;
    private TextView mNumPlayers;

    // Network
    //HostHandler mHandler;
    GameServer mGameServer;
    boolean mDiscoveryOn = true;

    private boolean buzzersEnabled = false;

    public HostFragment() {
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Log.d(TAG, "Broadcast IP: " + ((MainActivity) getActivity()).broadcastIP);
        Log.d(TAG, "Device IP: " + ((MainActivity) getActivity()).thisDeviceIP);

        Handler mHandler = new HostHandler();
        mGameServer = new GameServer(mHandler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        View v = inflater.inflate(R.layout.fragment_host, container, false);

        mEnableBuzzers = (Button) v.findViewById(R.id.enable_buzzers);
        mEnableBuzzers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This executes when the Enable Buzzers button is clicked
                // TODO Send message to contestants to enable buzzers
                enableBuzzers();
            }
        });

        mNumPlayers = (TextView) v.findViewById((R.id.players));

        mDiscovery = (ToggleButton) v.findViewById(R.id.discovery);
        mDiscovery.setChecked(mDiscoveryOn);
        mDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d(TAG, "Starting broadcast");
                    mDiscoveryOn = true;
                    startBroadcast();
                } else {
                    Log.d(TAG, "Stopping broadcast");
                    mDiscoveryOn = false;
                    stopBroadcast();
                }
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start the server
        if(mDiscoveryOn) {
            startBroadcast();
        }
        startServer();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop the server and broadcast
        if(mDiscoveryOn) {
            stopBroadcast();
        }
        stopServer();
    }

    //*
    private class HostHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            // TODO Return true when message handled (I think)
            Bundle data = msg.getData();
            String event_type = data.getString(GameServer.ARG_EVENT_TYPE);
            Log.d(TAG, "handleMessage() Event type: " + event_type);

            switch (event_type) {
                case GameServer.EVENT_CLIENT_DISCONNECT:
                case GameServer.EVENT_CLIENT_CONNECT:
                    String numPlayers = Integer.toString(mGameServer.getClientIDList().size());
                    mNumPlayers.setText(numPlayers);
                    //updatePlayers();
                    break;
                case GameServer.EVENT_RECEIVED_BROADCAST:
                    String clientAddress = data.getString(GameServer.ARG_CLIENT_ADDRESS);
                    break;
                case GameServer.EVENT_MESSAGE_RECEIVED:
                    String clientMessage = data.getString(GameServer.ARG_MESSAGE);
                    switch (clientMessage) {
                        case MainActivity.MSG_BUZZ_REQUEST:
                            disableBuzzers(data.getInt(GameServer.ARG_CLIENT_ID));
                            break;
                        default:
                            Log.d(TAG, "Unrecognized message: " + msg);
                            break;
                    }
                    break;
                default:
                    Log.d(TAG, "Unrecognized event type: " + event_type);
                    break;
            }
        }
    };
    //*/

    private void startBroadcast() {
        mGameServer.startBroadcast(MainActivity.MSG_HOST_BROADCAST, MainActivity.SERVER_PORT);
    }
    private void stopBroadcast() {
        mGameServer.stopBroadcast();
    }
    private void startServer(){
        mGameServer.startServer(MainActivity.SERVER_PORT);
    }
    private void stopServer(){
        mGameServer.stopServer();
    }

    private void enableBuzzers(){
        buzzersEnabled = true;
        Set<Integer> clientIDList = mGameServer.getClientIDList();
        for(int clientID : clientIDList){
            Log.d(TAG, "Enabling buzzer for client #" + clientID);
            mGameServer.sendMessageToClient(clientID, MainActivity.MSG_ENABLE);
        }
    }

    private void disableBuzzers(int exceptThisClient){
        if(buzzersEnabled == false){
            return;
        }
        buzzersEnabled = false;
        Set<Integer> clientIDList = mGameServer.getClientIDList();
        for(int clientID : clientIDList){
            if(clientID == exceptThisClient) {
                mGameServer.sendMessageToClient(clientID, MainActivity.MSG_BUZZ_WIN);
            }else{
                mGameServer.sendMessageToClient(clientID, MainActivity.MSG_DISABLE);
            }
        }
    }

    private void updatePlayers(){
        Log.d(TAG, "updatePlayers()");
        mNumPlayers.setText(Integer.toString(mGameServer.getClientIDList().size()));
    }

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
