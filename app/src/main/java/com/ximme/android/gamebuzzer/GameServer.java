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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by matthew on 2015-03-23.
 */
public class GameServer {
    private static final String TAG = GameServer.class.getSimpleName();

    public static final String EVENT_TYPE = "Server.event_type";
    public static final String EVENT_CLIENT_CONNECT = "Server.event.client_connect";
    public static final String EVENT_CLIENT_DISCONNECT = "Server.event.client_disconnect";
    public static final String EVENT_MESSAGE_RECEIVED = "Server.event.message_received";
    public static final String EVENT_RECEIVED_BROADCAST = "Server.event.received_broadcast";

    public static final String ARG_CLIENT_ID = "Server.arg.client_id";
    public static final String ARG_MESSAGE = "Server.arg.message";
    public static final String ARG_CLIENT_ADDRESS = "Server.arg.client_address";

    private static final int BROADCAST_INTERVAL = 1000;     // Milliseconds

    private Handler mHandler;

    private Thread broadcastThread = null;
    private Thread broadcastListenerThread = null;
    private DatagramSocket broadcastListenerSocket = null;
    private Thread serverThread = null;
    private ServerSocket serverSocket = null;
    private int nextClientID = 0;
    private HashMap<Integer, ClientConnection> clientConnectionList = new HashMap<>();

    public GameServer(Handler handler) {
        mHandler = handler;
    }

    /*
    public void setNewConnectionRunnable(Runnable runnable){
        newConnectionRunnable = runnable;
    }
    //*/

    public void startServer(final int server_port) {
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                Bundle data;
                Message msg;

                try {
                    serverSocket = new ServerSocket(server_port);
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
                        clientConnectionList.put(newClient.clientID, newClient);

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
                        break;
                    }
                }
                Log.d(TAG, "Server thread finished");
            }
        });
        serverThread.start();
    }

    public void stopServer() {
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
        for (Map.Entry<Integer, ClientConnection> client : clientConnectionList.entrySet()) {
            Log.d(TAG, "Attempting to enable a buzzer");
            client.getValue().disconnect();
            // TODO Do I need to also interrupt the thread? Or will it complete automatically when the socket is closed?
            // client.listenerThread.interrupt();
        }
    }

    public void startBroadcast(final String broadcastMessage, final int broadcastPort) {
        if(broadcastThread != null){
            Log.e(TAG, "startBroadcast() broadcastThread is not null indicating it is already running, this shouldn't ever happen!");
            broadcastThread.interrupt();
        }
        broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "startBroadcast()");
                while (!Thread.currentThread().isInterrupted()) {
                    sendBroadcast(broadcastMessage, broadcastPort);
                    try {
                        Thread.sleep(BROADCAST_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.w(TAG, "startBroadcast() Couldn't sleep - thread must have been interrupted");
                        break;
                    }
                }
                Log.d(TAG, "startBroadcast() finished, thread must have been interrupted");
            }
        });
        broadcastThread.start();
    }

    public void stopBroadcast(){
        Log.d(TAG, "stopBroadcast()");
        if(broadcastThread != null) {
            broadcastThread.interrupt();
            broadcastThread = null;
            Log.d(TAG, "stopBroadcast() finished");
        }else {
            Log.d(TAG, "stopBroadcast() broadcastThread was null");
        }
    }

    public void startBroadcastListener(final int port){
        broadcastListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String clientAddress;
                DatagramPacket packet;
                Bundle data;
                Message message;

                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        InetAddress broadcastAddress = InetAddress.getByName(Utils.getBroadcastIP());
                        broadcastListenerSocket = new DatagramSocket(port, broadcastAddress);
                        byte[] recvBuf = new byte[15000];

                        while(!Thread.currentThread().isInterrupted()) {
                            packet = new DatagramPacket(recvBuf, recvBuf.length);
                            Log.d(TAG, "startBroadcastListener() broadcastListenerSocket.receive()");
                            broadcastListenerSocket.receive(packet);
                            if(broadcastListenerSocket.isClosed()){
                                Log.d(TAG, "startBroadcastListener(): broadcastListenerSocket was closed");
                                return;
                            }

                            // Packet received
                            // Get host info
                            clientAddress = packet.getAddress().getHostAddress();
                            Log.i(TAG, "Packet received from: " + clientAddress);

                            String msgReceived = new String(packet.getData()).trim();
                            Log.i(TAG, "Packet received:  " + msgReceived);

                            data = new Bundle();
                            data.putString(ARG_CLIENT_ADDRESS, clientAddress);
                            data.putString(EVENT_TYPE, EVENT_RECEIVED_BROADCAST);
                            data.putString(ARG_MESSAGE, msgReceived);

                            message = new Message();
                            message.setData(data);

                            mHandler.handleMessage(message);
                        }

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (SocketException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        broadcastListenerThread.start();
    }

    public void stopBroadcastListener(){
        Log.d(TAG, "stopBraodcastListener()");
        broadcastListenerSocket.close();
    }

    public void sendMessageToClient(final int clientID, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Sending message to client: " + clientID);
                clientConnectionList.get(clientID).sendMessage(msg);
            }
        });
    }

    public void sendMessageToAddress(final String address, final String msg){

    }

    public Set<Integer> getClientIDList(){
        return clientConnectionList.keySet();
    }

    private void sendBroadcast(String broadcastMessage, int broadcastPort) {
        try {
            DatagramSocket broadcastSocket = new DatagramSocket();

            broadcastSocket.setBroadcast(true);
            // TODO not necessary to get the broadcast IP each time - more efficient to get it once and save
            InetAddress address = InetAddress.getByName(Utils.getBroadcastIP());

            byte[] sendData;

            sendData = broadcastMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, broadcastPort);
            broadcastSocket.send(packet);
            Log.d(TAG, "sendBroadcast() broadcast sent");

            broadcastSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ClientConnection {
        private Thread listenerThread;
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        public int clientID;

        public ClientConnection(int id, Socket socket) {
            Log.d(TAG, "ClientConnection() constructor");

            clientID = id;
            this.socket = socket;

            try {
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }


            SocketListenerThread commThread = new SocketListenerThread(clientID, in);
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

        public void sendMessage(String msg) {
            out.println(msg);
        }

        public boolean disconnect() {
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

    class SocketListenerThread implements Runnable {
        private int clientID;
        private Socket clientSocket;
        private BufferedReader input;

        public SocketListenerThread(int clientID, BufferedReader reader) {
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
                    if (read == null) {
                        // Connection was terminated, exit the loop
                        data = new Bundle();
                        data.putString(EVENT_TYPE, EVENT_CLIENT_DISCONNECT);
                        data.putInt(GameServer.ARG_CLIENT_ID, clientID);

                        msg = new Message();
                        msg.setData(data);
                        mHandler.handleMessage(msg);
                        break;
                    }

                    data = new Bundle();
                    data.putString(EVENT_TYPE, EVENT_MESSAGE_RECEIVED);
                    data.putInt(GameServer.ARG_CLIENT_ID, clientID);
                    data.putString(GameServer.ARG_MESSAGE, read);

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
