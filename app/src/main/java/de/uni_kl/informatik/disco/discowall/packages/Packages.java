package de.uni_kl.informatik.disco.discowall.packages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Packages {
    public enum TransportProtocol { TCP, UDP }
    public enum NetworkInterface { Loopback, WiFi, Umts }

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

    private static abstract class Package {
        private NetworkInterface networkInterface = null;
        private final long timestamp = System.nanoTime();
        private int userId = -1;

        /**
         * Timestamp in nanoseconds. This timestamp does not represent a usual system-time,
         * as it is timezone independent and not influenced by the changing of the devices clock.
         * @return
         */
        public long getTimestamp() {
            return timestamp;
        }

        public String getTimestampReadable() {
            Date date = new Date(timestamp/1000); // nanoseconds to milliseconds
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
            return formatter.format(date);
        }

        public NetworkInterface getNetworkInterface() {
            return networkInterface;
        }

        public void setNetworkInterface(NetworkInterface networkInterface) {
            this.networkInterface = networkInterface;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public int getUserId() {
            return userId;
        }

        @Override
        public String toString() {
            return "{ [*] timestamp="+ timestamp + ", time=" + getTimestampReadable() + (networkInterface==null ? "" : ", interface="+networkInterface) + (userId < 0 ? "" : ", uid=" + userId) +" }";
        }

    }

    private static abstract class NetfilterPackage extends Package {
        /**
         * The optional netfilter-package marking, which can be set using an iptables rule "-j MARK --set-mark <n>".
         */
        private int mark = -1;

        public int getMark() {
            return mark;
        }

        public void setMark(int mark) {
            this.mark = mark;
        }

        @Override
        public String toString() {
            return  " { [netfilter]"
                    + (mark<0 ? "" : " mark=" + mark)
                    +" } "
                    + super.toString();
        }
    }

    private static abstract class PhysicalLayerPackage extends NetfilterPackage {
        private int inputDeviceIndex = -1;
        private int outputDeviceIndex = -1;

        public int getOutputDeviceIndex() {
            return outputDeviceIndex;
        }

        public void setOutputDeviceIndex(int outputDeviceIndex) {
            this.outputDeviceIndex = outputDeviceIndex;
        }

        public int getInputDeviceIndex() {
            return inputDeviceIndex;
        }

        public void setInputDeviceIndex(int inputDeviceIndex) {
            this.inputDeviceIndex = inputDeviceIndex;
        }

        @Override
        public String toString() {
            return  " { [phys]"
                    + (inputDeviceIndex<0 ? "" : " in-dev=" + inputDeviceIndex)
                    + (outputDeviceIndex<0 ? "" : " out-dev=" + outputDeviceIndex)
                    +" } "
                    + super.toString();
        }
    }

    private static abstract class IpPackage extends PhysicalLayerPackage {
        public abstract String getSourceIP();
        public abstract String getDestinationIP();

        public IpPackage() {
        }
    }

    public static abstract class TransportLayerPackage extends IpPackage implements Connections.IConnection {
        private final TransportProtocol protocol;
        private final IpPortPair source, destination;
        private final int checksum, length;

        public TransportProtocol getProtocol() { return protocol; }

        @Override public IpPortPair getSource() { return source; }
        @Override public int getSourcePort() { return source.getPort(); }
        @Override public String getSourceIP() { return source.getIp(); }

        @Override public IpPortPair getDestination() { return destination; }
        @Override public int getDestinationPort() { return destination.getPort(); }
        @Override public String getDestinationIP() { return destination.getIp(); }

        public int getLength() { return length; }
        public int getChecksum() { return checksum; }

        public TransportLayerPackage(TransportProtocol protocol, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super();
            this.protocol = protocol;
            this.checksum = checksum;
            this.length = length;

            source = new IpPortPair(sourceIP, sourcePort);
            destination = new IpPortPair(destinationIP, destinationPort);
        }

        protected String transportLayerToString() {
            return source + " -> " + destination + " length="+ length +", checksum=" + checksum;
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
            super(TransportProtocol.TCP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);
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
            return "{ [TCP] " + transportLayerToString() + " #SEQ="+ seqNumber +", #ACK= " + ackNumber
                    + ", isACK= " +hasFlagACK + ", isSYN=" + hasFlagSYN +", isFIN=" +hasFlagFIN + ", isRST=" + hasFlagReset + ", isPSH=" + hasFlagPush + ", isURG=" + hasFlagUrgent
                    + " } " + super.toString();
        }
    }

    public static  class UdpPackage extends TransportLayerPackage {
        public UdpPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super(TransportProtocol.UDP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);
        }

        public String toString() { return "{ [UDP] "+transportLayerToString()+ " } " + super.toString(); }
    }
}
