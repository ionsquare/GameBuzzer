package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private String SERVER_IP;
    private Socket socket;
    private Thread listenerThread;
    Handler mHandler;

    // Layout elements
    private Button mBuzz;

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
        SERVER_IP = args.getString(ARG_HOST_ADDRESS);

        Log.d(TAG, "SERVER_IP: " + SERVER_IP);

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
                // This executes when the Host button is clicked
                // TODO implement this - send buzz message to host

                try{
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("Buzz!");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        return v;
    }

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress hostAddress = InetAddress.getByName(SERVER_IP);

                socket = new Socket(hostAddress, MainActivity.SERVERPORT);

                // Socket intialized, start listener thread
                // This will listen for buzzer block instruction
                listenerThread = new Thread(new ListenerThread(socket));
                listenerThread.start();

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }

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
                    String read = input.readLine();
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
            // text.setText(text.getText().toString()+"Client Says: "+ msg + "\n");
            makeText(msg);
            // TODO disable buzzer
        }
    }


}
