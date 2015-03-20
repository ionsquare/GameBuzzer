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
    private ArrayList<ClientConnection> clientConnectionList = new ArrayList<>();
    private int nextClientID = 0;

    private boolean buzzersEnabled = false;

    public HostFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
        Log.d(TAG, "HostFragment() (constructor)");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Log.d(TAG, "Broadcast IP: " + ((MainActivity)getActivity()).broadcastIP);
        Log.d(TAG, "Device IP: " + ((MainActivity) getActivity()).thisDeviceIP);

        updateConversationHandler = new Handler();
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
        serverThread = new Thread(new ServerThread());
        this.serverThread.start();
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

    public void startEnableBuzzersThread(){
        new Thread(new EnableBuzzersThread()).start();
    }

    public class EnableBuzzersThread implements Runnable {
        @Override
        public void run(){
            Log.d(TAG, "Enable buzzers");
            Log.d(TAG, "client list size: ");

            buzzersEnabled = true;
            for(ClientConnection client: clientConnectionList){
                Log.d(TAG, "Attempting to enable a buzzer");
                client.changeBuzzerState(true);
            }
        }
    }

    public void startDisableOthersThread(int callingClientID){
        new Thread(new DisableOthersThread(callingClientID)).start();
    }

    public class DisableOthersThread implements Runnable {
        int callingClientID;

        DisableOthersThread(int callingClientID){
            this.callingClientID = callingClientID;
        }

        @Override
        public void run() {
            for(ClientConnection client: clientConnectionList){
                if(client.clientID != callingClientID) {
                    client.changeBuzzerState(false);
                }else{
                    client.sendBuzzWin();
                }
            }
        }
    }

    public class ClientConnection {
        private Thread listenerThread;
        private Socket socket;
        private PrintWriter out;
        public int clientID;

        public ClientConnection(int id, Socket socket){
            Log.d(TAG, "ClientConnection() constructor");

            clientID = id;
            this.socket = socket;

            CommunicationThread commThread = new CommunicationThread(clientID, this.socket);
            listenerThread = new Thread(commThread);
            listenerThread.start();
            Log.d(TAG, "ClientConnection() listenerThread started");

            try {
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void changeBuzzerState(final boolean enabled){
            new Thread(new Runnable() {
                public void run() {
                    if(enabled == true){
                        Log.d(TAG, "changeBuzzerState(): Enable");
                        out.println(MainActivity.MSG_ENABLE);
                    }else{
                        Log.d(TAG, "changeBuzzerState(): Disable");
                        out.println(MainActivity.MSG_DISABLE);
                    }

                }
            }).start();
        }

        public void sendBuzzWin(){
            new Thread(new Runnable() {
                public void run() {
                    Log.d(TAG, "Sending buzz win notification");
                    out.println(MainActivity.MSG_BUZZ_WIN);
                }
            }).start();
        }

        public boolean destroy(){
            listenerThread.interrupt();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
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
                    Log.d(TAG, "Listening for socket connection");
                    socket = serverSocket.accept();
                    Log.d(TAG, "Accepted socket");
                    ClientConnection newClient = new ClientConnection(nextClientID++, socket);
                    clientConnectionList.add(newClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Server thread stopped");
        }
    }

    class CommunicationThread implements Runnable {
        private int clientID;
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(int clientID, Socket clientSocket) {
            this.clientID = clientID;
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
                    Log.d(TAG, "CommunicationThread() running, listening on socket");
                    String read = input.readLine();
                    if(read == null){
                        // Connection was terminated, exit the loop
                        break;
                    }
                    updateConversationHandler.post(new updateUIThread(clientID, read));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "CommunicationThread() interrupted, exiting");
        }
    }

    class updateUIThread implements Runnable {
        private String msg;
        private int clientID;

        public updateUIThread(int clientID, String str) {
            this.msg = str;
            this.clientID = clientID;
        }

        @Override
        public void run() {
            if(msg.equals(MainActivity.MSG_BUZZ_REQUEST) && buzzersEnabled){
                // This should be thread-safe since it runs on the UI thread
                buzzersEnabled = false;
                startDisableOthersThread(clientID);
            }
            makeText(msg);
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
