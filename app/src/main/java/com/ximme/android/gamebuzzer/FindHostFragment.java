package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class FindHostFragment extends Fragment {
    private static final String TAG = FindHostFragment.class.getSimpleName();

    // Network
    Handler mHandler;
    GameServer mGameClient;
    Thread listenerThread;

    // Layout elements
    private LinearLayout mFindHostList;
    private TextView mSearchingForHosts;

    public FindHostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new FindHostHandler();
        mGameClient = new GameServer(mHandler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_find_host, container, false);

        mSearchingForHosts = (TextView) v.findViewById(R.id.searching_for_hosts);
        mFindHostList = (LinearLayout) v.findViewById(R.id.find_host_list);


        /*
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage){
                Bundle bundle = inputMessage.getData();
                Toast.makeText(getActivity(), bundle.getString("data"), Toast.LENGTH_LONG).show();
            }
        };
        //*/


        /*
        new Thread(new Runnable() {
            public void run() {
                receiveBroadcast();
            }
        }).start();
        //*/

        // TODO Remove this section when host/contestant connectivity works ========================
        // Add a TextView to the LinearLayout
        /*
        TextView placeHolder = new TextView(getActivity());
        placeHolder.setText("PlaceHolder Host for dev");
        placeHolder.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        placeHolder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        placeHolder.setBackgroundColor(0xffcccccc);
        placeHolder.setPadding(20, 20, 20, 20);
        mFindHostList.addView(placeHolder);

        // Set listener for pretend host to launch ContestantFragment
        // This will be removed when real host connection is available
        placeHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).onJoinHost();
            }
        });
        //*/
        // TODO End of section to remove ===========================================================


        // TODO Search for hosts
        // TODO When a host is found, add it to the mFindHostList LinearLayout
        // TODO If connection to host is lost, remove that host from mFindHostList
        // Continue searching for hosts even if one was found
        // TODO When user selects host, switch to ContestantFragment

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();

        // Listen for host broadcast
        // startListenerThread();
        startBroadcastListener();
    }

    @Override
    public void onPause(){
        super.onPause();
        //listenerThread.interrupt();
        //Log.d(TAG, "Thread Interrupted");
        stopBroadcastListener();
    }

    private class FindHostHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage()");
            Bundle data = msg.getData();
            String event_type = data.getString(GameServer.EVENT_TYPE);
            Log.d(TAG, "handleMessage() Event type: " + event_type);

            switch(event_type){
                case GameServer.EVENT_RECEIVED_BROADCAST:
                    Log.d(TAG, "handleMessage() broadcast event received");
                    String hostAddress = data.getString(GameServer.ARG_CLIENT_ADDRESS);
                    String message = data.getString(GameServer.ARG_MESSAGE);
                    Log.d(TAG, "handleMessage(): message = " + message);
                    if(message.equals(MainActivity.MSG_HOST_BROADCAST)){
                        ((MainActivity) getActivity()).onJoinHost(hostAddress);
                    }else{
                        // TODO Remove this
                        Log.d(TAG, "This shouldn't happen, I did something wrong");
                    }

                    break;
                default:
                    Log.e(TAG, "handleMessage() event type not recognized: " + event_type);
                    break;
            }
        }
    }


    private void startBroadcastListener(){
        Log.d(TAG, "startBroadcastListener()");
        mGameClient.startBroadcastListener(MainActivity.SERVER_PORT);
    }

    private void stopBroadcastListener(){
        Log.d(TAG, "stopBroadcastListener()");
        mGameClient.stopBroadcastListener();
    }

    private void startBroadcast(){
        // TODO Implement and use - host currently does not listen for client broadcasts
        mGameClient.startBroadcast(MainActivity.MSG_CLIENT_BROADCAST, MainActivity.CLIENT_BROADCAST_PORT);
    }

    private void stopBroadcast(){
        // TODO Implement and use - host currently does not listen for client broadcasts
        mGameClient.stopBroadcast();
    }

    private void joinHost(){

    }

    /*
    public void startListenerThread(){
        listenerThread = new Thread(new Runnable() {
            public void run() {
                receiveBroadcast();
            }
        });
        listenerThread.start();
    }

    private void receiveBroadcast(){
        try {
            //Keep a socket open to listen to all the UDP traffic that is destined for this port
            // InetAddress broadcastAddress = Utils.getBroadcastAddress(getActivity());
            InetAddress broadcastAddress = InetAddress.getByName("127.0.0.1");
            Log.d(TAG, "broadcastAddress: " + broadcastAddress.getHostAddress());

            DatagramSocket socket = new DatagramSocket(MainActivity.SERVER_PORT,
                    broadcastAddress);
//            socket.setBroadcast(true);

            Log.i(TAG, "Ready to receive broadcast packets!");

            //Receive a packet
            byte[] recvBuf = new byte[15000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);
            socket.close();

            //Packet received
            Log.i(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
            String data = new String(packet.getData()).trim();
            Log.i(TAG, "Packet received; data: " + data);

            // Get host info
            hostAddress = packet.getAddress();
            Log.d(TAG, "Host address: " + hostAddress.getHostAddress());

            makeText(data);

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ((MainActivity) getActivity()).onJoinHost(hostAddress.getHostAddress());
                }
            });
        } catch (IOException ex) {
            Log.i(TAG, "Oops" + ex.getMessage());
            makeText("There was an error, go back and try again");
            ex.printStackTrace();
        }


    }
    //*/

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
