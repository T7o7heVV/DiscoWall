package de.uni_kl.informatik.disco.discowall.packages;

public class Connections {
    public static interface IConnectionSource {
        Packages.IpPortPair getSource();
        int getSourcePort();
        public String getSourceIP();
    }

    public static interface IConnectionDestination {
        Packages.IpPortPair getDestination();
        int getDestinationPort();
        public String getDestinationIP();
    }

    public static interface IConnection extends IConnectionSource, IConnectionDestination {
    }

    public static abstract class Connection implements IConnection {
        private final Packages.IpPortPair source, destination;
        private final long timestamp = System.nanoTime();
        private int packagesCount = 0;
        private int totalLength = 0;

        @Override public Packages.IpPortPair getSource() { return source; }
        @Override public int getSourcePort() { return source.getPort(); }
        @Override public String getSourceIP() { return source.getIp(); }

        @Override public Packages.IpPortPair getDestination() { return destination; }
        @Override public int getDestinationPort() { return destination.getPort(); }
        @Override public String getDestinationIP() { return destination.getIp(); }

        public long getTimestamp() { return timestamp; }

        Connection(IConnection connectionData) {
            this(connectionData.getSource(), connectionData.getDestination());
        }

        Connection(Packages.IpPortPair source, Packages.IpPortPair destination) {
            this(source.getIp(), source.getPort(), destination.getIp(), destination.getPort());
        }

        Connection(String sourceIP, int sourcePort, String destinationIP, int destinationPort) {
            source = new Packages.IpPortPair(sourceIP, sourcePort);
            destination = new Packages.IpPortPair(destinationIP, destinationPort);
        }

        @Override
        public String toString() {
            return source + " -> " + destination + " { timestamp="+ timestamp + ", #packages=" + packagesCount + ", totalBytes=" + totalLength +" }";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;

            if (o instanceof Connection) {
                Connection connection = (Connection)o;
                return connection.getID().equals(this.getID());
            } else {
                return super.equals(o);
            }
        }

        public int getPackagesCount() {
            return packagesCount;
        }

        public int getTotalLength() {
            return totalLength;
        }

        /**
         * The connection ID is a ordered string of source->destination.
         * @return
         */
        public String getID() {
            String sourceID = source.toString();
            String destinationID = destination.toString();

            if (sourceID.compareTo(destinationID) < 0)
                return sourceID + "->" + destinationID;
            else
                return destinationID + "->" + sourceID;
        }

        protected boolean update(Packages.TransportLayerPackage tlPackage) {
            if (!isPackagePartOfConnection(tlPackage))
                return false;

            packagesCount++;
            totalLength += tlPackage.getLength();

            return true;
        }

        public boolean isPackagePartOfConnection(Packages.TransportLayerPackage tlPackage) {
            return ( tlPackage.getSource().equals(getSource()) && tlPackage.getDestination().equals(getDestination()) )
                    || ( tlPackage.getSource().equals(getDestination()) && tlPackage.getDestination().equals(getSource()) );
        }
    }

    public static class UdpConnection extends Connection {
        UdpConnection(IConnection connectionData) {
            super(connectionData);
        }

        UdpConnection(Packages.IpPortPair source, Packages.IpPortPair destination) {
            super(source, destination);
        }

        UdpConnection(String sourceIP, int sourcePort, String destinationIP, int destinationPort) {
            super(sourceIP, sourcePort, destinationIP, destinationPort);
        }

        public boolean update(Packages.UdpPackage udpPackage) {
            return super.update(udpPackage);
        }
    }

    public static class TcpConnection extends Connection {
        public enum TcpConnectionState { UNKNOWN, OPEN, SYN_WAIT, CLOSE_WAIT, CLOSED, RESET }
        private int lastSeqNumber = -1;
        private Packages.TcpPackage lastPackage;
        private TcpConnectionState state = TcpConnectionState.UNKNOWN;

        TcpConnection(IConnection connectionData) {
            super(connectionData);
        }

        TcpConnection(Packages.IpPortPair source, Packages.IpPortPair destination) {
            super(source, destination);
        }

        TcpConnection(String sourceIP, int sourcePort, String destinationIP, int destinationPort) {
            super(sourceIP, sourcePort, destinationIP, destinationPort);
        }

        public int getLastSeqNumber() {
            return lastSeqNumber;
        }

        public TcpConnectionState getState() {
            return state;
        }

        public Packages.TcpPackage getLastPackage() {
            return lastPackage;
        }

        public boolean update(Packages.TcpPackage tcpPackage) {
            if (!super.update(tcpPackage))
                return false;

            lastSeqNumber = tcpPackage.getSeqNumber();
            lastPackage = tcpPackage;

            if (tcpPackage.hasFlagFIN() && !tcpPackage.hasFlagACK())
                state = TcpConnectionState.CLOSE_WAIT;
            else if (tcpPackage.hasFlagFIN() && tcpPackage.hasFlagACK())
                state = TcpConnectionState.CLOSED;
            else if (tcpPackage.hasFlagSYN() && !tcpPackage.hasFlagACK())
                state = TcpConnectionState.SYN_WAIT;
            else if (tcpPackage.hasFlagSYN() && tcpPackage.hasFlagACK())
                state = TcpConnectionState.OPEN;
            else if (tcpPackage.hasFlagReset())
                state = TcpConnectionState.RESET;

            return true;
        }

        @Override
        public String toString() {
            return super.toString() + " { state=" + state + ", lastSeqNr=" + lastSeqNumber + " }";
        }
    }
}
