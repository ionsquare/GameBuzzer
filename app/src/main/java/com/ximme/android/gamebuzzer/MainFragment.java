package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MainFragment extends Fragment {
    private static final String TAG = MainFragment.class.getSimpleName();

    // Layout elements
    private Button mHost;
    private Button mContestant;

    public MainFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_main, container, false);

        mHost = (Button) v.findViewById(R.id.host);
        mHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This executes when the Host button is clicked
                // TODO implement this - should switch to HostFragment

                Log.d(TAG, "Host role selected");
                ((MainActivity) getActivity()).onHostRoleSelected();

            }
        });

        mContestant = (Button) v.findViewById(R.id.contestant);
        mContestant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This executes when the Contestant button is clicked
                // TODO implement this - should switch to FindHostFragment

                ((MainActivity)getActivity()).onContestantSelected();
                Log.d(TAG, "Contestant role selected");

            }
        });

        return v;
    }

}
