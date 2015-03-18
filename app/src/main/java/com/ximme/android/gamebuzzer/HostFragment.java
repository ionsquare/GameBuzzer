package com.ximme.android.gamebuzzer;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO List of things that need to be done
 * Update number of connected contestants as users join or drop
 * Listen for buzzers
 * When a buzzer is detected, disable all buzzers; notify successful contestant and tell it to buzz
 */

public class HostFragment extends ListFragment implements WifiP2pManager.PeerListListener{
    private static final String TAG = HostFragment.class.getSimpleName();

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pDevice device;

    // Layout elements
    private Button mEnableBuzzers;

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

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

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // Out with the old, in with the new.
        peers.clear();
        peers.addAll(peerList.getDeviceList());

        // If an AdapterView is backed by this data, notify it
        // of the change.  For instance, if you have a ListView of available
        // peers, trigger an update.
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(MainActivity.TAG, "No devices found");
            return;
        }

    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((MainActivity) getActivity()).showDeviceDetails(device);
    }

}
