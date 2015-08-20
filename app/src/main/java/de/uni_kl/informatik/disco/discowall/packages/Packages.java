package de.uni_kl.informatik.disco.discowall.packages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;

public class Packages {
    public enum TransportLayerProtocol {
        TCP, UDP;

        public boolean isTcp() {
            return this == TCP;
        }

        public boolean isUdp() {
            return this == UDP;
        }

        public FirewallRules.ProtocolFilter toFilter() {
            switch(this) {
                case TCP: return FirewallRules.ProtocolFilter.TCP;
                case UDP: return FirewallRules.ProtocolFilter.UDP;
                default: throw new RuntimeException("Missing implementation for protocol-kind: " + this);
            }
        }
    }

    public enum NetworkInterface { Loopback, WiFi, Umts }

    public static class IpPortPair {
        public static final int PORT_ANY = 0;
        public static final int PORT_MAX = 65535;
        public static final String IP_ANY = "";

        private final String ip;
        private final int port;

        public int getPort() { return port; }
        public String getIp() { return ip; }

        public IpPortPair(String ip, int port) {
            if (ip == null)
                throw new IllegalArgumentException("IP address cannot be null.");
            if (port < 0)
                throw new IllegalArgumentException("Port-numbers cannot be negative.");
            else if (port > PORT_MAX)
                throw new IllegalArgumentException("Port " + port + " exceeds maximum " + PORT_MAX + ".");

            this.ip = ip.trim();
            this.port = port;
        }

        public boolean hasIp() {
            return !isIpAny();
        }

        public boolean isIpAny() {
            return ip.trim().isEmpty() || ip.trim().equals("*");
        }

        public boolean hasPort() {
            return !isPortAny();
        }

        public boolean isPortAny() {
            return port <= 0;
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
        private final int inputDeviceIndex;
        private final int outputDeviceIndex;

        public PhysicalLayerPackage(int inputDeviceIndex, int outputDeviceIndex) {
            this.inputDeviceIndex = inputDeviceIndex;
            this.outputDeviceIndex = outputDeviceIndex;
        }

        public int getOutputDeviceIndex() {
            return outputDeviceIndex;
        }

        public int getInputDeviceIndex() {
            return inputDeviceIndex;
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
        public IpPackage(int inputDeviceIndex, int outputDeviceIndex) {
            super(inputDeviceIndex, outputDeviceIndex);
        }

        public abstract String getSourceIP();
        public abstract String getDestinationIP();
    }

    public static abstract class TransportLayerPackage extends IpPackage implements Connections.IConnection {
        private final TransportLayerProtocol protocol;
        private final IpPortPair source, destination, localAddress, remoteAddress;
        private final int checksum, length;

        public TransportLayerProtocol getProtocol() { return protocol; }

        @Override public IpPortPair getSource() { return source; }
        @Override public int getSourcePort() { return source.getPort(); }
        @Override public String getSourceIP() { return source.getIp(); }

        @Override public IpPortPair getDestination() { return destination; }
        @Override public int getDestinationPort() { return destination.getPort(); }
        @Override public String getDestinationIP() { return destination.getIp(); }

        public IpPortPair getLocalAddress() {
            return localAddress;
        }

        public IpPortPair getRemoteAddress() {
            return remoteAddress;
        }

        public int getLength() { return length; }
        public int getChecksum() { return checksum; }

        public boolean isOriginRemote() {
            return remoteAddress.equals(destination);
        }

        public boolean isOriginLocal() {
            return localAddress.equals(source);
        }

        public TransportLayerPackage(int inputDeviceIndex, int outputDeviceIndex, TransportLayerProtocol protocol, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super(inputDeviceIndex, outputDeviceIndex);

            this.protocol = protocol;
            this.checksum = checksum;
            this.length = length;

            source = new IpPortPair(sourceIP, sourcePort);
            destination = new IpPortPair(destinationIP, destinationPort);

            if (getInputDeviceIndex() >= 0) {
                // if input-device index specified, the package has been received by this device
                localAddress = destination; // ==> The destination of this package is the device
                remoteAddress = source; // ==> The source of this package is the remote-host
            } else if (getOutputDeviceIndex() >= 0) {
                localAddress = source;
                remoteAddress = destination;
            } else {
                throw new RuntimeException("Invalid package definition! Neither input- nor output-device specified. Package: " + this.toString());
            }
        }

        protected String transportLayerToString() {
            return source + " -> " + destination + ", local=" + localAddress + ", remote=" + remoteAddress + ", length="+ length +", checksum=" + checksum;
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

        public TcpPackage(int inputDeviceIndex, int outputDeviceIndex, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length,
                          int seqNumber, int ackNumber,
                          boolean hasFlagACK, boolean hasFlagFIN, boolean hasFlagSYN, boolean hasFlagPush, boolean hasFlagReset, boolean hasFlagUrgent
        ) {
            super(inputDeviceIndex, outputDeviceIndex, TransportLayerProtocol.TCP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);

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
                    + (hasFlagACK ? ", ACK" : "" )    + (hasFlagSYN ? ", SYN" : "")   + (hasFlagFIN ? ", FIN" : "")
                    + (hasFlagReset ? ", RST" : "")   + (hasFlagPush ? ", PSH" : "")  + (hasFlagUrgent ? ", URG" : "")
//                    + (hasFlagACK ? ", isACK= " +hasFlagACK : "" )    + (hasFlagSYN ? ", isSYN=" + hasFlagSYN : "")   + (hasFlagFIN ? ", isFIN=" +hasFlagFIN : "")
//                    + (hasFlagReset ? ", isRST=" + hasFlagReset : "") + (hasFlagPush ? ", isPSH=" + hasFlagPush : "") + (hasFlagUrgent ? ", isURG=" + hasFlagUrgent : "")
                    + " } " + super.toString();
        }

        /**
         * Specifies whether this package is the attempt to establish a connection from a remote host.
         * <p></p>
         * This is true, if the package originates in remote-host, has SYN flag but no ACK flag.
         * @return
         */
        public boolean isRemoteConnectionEstablishSyn() {
            return isOriginRemote() && hasFlagSYN() && !hasFlagACK();
        }

        /**
         * Specifies whether this package is the attempt to establish a connection from localhost.
         * <p></p>
         * This is true, if the package originates in remote-server, has SYN flag but no ACK flag.
         * @return
         */
        public boolean isLocalConnectionEstablishSyn() {
            return isOriginLocal() && hasFlagSYN() && !hasFlagACK();
        }

        public boolean isConnectionEstablishSyn() {
            return hasFlagSYN() && !hasFlagACK();
        }
    }

    public static  class UdpPackage extends TransportLayerPackage {
        public UdpPackage(int inputDeviceIndex, int outputDeviceIndex, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super(inputDeviceIndex, outputDeviceIndex, TransportLayerProtocol.UDP, sourceIP, destinationIP, sourcePort, destinationPort, checksum, length);
        }

        public String toString() { return "{ [UDP] "+transportLayerToString()+ " } " + super.toString(); }
    }
}
