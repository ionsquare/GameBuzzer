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

    public static final String ARG_EVENT_TYPE = "server.event_type";
    public static final String EVENT_CLIENT_CONNECT = "server.event.client_connect";
    public static final String EVENT_CLIENT_DISCONNECT = "server.event.client_disconnect";
    public static final String EVENT_MESSAGE_RECEIVED = "server.event.message_received";
    public static final String EVENT_RECEIVED_BROADCAST = "server.event.received_broadcast";
    public static final String EVENT_CONNECTION_LOST = "server.event.connection_lost";

    public static final String ARG_CLIENT_ID = "server.arg.client_id";
    public static final String ARG_MESSAGE = "server.arg.message";
    public static final String ARG_CLIENT_ADDRESS = "server.arg.client_address";

    public static final String MSG_KEEP_ALIVE = "server.msg.keep_alive";

    private static final int INTERVAL_BROADCAST = 1000;     // Milliseconds
    private static final int INTERVAL_KEEP_ALIVE = 5000;     // Milliseconds

    private Handler mHandler;

    private Thread broadcastThread = null;
    private Thread broadcastListenerThread = null;
    private Thread keepAliveThread = null;
    private DatagramSocket broadcastListenerSocket = null;
    private Thread serverThread = null;
    private ServerSocket serverSocket = null;
    private int nextClientID = 0;
    private HashMap<Integer, ClientConnection> clientConnectionList;

    // For client use
    private Socket hostConnectionSocket = null;
    private Thread hostListenerThread = null;

    public GameServer(Handler handler) {
        mHandler = handler;
    }

    /**
     * Starts a server to listen for new connections
     *
     * @param server_port    Port to listen for connections on
     */
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
                clientConnectionList = new HashMap<>();
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Log.d(TAG, "Listening for socket connection");
                        socket = serverSocket.accept();
                        Log.d(TAG, "Accepted socket");
                        ClientConnection newClient = new ClientConnection(nextClientID++, socket);
                        clientConnectionList.put(newClient.clientID, newClient);

                        data = new Bundle();
                        data.putInt(ARG_CLIENT_ID, newClient.clientID);
                        data.putString(ARG_EVENT_TYPE, EVENT_CLIENT_CONNECT);

                        msg = new Message();
                        msg.setData(data);
                        mHandler.sendMessage(msg);

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
        //startKeepAliveThread();
    }

    /**
     * Stops the server that listens for new client connections
     */
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
            Log.d(TAG, "Disconnecting from client");
            client.getValue().disconnect();
            Log.d(TAG, "Disconnected");
            // TODO Do I need to also interrupt the thread? Or will it complete automatically when the socket is closed?
            // client.listenerThread.interrupt();
        }
        //stopKeepAliveThread();
    }

    /**
     * Starts broadcasting broadcastMessage on the broadcast IP
     * This is used to let clients know where the host is located so they can initiate a connection
     *
     * @param broadcastMessage    The message to broadcast
     * @param broadcastPort       Port on which to broadcast
     */
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
                        Thread.sleep(INTERVAL_BROADCAST);
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

    /**
     * Stop broadcasting
     */
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

    /**
     * Start listening for broadcasts
     *
     * @param port    The port on which to listen for broadcasts
     */
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
                            data.putString(ARG_EVENT_TYPE, EVENT_RECEIVED_BROADCAST);
                            data.putString(ARG_MESSAGE, msgReceived);

                            message = new Message();
                            message.setData(data);

                            mHandler.sendMessage(message);
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

    /**
     * Stop listening for broadcasts
     */
    public void stopBroadcastListener(){
        Log.d(TAG, "stopBraodcastListener()");
        broadcastListenerSocket.close();
    }

    /**
     * Send a message to a client with which a connection has already been established
     *
     * @param clientID    ID of the client to send the message to
     * @param msg         The message to send to the client
     */
    public void sendMessageToClient(final int clientID, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Sending message to client: " + clientID);
                clientConnectionList.get(clientID).sendMessage(msg);
            }
        }).start();
    }

    /**
     * Returns a list of the IDs of the clients currently connected to
     *
     * @return the set of client IDs currently connected to
     */
    public Set<Integer> getClientIDList(){
        return clientConnectionList.keySet();
    }

    /**
     * For use by clients to establish connection to host
     *
     * @param address    Address of the host
     * @param port       Port that the host listens for new connections on
     */
    public void initHostConnection(final InetAddress address, final int port){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    hostConnectionSocket = new Socket(address, port);
                    Log.d(TAG, "initConnection() Socket opened");

                    // TODO Maybe this thread does not need to be kept in a variable
                    hostListenerThread = new Thread(new ListenerThread(hostConnectionSocket));
                    hostListenerThread.start();
                    Log.d(TAG, "initConnection() listenerThread started");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void closeHostConnection(){
        try {
            hostConnectionSocket.close();
            hostConnectionSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the host
     *
     * @param msg    Message to send to the host
     */
    public void sendMessageToHost(final String msg){
        if(hostConnectionSocket == null || hostConnectionSocket.isClosed()){
            Log.e(TAG, "sendMessageToHost() cannot send message, host socket is null or closed");
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(hostConnectionSocket.getOutputStream())),
                            true);
                    out.println(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startKeepAliveThread(){
        keepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Set<Integer> clientIDList;
                
                while(!Thread.currentThread().isInterrupted()) {
                    clientIDList = getClientIDList();
                    for (int clientID : clientIDList) {
                        Log.d(TAG, "Keep-alive for client #" + clientID);
                        ClientConnection client = clientConnectionList.get(clientID);
                        if (client.sendMessage(MSG_KEEP_ALIVE)) {
                            // An I/O error occurred, remove the client from the list
                            Log.d(TAG, "Disconnecting from client");
                            client.disconnect();
                            clientConnectionList.remove(clientID);
                        }
                    }
                    try {
                        Thread.sleep(INTERVAL_KEEP_ALIVE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.e(TAG, "Keep alive thread stopping");
            }
        });
        keepAliveThread.start();
    }

    private void stopKeepAliveThread(){
        keepAliveThread.interrupt();
    }

    /**
     * Listener thread for use by clients - listens for messages from host and sends events to the
     * handler.
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
            Bundle data;
            Message message;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Log.d(TAG, "ListenerThread waiting for message on socket...");
                    String read = input.readLine();
                    if(read == null){
                        // Connection was terminated, notify handler and exit the loop
                        data = new Bundle();
                        data.putString(ARG_EVENT_TYPE, EVENT_CONNECTION_LOST);

                        message = new Message();
                        message.setData(data);

                        mHandler.sendMessage(message);

                        Log.w(TAG, "Listener thread lost connection (read == null)");
                        break;
                    }
                    Log.d(TAG, "ListenerThread received message: " + read);
                    data = new Bundle();
                    data.putString(ARG_EVENT_TYPE, EVENT_MESSAGE_RECEIVED);
                    data.putString(ARG_MESSAGE, read);

                    message = new Message();
                    message.setData(data);

                    mHandler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            Log.d(TAG, "ListenerThread exiting");
        }
    }

    /**
     * Sends broadcast message
     * Must not be run on the UI thread
     *
     * @param broadcastMessage    Message to broadcast
     * @param broadcastPort       Port on which to broadcast to
     */
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

        /**
         * Returns true if the BufferedWriter encounters an I/O error
         *
         * @param msg    Message to send to the host
         * @return true if there was an I/O error, false otherwise
         */
        public boolean sendMessage(String msg) {
            out.println(msg);
            return out.checkError();
        }

        public boolean disconnect() {
            Log.d(TAG, "ClientConnection.disconnect()");
            try {
                socket.close();
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
                    Log.d(TAG, "SocketListenerThread.run(): listening on socket");

                    String read = input.readLine();
                    if (read == null) {
                        // Connection was terminated, exit the loop

                        // Remove client from client list
                        clientConnectionList.remove(clientID);

                        // Notify handler of disconnect event
                        data = new Bundle();
                        data.putString(ARG_EVENT_TYPE, EVENT_CLIENT_DISCONNECT);
                        data.putInt(GameServer.ARG_CLIENT_ID, clientID);

                        msg = new Message();
                        msg.setData(data);
                        mHandler.sendMessage(msg);
                        break;
                    }

                    data = new Bundle();
                    data.putString(ARG_EVENT_TYPE, EVENT_MESSAGE_RECEIVED);
                    data.putInt(GameServer.ARG_CLIENT_ID, clientID);
                    data.putString(GameServer.ARG_MESSAGE, read);

                    msg = new Message();
                    msg.setData(data);
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            Log.d(TAG, "SocketListenerThread.run() completed");
        }
    }
}
