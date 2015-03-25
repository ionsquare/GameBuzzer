package com.ximme.android.gamebuzzer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    private static final String EVENT_TYPE = "Server.event_type";
    private static final String EVENT_CLIENT_CONNECT = "Server.event.client_connect";
    private static final String EVENT_CLIENT_DISCONNECT = "Server.event.client_disconnect";
    private static final String EVENT_MESSAGE_RECEIVED = "Server.event.message_received";

    private static final String ARG_CLIENT_ID = "Server.arg.client_id";
    private static final String ARG_MESSAGE = "Server.arg.message";

    private int server_port;
    private Handler mHandler;

    private Thread serverThread;
    //private Runnable newConnectionRunnable = null;
    private ServerSocket serverSocket = null;
    private int nextClientID = 0;
    private ArrayList<ClientConnection> clientConnectionList = new ArrayList<>();

    public Server(int server_port, Handler handler){
        this.server_port = server_port;
        mHandler = handler;
    }

    /*
    public void setNewConnectionRunnable(Runnable runnable){
        newConnectionRunnable = runnable;
    }
    //*/

    public void startServer(){
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                Bundle data;
                Message msg;

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

                        data = new Bundle();
                        data.putInt(ARG_CLIENT_ID, newClient.clientID);
                        data.putString(EVENT_TYPE, EVENT_CLIENT_CONNECT);

                        msg = new Message();
                        msg.setData(data);
                        mHandler.handleMessage(msg);

                        /*
                        if(newConnectionRunnable != null) {
                            mHandler.post(newConnectionRunnable);
                        }
                        //*/
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
        private BufferedReader in;
        public int clientID;

        public ClientConnection(int id, Socket socket){
            Log.d(TAG, "ClientConnection() constructor");

            clientID = id;
            this.socket = socket;

            try {
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }


            CommunicationThread commThread = new CommunicationThread(clientID, in);
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

        public boolean destroy(){
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            // TODO Check if the thread actually needs to be interrupted
            // It might finish on its own after the socket gets closed
            //listenerThread.interrupt();

            // TODO For fun, check what happened to out and socket
            // Might need to close them too

            return true;
        }
    }

    class CommunicationThread implements Runnable {
        private int clientID;
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(int clientID, BufferedReader reader) {
            this.clientID = clientID;
            this.input = reader;
        }

        @Override
        public void run() {
            Message msg;
            Bundle data;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Log.d(TAG, "CommunicationThread.run(): listening on socket");

                    String read = input.readLine();
                    if(read == null){
                        // Connection was terminated, exit the loop
                        data = new Bundle();
                        data.putString(EVENT_TYPE, EVENT_CLIENT_DISCONNECT);
                        data.putInt(Server.ARG_CLIENT_ID, clientID);

                        msg = new Message();
                        msg.setData(data);
                        mHandler.handleMessage(msg);
                        break;
                    }

                    data = new Bundle();
                    data.putString(EVENT_TYPE, EVENT_MESSAGE_RECEIVED);
                    data.putInt(Server.ARG_CLIENT_ID, clientID);
                    data.putString(Server.ARG_MESSAGE, read);

                    msg = new Message();
                    msg.setData(data);
                    mHandler.handleMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "CommunicationThread.run() completed");
        }
    }
}
