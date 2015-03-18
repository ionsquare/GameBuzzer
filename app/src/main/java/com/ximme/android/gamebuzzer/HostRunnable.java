package com.ximme.android.gamebuzzer;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
   * Created by matthew on 2015-03-17.
   */
 public class HostRunnable implements Runnable {
    private static final String TAG = HostRunnable.class.getSimpleName();

    Activity mActivity;
    Handler mHandler;
    InetAddress mBroadcastAddress;

//    public HostRunnable(Handler handler, InetAddress broadcastAddress){
    public HostRunnable(Activity activity, InetAddress broadcastAddress){
//        mHandler = handler;
        mActivity = activity;
        mBroadcastAddress = broadcastAddress;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        receiveBroadcast();

        // Listen for new contestants
        // Or possibly spawn another thread dedicated to listening for contestants
    }

    private void receiveBroadcast(){
        int port = MainActivity.port;
        String data;

        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            //            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            DatagramSocket socket = new DatagramSocket(port, mBroadcastAddress);
            socket.setBroadcast(true);

            while (true) {
                Log.i(TAG, "Ready to receive broadcast packets!");

                // Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                // Packet received
                Log.i(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                data = new String(packet.getData()).trim();
                Log.i(TAG, "Packet received; data: " + data);

                // Send data to UI thread to make toast
                /*
                Bundle bundle = new Bundle();
                bundle.putString("data", data);
                Message msg = new Message();
                msg.setData(bundle);
                mHandler.handleMessage(msg);
                //*/

               makeText(data);


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

    private void makeText(final String text){
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
