package de.uni_kl.informatik.disco.discowall.packages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Timestamp;

public class Packages {
    public enum PackageType { TCP, UDP }

    public static class IpPortPair {
        private final String ip;
        private final int port;

        public int getPort() { return port; }
        public String getIp() { return ip; }

        public IpPortPair(String ip, int port) {
            if (ip == null)
                throw new IllegalArgumentException("IP address cannot be null.");

            this.ip = ip.trim();
            this.port = port;
        }

        @Override
        public String toString() {
            return (ip.isEmpty()?"*":ip) + ":" + (port<1?"*":port); // examples: *:*, 127.0.0.1:*, 127.0.0.1:1337, *:1337
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;

            if (o instanceof IpPortPair) {
                IpPortPair pair = (IpPortPair)o;
                return pair.getIp().equals(getIp()) && pair.getPort() == getPort();
            } else {
                return super.equals(o);
            }
        }

        public String getHostname() {
            try {
                InetAddress ia = InetAddress.getByName(ip);
                return ia.getHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }

    }

//    public static class PackageException extends Exception {
//        private final TransportLayerPackage pkg;
//        public TransportLayerPackage getPkg() { return pkg; }
//
//        public PackageException(String message, TransportLayerPackage pkg) {
//            super(message);
//            this.pkg = pkg;
//        }
//    }

    private static abstract class IpPackage {
        public abstract String getSourceIP();
        public abstract String getDestinationIP();
        private final long timestamp = System.nanoTime();

        public long getTimestamp() {
            return timestamp;
        }

        public IpPackage() {
        }
    }

    public static abstract class TransportLayerPackage extends IpPackage implements Connections.IConnection {
        private final PackageType type;
        private final IpPortPair source, destination;
        private final int checksum, length;

        public PackageType getType() { return type; }

        @Override public IpPortPair getSource() { return source; }
        @Override public int getSourcePort() { return source.getPort(); }
        @Override public String getSourceIP() { return source.getIp(); }

        @Override public IpPortPair getDestination() { return destination; }
        @Override public int getDestinationPort() { return destination.getPort(); }
        @Override public String getDestinationIP() { return destination.getIp(); }

        public int getLength() { return length; }
        public int getChecksum() { return checksum; }

        public TransportLayerPackage(PackageType type, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super();
            this.type = type;
            this.checksum = checksum;
            this.length = length;

            source = new IpPortPair(sourceIP, sourcePort);
            destination = new IpPortPair(destinationIP, destinationPort);
        }

        @Override
        public String toString() {
            return source + " -> " + destination + " { length="+ length +", checksum=" + checksum +" }";
        }
    }

    public static  class TcpPackage extends TransportLayerPackage {
        private final int seqNumber, ackNumber;
        private final boolean hasFlagACK, hasFlagFIN, hasFlagSYN, hasFlagPush, hasFlagReset, hasFlagUrgent;

        public int getSeqNumber() { return seqNumber; }
        public int getAckNumber() { return ackNumber; }
        public boolean hasFlagACK() { return hasFlagACK; }
        public boolean hasFlagFIN() { return hasFlagFIN; }
        public boolean hasFlagSYN() { return hasFlagSYN; }
        public boolean hasFlagPush() { return hasFlagPush; }
        public boolean hasFlagReset() { return hasFlagReset; }
        public boolean hasFlagUrgent() { return hasFlagUrgent; }

        public TcpPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length,
                          int seqNumber, int ackNumber,
                          boolean hasFlagACK, boolean hasFlagFIN, boolean hasFlagSYN, boolean hasFlagPush, boolean hasFlagReset, boolean hasFlagUrgent
        ) {
            super(PackageType.TCP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);
            this.seqNumber = seqNumber;
            this.ackNumber = ackNumber;
            this.hasFlagACK = hasFlagACK;
            this.hasFlagFIN = hasFlagFIN;
            this.hasFlagSYN = hasFlagSYN;
            this.hasFlagPush = hasFlagPush;
            this.hasFlagReset = hasFlagReset;
            this.hasFlagUrgent = hasFlagUrgent;
        }

        public String toString() {
            return "[TCP] " + super.toString() + " { #SEQ="+ seqNumber +", #ACK= " + ackNumber
                    + ", isACK= " +hasFlagACK + ", isSYN=" + hasFlagSYN +", isFIN=" +hasFlagFIN + ", isRST=" + hasFlagReset + ", isPSH=" + hasFlagPush + ", isURG=" + hasFlagUrgent
                    + " }";
        }
    }

    public static  class UdpPackage extends TransportLayerPackage {
        public UdpPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super(PackageType.UDP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);
        }

        public String toString() { return "[UDP] " + super.toString(); }
    }
}
