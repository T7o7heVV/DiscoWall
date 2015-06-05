package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NetfilterBridgeCommunicator implements Runnable {
    private static final String LOG_TAG = "NfBridgeCommunicator";
    public final int listeningPort;

    private volatile boolean runCommunicationLoop;
    private volatile boolean connected;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private IOException connectionException;
    private PrintWriter socketOut;
    private BufferedReader socketIn;

    public NetfilterBridgeCommunicator(int listeningPort) {
        this.listeningPort = listeningPort;

        Log.v(LOG_TAG, "starting listening thread...");
        new Thread(this).start();
    }

    @Override
    public void run() {
        connected = false;

        try {
            Log.v(LOG_TAG, "opening listening port: " + listeningPort);
            serverSocket = new ServerSocket(listeningPort);

            Log.v(LOG_TAG, "waiting for client...");
            clientSocket = serverSocket.accept();

            Log.v(LOG_TAG, "client (netfilter bridge) connected.");
            socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
            socketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            Log.v(LOG_TAG, "IO streams connected.");

            connected = true;

            Log.v(LOG_TAG, "starting communication loop...");
            communicate();
            Log.v(LOG_TAG, "communication loop terminated.");

            Log.v(LOG_TAG, "closing listening port: " + listeningPort);
            serverSocket.close();
        } catch (IOException e) {
            connectionException = e;

            Log.e(LOG_TAG, "netfilter bridge connection closed with exception: " + e.getMessage());
            e.printStackTrace(); // will print to Log.i()
        } finally {
            connected = false;
        }

        if (runCommunicationLoop) {
            Log.d(LOG_TAG, "client disconnected. Reopening socket...");
            run();
        } else {
            Log.d(LOG_TAG, "communication loop terminated.");
        }
    }

    /**
     * Is being called within the thread by {@link #run} and stopy when {@link #runCommunicationLoop} is set to false,
     * or when an exception occurrs.
     */
    private void communicate() throws IOException {
        runCommunicationLoop = true;
        boolean firstMessage = true;

        while (runCommunicationLoop
                && clientSocket.isBound()
                && clientSocket.isConnected()
                && !clientSocket.isClosed()
                && !clientSocket.isInputShutdown()
                && !clientSocket.isOutputShutdown()
               ) {

            String message = socketIn.readLine();
            Log.v(LOG_TAG, "raw message received: " + message);

            if (message == null) {
                Log.d(LOG_TAG, "value 'null' received. Closing connection and waiting for new client.");
                break;
            }

            if (firstMessage) {
                sendMessage(DiscoWallBridgeProtocoll.Comment.MSG_PREFIX, "DiscoWall App says hello.");
                firstMessage = false;
            }

            onMessageReceived(message);
        }
    }

    private void sendMessage(String prefix, String message) {
        socketOut.println(prefix + message);
        socketOut.flush();
    }

    private static class DiscoWallBridgeProtocoll {
        public static class Comment {
            public static final String MSG_PREFIX = "#COMMENT#";
        }

        public static class QueryPackageAction {
            public static final String MSG_PREFIX = "#QUERY:PACKAGE-ACTION#";
        }
    }

    private void onMessageReceived(String message) {

    }

    public boolean isConnected() {
        return connected;
    }

    public IOException getConnectionException() {
        return connectionException;
    }

    public void disconnect() throws IOException {
        runCommunicationLoop = false;
    }

}
