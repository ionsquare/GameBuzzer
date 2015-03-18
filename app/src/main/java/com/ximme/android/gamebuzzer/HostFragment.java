package com.ximme.android.gamebuzzer;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * TODO List of things that need to be done
 * Update number of connected contestants as users join or drop
 * Listen for buzzers
 * When a buzzer is detected, disable all buzzers; notify successful contestant and tell it to buzz
 */

public class HostFragment extends Fragment {
    private static final String TAG = HostFragment.class.getSimpleName();

    private String broadcastIP;
    private String thisDeviceIP;

    // Layout elements
    private Button mEnableBuzzers;

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        broadcastIP = ((MainActivity) getActivity()).getBroadcast();
        thisDeviceIP = Utils.getIPAddress(true);


        Log.d(TAG, "Broadcast IP: " + broadcastIP);
        Log.d(TAG, "Device IP: " + thisDeviceIP);

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

            }
        });

        return v;
    }

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
}
