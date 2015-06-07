package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NetfilterBridgeCommunicator implements Runnable {
    public static interface EventsHandler {
        boolean onPackageReceived(NetfilterBridgePackages.TransportLayerPackage tlPackage);

        /**
         * This method should NEVER be called. It only exists to make debugging simpler, so that errors do not get stuck within LOGCAT only.
         * @param e
         * @return
         */
        void onInternalERROR(String message, Exception e);
    }

    private static final String LOG_TAG = "NfBridgeCommunicator";
    public final int listeningPort;
    private final EventsHandler eventsHandler;

    private volatile boolean runCommunicationLoop;
    private volatile boolean connected;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private IOException connectionException;
    private PrintWriter socketOut;
    private BufferedReader socketIn;

    public NetfilterBridgeCommunicator(EventsHandler eventsHandler, int listeningPort) {
        this.eventsHandler = eventsHandler;
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
        Log.d(LOG_TAG, "sendMessage(): " + prefix + message);
        socketOut.println(prefix + message);
        socketOut.flush();
    }

    private void handleReceivedMessage(final String message) {
        if (message.startsWith(NetfilterBridgeProtocol.QueryPackageAction.MSG_PREFIX)) {
            // Example: #Packet.QueryAction##protocol=tcp##ip.src=192.168.178.28##ip.dst=173.194.116.159##tcp.src.port=35251##tcp.dst.port=80#

            NetfilterBridgePackages.TransportLayerPackage tlPackage;

            try {
                String srcIP = extractStringValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.VALUE_SOURCE);
                String dstIP = extractStringValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.VALUE_DESTINATION);

                // Handling of different protocols - currently TCP/UDP
                if (message.contains(NetfilterBridgeProtocol.QueryPackageAction.IP.FLAG_PROTOCOL_TYPE_TCP)) {
                    // Handle TCP Package
                    int srcPort = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_SOURCE_PORT);
                    int dstPort = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_DESTINATION_PORT);
                    int length = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_LENGTH);
                    int checksum = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_CHECKSUM);
                    int seqNumber = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_SEQUENCE_NUMBER);
                    int ackNumber = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_ACK_NUMBER);
                    boolean hasFlagACK = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_IS_ACK);
                    boolean hasFlagFIN = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_FIN);
                    boolean hasFlagSYN = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_SYN);
                    boolean hasFlagPush = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_PUSH);
                    boolean hasFlagReset = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_RESET);
                    boolean hasFlagUrgent = extractBitValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.TCP.VALUE_FLAG_URGENT);

                    tlPackage = new NetfilterBridgePackages.TcpPackage(srcIP, dstIP, srcPort, dstPort, length, checksum,
                            seqNumber, ackNumber,
                            hasFlagACK, hasFlagFIN, hasFlagSYN, hasFlagPush, hasFlagReset, hasFlagUrgent
                        );
                } else if (message.contains(NetfilterBridgeProtocol.QueryPackageAction.IP.FLAG_PROTOCOL_TYPE_UDP)) {
                    // Handle UDP  Package
                    int srcPort = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.UDP.VALUE_SOURCE_PORT);
                    int dstPort = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.UDP.VALUE_DESTINATION_PORT);
                    int length = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.UDP.VALUE_LENGTH);
                    int checksum = extractIntValueFromMessage(message, NetfilterBridgeProtocol.QueryPackageAction.IP.UDP.VALUE_CHECKSUM);

                    tlPackage = new NetfilterBridgePackages.UdpPackage(srcIP, dstIP, srcPort, dstPort, length, checksum);
                } else {
                    Log.e(LOG_TAG, "Unknown message format (no transport-layer defined): " + message);
                    NetfilterBridgeProtocol.ProtocolFormatException formatException = new NetfilterBridgeProtocol.ProtocolFormatException("Unknown message format: no transport-layer defined", message);
                    eventsHandler.onInternalERROR(message, formatException);
                    return;
                }
            } catch(NetfilterBridgeProtocol.ProtocolException e) {
                Log.e(LOG_TAG, "Error while decoding message: " + message + "\n" + e.getMessage());
                eventsHandler.onInternalERROR("Error while decoding message: " + message + "\n" + e.getMessage(), e);
                return;
            }

            Log.v(LOG_TAG, "Decoded package-information: " + tlPackage);

            // React to received package
            onPackageReceived(tlPackage);
        } else if (message.startsWith(NetfilterBridgeProtocol.Comment.MSG_PREFIX)) {
            String comment = message.substring(message.indexOf(NetfilterBridgeProtocol.Comment.MSG_PREFIX));
            Log.v(LOG_TAG, "Comment received: " + comment);
        } else {
            Log.e(LOG_TAG, "Unknown message format: " + message);
        }
    }

    private boolean extractBitValueFromMessage(final String message, final String valueName) throws NetfilterBridgeProtocol.ProtocolValueException {
        int value = extractIntValueFromMessage(message, valueName);
        if (value != 0 && value != 1)
            throw new NetfilterBridgeProtocol.ProtocolValueException(message, value + "", message);

        return value == 0;
    }

    private int extractIntValueFromMessage(final String message, final String valueName) throws NetfilterBridgeProtocol.ProtocolValueMissingException, NetfilterBridgeProtocol.ProtocolValueTypeException {
        String intValueStr = extractStringValueFromMessage(message, valueName);

        try {
            return Integer.parseInt(intValueStr);
        } catch(Exception e) {
            throw new NetfilterBridgeProtocol.ProtocolValueTypeException(Integer.class, intValueStr, message);
        }
    }

    private String extractStringValueFromMessage(final String message, final String valueName) throws NetfilterBridgeProtocol.ProtocolValueMissingException {
        String valuePrefix = NetfilterBridgeProtocol.VALUE_PREFIX + valueName + NetfilterBridgeProtocol.VALUE_KEY_DELIM;
        String valueSuffix = NetfilterBridgeProtocol.VALUE_SUFFIX;

        if (! (message.contains(valuePrefix) && message.contains(valueSuffix)))
            throw new NetfilterBridgeProtocol.ProtocolValueMissingException(valueName, message);

        String messageStartingWithValue = message.substring(message.indexOf(valuePrefix) + valuePrefix.length());
        if (!messageStartingWithValue.contains(valueSuffix)) // checking again, in case the suffix is a substring of the prefix
            throw new NetfilterBridgeProtocol.ProtocolValueMissingException(valueName, message);

        return messageStartingWithValue.substring(0, messageStartingWithValue.indexOf(valueSuffix));
    }

    private void onPackageReceived(NetfilterBridgePackages.TransportLayerPackage tlPackage) {
        boolean acceptPackage = eventsHandler.onPackageReceived(tlPackage);

        if (acceptPackage) {
            Log.v(LOG_TAG, "Accepting package: " + tlPackage);
            sendMessage(NetfilterBridgeProtocol.QueryPackageActionResponse.MSG_PREFIX, NetfilterBridgeProtocol.QueryPackageActionResponse.FLAG_ACCEPT_PACKAGE);
//            sendMessage("#Packet.QueryAction.Resonse#", "#ACCEPT#");
        } else {
            Log.v(LOG_TAG, "Dropping package: " + tlPackage);
            sendMessage(NetfilterBridgeProtocol.QueryPackageActionResponse.MSG_PREFIX, NetfilterBridgeProtocol.QueryPackageActionResponse.FLAG_DROP_PACKAGE);
//            sendMessage("#Packet.QueryAction.Resonse#", "#DROP#");
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
