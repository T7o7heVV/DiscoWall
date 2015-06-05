package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;

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
                sendMessage(NetfilterBridgeProtocol.Comment.MSG_PREFIX, "DiscoWall App says hello.");
                firstMessage = false;
                continue;
            }

            handleReceivedMessage(message);
        }
    }

    private void sendMessage(String prefix, String message) {
        socketOut.println(prefix + message);
        socketOut.flush();
    }

    private void handleReceivedMessage(String message) {
        if (message.startsWith(NetfilterBridgeProtocol.QueryPackageAction.MSG_PREFIX)) {
            // Example: #Packet.QueryAction##protocol=tcp##ip.src=192.168.178.28##ip.dst=173.194.116.159##tcp.src.port=35251##tcp.dst.port=80#

            // Handling of different protocols - currently TCP/UDP
            if (message.contains(NetfilterBridgeProtocol.QueryPackageAction.IP.PROTOCOL_TYPE_TCP)) {
                // Handle TCP Package

            } else if (message.contains(NetfilterBridgeProtocol.QueryPackageAction.IP.PROTOCOL_TYPE_UDP)) {
                // Handle UDP  Package

            }
        } else if (message.startsWith(NetfilterBridgeProtocol.Comment.MSG_PREFIX)) {
            String comment = message.substring(message.indexOf(NetfilterBridgeProtocol.Comment.MSG_PREFIX));
            Log.v(LOG_TAG, "Comment received: " + comment);
        } else {
            Log.e(LOG_TAG, "Unknown message format: " + message);
        }
    }

    private String extractValueFromMessage(String valuePrefix, String valueSuffix) {
        return null;
    }

    private void onPackageReceived(String sourceIP, String destinationIP, int sourcePort, int destinationPort) {

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
