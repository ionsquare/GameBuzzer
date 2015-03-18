package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

    private int count = 0;

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Log.d(TAG, "Broadcast IP: " + ((MainActivity)getActivity()).broadcastIP);
        Log.d(TAG, "Device IP: " + ((MainActivity)getActivity()).thisDeviceIP);

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

                new Thread(new Runnable() {
                    public void run() {
                        sendUDPMessage("sendUDPMessage");
//                        altUDPMessage("altUDPMessage");
                    }
                }).start();


            }
        });

        return v;
    }

    private void sendUDPMessage(String msg) {
        Log.d(TAG, "sendUDPMessage()");

        int port = ((MainActivity)getActivity()).port;

        try {
            DatagramSocket clientSocket = new DatagramSocket();

            clientSocket.setBroadcast(true);
            InetAddress address = Utils.getBroadcastAddress(getActivity());
//            InetAddress address = InetAddress.getByName(Utils.getBroadcastIP());

            byte[] sendData;

            sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData,
                    sendData.length, address, port);
            clientSocket.send(sendPacket);

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void altUDPMessage(String data){
        Log.d(TAG, "altUDPMessage");
        Toast.makeText(getActivity(), "altUDPMessage", Toast.LENGTH_LONG).show();

        int port = ((MainActivity)getActivity()).port;

        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                    Utils.getBroadcastAddress(getActivity()), port);
//                    InetAddress.getByName(Utils.getBroadcastIP()), port);
            socket.send(packet);

//            byte[] buf = new byte[1024];
//            packet = new DatagramPacket(buf, buf.length);
//            socket.receive(packet);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
    public void registerService(int port) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Create the NsdServiceInfo object, and populate it.
            NsdServiceInfo serviceInfo  = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceInfo.setServiceName("gamebuzzer");
            serviceInfo.setServiceType("_http._tcp.");
            serviceInfo.setPort(port);
        }
    }
    //*/
}
