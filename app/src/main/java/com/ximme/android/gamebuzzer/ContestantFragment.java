package com.ximme.android.gamebuzzer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class ContestantFragment extends Fragment {
    private static final String TAG = ContestantFragment.class.getSimpleName();

    public static final String ARG_HOST_ADDRESS = "hostAddress";

    // Network
    private String server_ip;
    private Socket socket;
    private Thread listenerThread;
    Handler mHandler;
    GameServer mGameClient;

    // Layout elements
    private Button mBuzz;
    private MediaPlayer player;
    private TextView mStatus;

    public static ContestantFragment newInstance(String hostAddress) {
        ContestantFragment f = new ContestantFragment();

        Bundle args = new Bundle();
        args.putString(ARG_HOST_ADDRESS, hostAddress);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        server_ip = args.getString(ARG_HOST_ADDRESS);
        InetAddress server_address = null;
        try {
            server_address = InetAddress.getByName(server_ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not get host address!");
            // TODO Pop the backstack back to the main screen and show toast error message
        }

        Log.d(TAG, "server_ip: " + server_ip);

        mHandler = new ContestantHandler();
        mGameClient = new GameServer(mHandler);
        mGameClient.initHostConnection(server_address, MainActivity.SERVER_PORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_contestant, container, false);

        mBuzz = (Button) v.findViewById(R.id.buzz);
        mBuzz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable button
                mBuzz.setEnabled(false);
                sendBuzzMsg();
            }
        });

        mStatus = (TextView) v.findViewById(R.id.status);

        return v;
    }

    @Override
    public  void onResume(){
        super.onResume();

    }

    public void onPause(){
        super.onPause();
        // TODO Close socket!!

    }

    private void playBuzzSound(){
        player = MediaPlayer.create(getActivity(), R.raw.buzzer);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer player){
                player.release();
            }
        });
    }

    private void sendBuzzMsg(){
        Log.d(TAG, "Sending Buzz message");
        mGameClient.sendMessageToHost(MainActivity.MSG_BUZZ_REQUEST);
        Log.d(TAG, "Buzz message sent");
    }

    class ContestantHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Bundle data = message.getData();
            String event_type = data.getString(GameServer.ARG_EVENT_TYPE);

            if(event_type.equals(GameServer.EVENT_MESSAGE_RECEIVED)) {
                String msg = data.getString(GameServer.ARG_MESSAGE);
                if (msg.equals(MainActivity.MSG_DISABLE)) {
                    mBuzz.setEnabled(false);
                } else if (msg.equals(MainActivity.MSG_ENABLE)) {
                    mBuzz.setEnabled(true);
                } else if (msg.equals(MainActivity.MSG_BUZZ_WIN)) {
                    playBuzzSound();
                } else if (msg.equals(MainActivity.ACTION_CONN_LOST)) {

                } else {
                    makeText("Unrecognized action: " + msg);
                }
            }
        }
    }

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
