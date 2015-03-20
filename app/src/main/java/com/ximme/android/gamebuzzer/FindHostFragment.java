package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class FindHostFragment extends Fragment {
    private static final String TAG = FindHostFragment.class.getSimpleName();

    // Network
    private Handler mHandler;
    private InetAddress hostAddress = null;
    private Thread listenerThread;

    // Layout elements
    private LinearLayout mFindHostList;
    private TextView mSearchingForHosts;

    public FindHostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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
        startListenerThread();
    }

    @Override
    public void onPause(){
        super.onPause();
        listenerThread.interrupt();
        Log.d(TAG, "Thread Interrupted");
    }

    private void joinHost(){

    }

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
            DatagramSocket socket = new DatagramSocket(MainActivity.SERVERPORT,
                    Utils.getBroadcastAddress(getActivity()));
            socket.setBroadcast(true);

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

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
