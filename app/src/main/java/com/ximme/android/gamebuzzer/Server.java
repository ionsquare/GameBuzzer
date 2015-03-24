package com.ximme.android.gamebuzzer;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by matthew on 2015-03-23.
 */
public class Server {
    private static final String TAG = Server.class.getSimpleName();

    private int server_port;
    private Handler mHandler;

    private Thread serverThread;
    private Runnable newConnectionRunnable = null;
    private ServerSocket serverSocket = null;
    private int nextClientID = 0;
    private ArrayList<ClientConnection> clientConnectionList = new ArrayList<>();

    public Server(int server_port, Handler handler){
        this.server_port = server_port;
        mHandler = handler;
    }

    public void setNewConnectionRunnable(Runnable runnable){
        newConnectionRunnable = runnable;
    }

    public void startServer(){
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    serverSocket = new ServerSocket(MainActivity.SERVERPORT);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Log.d(TAG, "Listening for socket connection");
                        socket = serverSocket.accept();
                        Log.d(TAG, "Accepted socket");
                        ClientConnection newClient = new ClientConnection(nextClientID++, socket);
                        clientConnectionList.add(newClient);
                        if(newConnectionRunnable != null) {
                            mHandler.post(newConnectionRunnable);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "Server thread finished");
            }
        });
        serverThread.start();
    }

    public void stopServer(){
        // Close the server socket
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing serverSocket");
            e.printStackTrace();
        }

        // Stop the server thread
        // TODO Do I need to also interrupt the thread? Or will it complete automatically when the socket is closed?
        //serverThread.interrupt();
        //Log.d(TAG, "Server thread stopped");

        // Close all client connections
        for(ClientConnection client: clientConnectionList){
            Log.d(TAG, "Attempting to enable a buzzer");
            try {
                client.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // TODO Do I need to also interrupt the thread? Or will it complete automatically when the socket is closed?
            // client.listenerThread.interrupt();
        }
    }

    private class ClientConnection {
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
                    mHandler.post(new updateUIThread(clientID, read));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "CommunicationThread() interrupted, exiting");
        }
    }
}
