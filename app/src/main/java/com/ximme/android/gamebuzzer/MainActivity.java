package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {
    private static String TAG = MainActivity.class.getSimpleName();

    public static final int SERVERPORT = 2769;
    public static final String MSG_ENABLE = "enable";
    public static final String MSG_DISABLE = "disable";
    public static final String MSG_BUZZ_REQUEST = "buzz?";
    public static final String MSG_BUZZ_WIN = "buzz!";
    public static final String MSG_HOST_HERE = "GameBuzzer Host here";

    public static final String ACTION_CONN_LOST = "lost connection";

    public String broadcastIP;
    public String thisDeviceIP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }

        try {
            broadcastIP = Utils.getBroadcastIP();
            thisDeviceIP = Utils.getIPAddress(true);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_invite:
                break;

            default:
                Log.d(TAG, "Unrecognized option");
        }

        return super.onOptionsItemSelected(item);
    }

    public void onHostRoleSelected(){
        HostFragment hostFragment = new HostFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, hostFragment)
                .addToBackStack(null)
                .commit();
    }

    public void onContestantSelected(){
        FindHostFragment findHostFragment = new FindHostFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, findHostFragment)
                .addToBackStack(null)
                .commit();
    }

    public void onJoinHost(String hostAddress){
        // TODO This will need to carry some data about what host to join
        // Not sure what that data will be yet
        // Will probably need to use newInstance strategy

        ContestantFragment contestantFragment = ContestantFragment.newInstance(hostAddress);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, contestantFragment)
                .addToBackStack(null)
                .commit();
    }

}
