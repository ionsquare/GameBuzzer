package com.ximme.android.gamebuzzer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

        Log.d(TAG, "server_ip: " + server_ip);

        mHandler = new Handler();
        new Thread(new ClientThread()).start();

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
        new Thread(new Runnable() {
            public void run() {
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Sending Buzz message");
                out.println(MainActivity.MSG_BUZZ_REQUEST);
                Log.d(TAG, "Buzz message sent");
            }
        }).start();
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "ClientThread running");
            try {
                InetAddress hostAddress = InetAddress.getByName(server_ip);

                socket = new Socket(hostAddress, MainActivity.SERVERPORT);
                Log.d(TAG, "Socket opened");

                // Socket initialized, start listener thread
                // This will listen for buzzer block instruction
                listenerThread = new Thread(new ListenerThread(socket));
                listenerThread.start();
                Log.d(TAG, "Listener Thread started");

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }

    /**
     * Listens for messages from Host
     */
    class ListenerThread implements Runnable {
        private Socket socket;
        private BufferedReader input;

        ListenerThread(Socket socket){
            this.socket = socket;
            try{
                this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Log.d(TAG, "ListenerThread waiting for message on socket...");
                    String read = input.readLine();
                    if(read == null){
                        // Connection was terminated, exit the loop
                        Log.w(TAG, "Listener thread lost connection (read == null)");
                        mHandler.post(new updateUIThread(MainActivity.ACTION_CONN_LOST));
                        break;
                    }
                    Log.d(TAG, "ListenerThread received message");

                    mHandler.post(new updateUIThread(read));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            if(msg.equals(MainActivity.MSG_DISABLE)){
                mBuzz.setEnabled(false);
            }else if(msg.equals(MainActivity.MSG_ENABLE)){
                mBuzz.setEnabled(true);
            }else if(msg.equals(MainActivity.MSG_BUZZ_WIN)){
                playBuzzSound();
            }else if(msg.equals(MainActivity.ACTION_CONN_LOST)){

            }else{
                makeText("Unrecognized action: " + msg);
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
