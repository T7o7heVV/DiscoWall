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

        public int getSourcePort() { return sourcePort; }
        public int getDestinationPort() { return destinationPort; }
        public PackageType getType() { return type; }

        public TransportLayerPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort, PackageType type) {
            super(sourceIP, destinationIP);
            this.sourcePort = sourcePort;
            this.destinationPort = destinationPort;
            this.type = type;
        }

        @Override
        public String toString() {
            return sourceIP +":" + sourcePort + " -> " + destinationIP + ":" + destinationPort;
        }
    }

    public static  class TcpPackage extends TransportLayerPackage {
        public TcpPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort) {
            super(sourceIP, destinationIP, sourcePort, destinationPort, PackageType.TCP);
        }

        public String toString() {
            return "[TCP] " + super.toString();
        }
    }

    public static  class UdpPackage extends TransportLayerPackage {
        public UdpPackage(String sourceIP, String destinationIP, int sourcePort, int destinationPort) {
            super(sourceIP, destinationIP, sourcePort, destinationPort, PackageType.UDP);
        }

        public String toString() {
            return "[UDP] " + super.toString();
        }
    }
}
