package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class FindHostFragment extends Fragment {
    private static final String TAG = ContestantFragment.class.getSimpleName();

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

        new Thread(new Runnable() {
            public void run() {
                receiveBroadcast();
            }
        }).start();

        // TODO Remove this section when host/contestant connectivity works ========================
        // Add a TextView to the LinearLayout
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
        // TODO End of section to remove ===========================================================


        // TODO Search for hosts
        // TODO When a host is found, add it to the mFindHostList LinearLayout
        // TODO If connection to host is lost, remove that host from mFindHostList
        // Continue searching for hosts even if one was found
        // TODO When user selects host, switch to ContestantFragment

        return v;
    }

    private void joinHost(){

    }

    private void receiveBroadcast(){
        int port = ((MainActivity)getActivity()).port;

        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
//            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            DatagramSocket socket = new DatagramSocket(port, Utils.getBroadcastAddress(getActivity()));
            socket.setBroadcast(true);

            while (true) {
                Log.i(TAG, "Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                Log.i(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                String data = new String(packet.getData()).trim();
                Log.i(TAG, "Packet received; data: " + data);

//                Toast.makeText(getActivity(), data, Toast.LENGTH_LONG).show();

                /*
                // Send the packet data back to the UI thread
                Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                        // Puts the data into the Intent
                        .putExtra(Constants.EXTENDED_DATA_STATUS, data);
                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(localIntent);
                //*/
            }
        } catch (IOException ex) {
            Log.i(TAG, "Oops" + ex.getMessage());
        }
    }

}
