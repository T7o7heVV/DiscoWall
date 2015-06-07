package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

public class NetfilterBridgePackages {
    public enum PackageType { TCP, UDP }

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
        protected final String sourceIP;
        protected final String destinationIP;

        public String getDestinationIP() { return destinationIP; }
        public String getSourceIP() { return sourceIP; }

        public IpPackage(String sourceIP, String destinationIP) {
            this.sourceIP = sourceIP;
            this.destinationIP = destinationIP;
        }
    }

    public static abstract class TransportLayerPackage extends IpPackage {
        protected final int sourcePort;
        protected final int destinationPort;
        protected final PackageType type;
        protected final int checksum;
        protected final int length;

        public PackageType getType() { return type; }
        public int getSourcePort() { return sourcePort; }
        public int getLength() { return length; }
        public int getChecksum() { return checksum; }

        public TransportLayerPackage(PackageType type, String sourceIP, String destinationIP, int sourcePort, int destinationPort, int checksum, int length) {
            super(sourceIP, destinationIP);
            this.type = type;
            this.sourcePort = sourcePort;
            this.destinationPort = destinationPort;
            this.checksum = checksum;
            this.length = length;
        }

        @Override
        public String toString() {
            return sourceIP +":" + sourcePort + " -> " + destinationIP + ":" + destinationPort + " { length="+ length +", checksum=" + checksum +" }";
        }
    }

    public static  class TcpPackage extends TransportLayerPackage {
        protected final int seqNumber, ackNumber;
        protected final boolean hasFlagACK, hasFlagFIN, hasFlagSYN, hasFlagPush, hasFlagReset, hasFlagUrgent;

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
