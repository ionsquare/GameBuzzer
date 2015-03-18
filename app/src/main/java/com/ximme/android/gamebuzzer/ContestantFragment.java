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

/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class ContestantFragment extends Fragment {
    private static final String TAG = ContestantFragment.class.getSimpleName();

    // Layout elements
    private Button mBuzz;

    public ContestantFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_contestant, container, false);

        mBuzz = (Button) v.findViewById(R.id.buzz);
        mBuzz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This executes when the Host button is clicked
                // TODO implement this - send buzz message to host
                receiveBroadcast();

            }
        });

        return v;
    }

    private void receiveBroadcast(){
        Log.d(TAG, "Waiting for broadcast");
        Toast.makeText(getActivity(), "Waiting for broadcast", Toast.LENGTH_LONG).show();

        int port = ((MainActivity)getActivity()).port;

        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setBroadcast(true);

            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
