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

    public static class Connection implements IConnection {
        private final Packages.IpPortPair source, destination;
        private final long timestamp = System.nanoTime();
//        private final LinkedList<Packages.TcpPackage> tcpPackages = new LinkedList<>();

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

//        /**
//         * Adds a package to the list of packages within this connection.
//         * @param tcpPackage
//         * @see #isPackagePartOfConnection(Packages.TcpPackage)
//         * @return <code>true</code> if the package has been added. <code>false</code> if the package is not part of this connection.
//         */
//        public boolean addPackage(Packages.TcpPackage tcpPackage) {
//            if (!isPackagePartOfConnection(tcpPackage))
//                return false;
//
//            tcpPackages.add(tcpPackage);
//            return true;
//        }
//
//        public LinkedList<Packages.TcpPackage> getPackages() {
//            return new LinkedList<>(tcpPackages);
//        }
//
//        public int getPackagesCount() {
//            return tcpPackages.size();
//        }

        public boolean isPackagePartOfConnection(Packages.TcpPackage tcpPackage) {
            return ( tcpPackage.getSource().equals(source) && tcpPackage.getDestination().equals(destination) )
                    || ( tcpPackage.getSource().equals(destination) && tcpPackage.getDestination().equals(source) );
        }

        @Override
        public String toString() {
            return source + " -> " + destination + " { timestamp="+ timestamp +" }";
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
    }
}
