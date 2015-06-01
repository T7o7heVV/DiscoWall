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
            Socket clientSocket = serverSocket.accept();

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
    }

    /**
     * Is being called within the thread by {@link #run} and stopy when {@link #runCommunicationLoop} is set to false,
     * or when an exception occurrs.
     */
    private void communicate() throws IOException {
        runCommunicationLoop = true;

        while (runCommunicationLoop) {
            String message = socketIn.readLine();
            Log.d(LOG_TAG, "message received: " + message);

        }
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
