package de.uni_kl.informatik.disco.discowall.netfilter.dnsCache;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;

public class DnsCache {
    private static final String LOG_TAG = DnsCache.class.getSimpleName();
    private static final int dnsServerPort = 53;

    private int cacheListeningPort;
    private InetAddress dnsServer;
    private volatile boolean listen;
    private IOException listeningThreadException;

    public DnsCache(int cacheListeningPort, String dnsServer) throws UnknownHostException, IOException {
        this(cacheListeningPort, InetAddress.getByName(dnsServer));
    }

    public DnsCache(final int cacheListeningPort, final InetAddress dnsServer) throws IOException {
        this.cacheListeningPort = cacheListeningPort;
        this.dnsServer = dnsServer;

        Thread listeningThread = new Thread() {
            @Override
            public void run() {
                try {
                    Log.i(LOG_TAG, "Starting DnsCache [cached server = "+dnsServer+"] on port: " + cacheListeningPort);
                    startServer();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Could not start DnsCache listening server on port: " + cacheListeningPort, e);
                    listen = false;
                    listeningThreadException = e;
                }
            }
        };

        listeningThread.setDaemon(true);
        listeningThread.start();

        createIptableRules();
    }

    public IOException getListeningThreadException() {
        return listeningThreadException;
    }

    public static int getDnsServerPort() {
        return dnsServerPort;
    }

    public int getCacheListeningPort() {
        return cacheListeningPort;
    }

    public InetAddress getDnsServer() {
        return dnsServer;
    }

    public boolean isCacheRunning() {
        return listen;
    }

    public void stopCache() {
        listen = false;
        removeIptableRules();
    }

    private void createIptableRules() {
        // 1. Exclude all udp packages of this app from bein redirected
//        IptablesControl.ruleAdd()

        // 2. Redirect all outgoing udp-packages to system-dns servers (matching by ip and port 53)
    }

    private void removeIptableRules() {

    }

    private void startServer() throws IOException {
        listen = true;

        final DatagramSocket serverSocket = new DatagramSocket(cacheListeningPort);
        byte[] receiveData = new byte[1024];

        while(listen) {
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            System.out.println("Waiting for DNS query...");

            serverSocket.receive(receivePacket); // blocks until input available, i.e. a datagram received

            Thread threadedResponse = new Thread() {
                public void run() {
                    try {
                        String dnsQuery = new String(receivePacket.getData());
                        System.out.println("RECEIVED: " + dnsQuery);

                        InetAddress clientAddress = receivePacket.getAddress();
                        int clientPort = receivePacket.getPort();
                        System.out.println("[from: " + clientAddress +":" + clientPort + "]");

                        String dnsResponse = sendMessageAndReceiveAnswer(dnsServer, dnsServerPort, dnsQuery);

                        byte[] dnsResponseBackToClient = new byte[1024];
                        dnsResponseBackToClient = dnsResponse.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(dnsResponseBackToClient, dnsResponseBackToClient.length, clientAddress, clientPort);

                        synchronized(serverSocket) {
                            serverSocket.send(sendPacket);
                        }
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                };
            };

            threadedResponse.setDaemon(true);
            threadedResponse.start();
        }

        serverSocket.close();
    }

    private static String sendMessageAndReceiveAnswer(InetAddress receiver, int receiverPort, String message) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket();
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiver, receiverPort);

        System.out.println("Forwarding DNS request to server: " + receiver + ":" + receiverPort);
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String answer = new String(receivePacket.getData());
        System.out.println("FROM SERVER:" + answer);

        clientSocket.close();

        return answer;
    }

}
