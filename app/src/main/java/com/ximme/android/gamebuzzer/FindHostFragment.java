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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class FindHostFragment extends ListFragment implements WifiP2pManager.PeerListListener {
    private static final String TAG = ContestantFragment.class.getSimpleName();

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pDevice device;
    // Layout elements
    private LinearLayout mFindHostList;
    private TextView mSearchingForHosts;

    public FindHostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));

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

        // TODO Search for hosts
        // TODO When a host is found, add it to the mFindHostList LinearLayout
        // TODO If connection to host is lost, remove that host from mFindHostList
        // Continue searching for hosts even if one was found
        // TODO When user selects host, switch to ContestantFragment

        return v;
    }

    private void joinHost(){

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


}
