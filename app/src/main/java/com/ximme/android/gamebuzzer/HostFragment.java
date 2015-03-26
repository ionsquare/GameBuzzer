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

public class HostFragment extends Fragment implements Handler.Callback {
    private static final String TAG = HostFragment.class.getSimpleName();

    // Layout elements
    private Button mEnableBuzzers;
    private ToggleButton mDiscovery;

    // Network
    Handler mHandler;
    GameServer mGameServer;
    Thread mBroadcastThread = null;

    private boolean buzzersEnabled = false;

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Log.d(TAG, "Broadcast IP: " + ((MainActivity)getActivity()).broadcastIP);
        Log.d(TAG, "Device IP: " + ((MainActivity) getActivity()).thisDeviceIP);

        mHandler = new Handler(this);
        mGameServer = new GameServer(MainActivity.SERVER_PORT, mHandler);
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

        mDiscovery = (ToggleButton) v.findViewById(R.id.discovery);
        mDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){

                }
            }
        });

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();

        // Start the server
        mGameServer.startServer();
        mGameServer.startBroadcast();
    }

    @Override
    public void onPause(){
        super.onPause();

        // Stop the server and broadcast
        mGameServer.stopBroadcast();
        mGameServer.stopServer();
    }

    private void enableBuzzers(){
        buzzersEnabled = true;
        Set<Integer> clientIDList = mGameServer.getClientIDList();
        for(int clientID : clientIDList){
            mGameServer.sendMessage(clientID, MainActivity.MSG_ENABLE);
        }
    }

    private void disableBuzzers(int exceptThisClient){
        buzzersEnabled = false;
        Set<Integer> clientIDList = mGameServer.getClientIDList();
        for(int clientID : clientIDList){
            if(clientID == exceptThisClient) {
                mGameServer.sendMessage(clientID, MainActivity.MSG_BUZZ_WIN);
            }else{
                mGameServer.sendMessage(clientID, MainActivity.MSG_DISABLE);
            }
        }
    }

    private void updatePlayers(){
        getActivity().setContentView(R.layout.fragment_host);
        TextView tv = new TextView(getActivity());
        tv = (TextView) getActivity().findViewById(R.id.players);
        tv.setText(mGameServer.getClientIDList().size());
    }

    @Override
    public boolean handleMessage(Message msg) {
        Bundle data = msg.getData();
        String action = data.getString(GameServer.EVENT_TYPE);
        switch(action){
            case GameServer.EVENT_CLIENT_CONNECT:
                break;
            case GameServer.EVENT_CLIENT_DISCONNECT:
                break;
            case GameServer.EVENT_MESSAGE_RECEIVED:
                String clientMessage = data.getString(GameServer.ARG_MESSAGE);
                switch(clientMessage){
                    case MainActivity.MSG_BUZZ_REQUEST:
                        break;
                    default:
                        Log.d(TAG, "Unrecognized message: " + msg);
                        break;
                }
                break;
            default:
                Log.d(TAG, "Unrecognized action: " + action);
                break;
        }
        return false;
    }

    class updateUIThread implements Runnable {
        private String msg;
        private int clientID;

        public updateUIThread(int clientID, String str) {
            this.msg = str;
            this.clientID = clientID;
        }

        @Override
        public void run() {
            if(msg.equals(MainActivity.MSG_BUZZ_REQUEST) && buzzersEnabled){
                // This should be thread-safe since it runs on the UI thread
                disableBuzzers(clientID);
            }
            makeText(msg);
        }
    }

    class updatePlayersThread implements Runnable{
        @Override
        public void run(){
            updatePlayers();
        }
    }

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
