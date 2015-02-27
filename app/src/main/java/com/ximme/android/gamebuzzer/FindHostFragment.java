package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

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

}
