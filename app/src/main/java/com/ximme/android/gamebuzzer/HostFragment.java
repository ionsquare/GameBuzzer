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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * TODO List of things that need to be done
 * Update number of connected contestants as users join or drop
 * Listen for buzzers
 * When a buzzer is detected, disable all buzzers; notify successful contestant and tell it to buzz
 */

public class HostFragment extends Fragment {
    private static final String TAG = HostFragment.class.getSimpleName();

    // Layout elements
    private Button mEnableBuzzers;

    // Network
    private ServerSocket serverSocket;
    Handler updateConversationHandler;
    Thread serverThread = null;
    private boolean fragmentIsActive = false;
    private static final int BROADCASTINTERVAL = 10000;     // Milliseconds
    private ArrayList<Socket> socketList = new ArrayList<>();
    private ArrayList<CommunicationThread> communicationThreadList= new ArrayList<>();

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Log.d(TAG, "Broadcast IP: " + ((MainActivity)getActivity()).broadcastIP);
        Log.d(TAG, "Device IP: " + ((MainActivity)getActivity()).thisDeviceIP);

        updateConversationHandler = new Handler();
        serverThread = new Thread(new ServerThread());
        this.serverThread.start();
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
            startEnableBuzzersThread();
            }
        });

        return v;
    }

    @Override
    public void onResume(){
        super.onResume();

        // Start broadcasting as host
        fragmentIsActive = true;
        startBroadcastThread();
    }

    @Override
    public void onPause(){
        super.onPause();
        // TODO close all existing sockets

        fragmentIsActive = false;
    }

    @Override
    public void onStop(){
        super.onStop();

        try{
            serverSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void startBroadcastThread(){
        new Thread(new Runnable() {
            public void run() {
                while(fragmentIsActive) {
                    try {
                        sendUDPMessage("GameBuzzer Host here");
                        Thread.sleep(BROADCASTINTERVAL);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void sendUDPMessage(String msg) {
        Log.d(TAG, "sendUDPMessage()");

        try {
            DatagramSocket clientSocket = new DatagramSocket();

            clientSocket.setBroadcast(true);
            InetAddress address = Utils.getBroadcastAddress(getActivity());

            byte[] sendData;

            sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData,
                    sendData.length, address, MainActivity.SERVERPORT);
            clientSocket.send(sendPacket);

            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void makeText(final String text){
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void startEnableBuzzersThread(){
        new Thread(new EnableBuzzersThread()).start();
    }

    public class EnableBuzzersThread implements Runnable {
        @Override
        public void run(){
            Log.d(TAG, "Enable buzzers");

            PrintWriter out;
            for(Socket socket : socketList){
                Log.d(TAG, "Attempting to enable a buzzer");
                try {
                    out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("Enable Buzzer");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ServerThread implements Runnable {
        @Override
        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(MainActivity.SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (fragmentIsActive && !Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    socketList.add(socket);
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    updateConversationHandler.post(new updateUIThread(read));
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
        }
    }
}
