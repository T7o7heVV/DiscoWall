package de.uni_kl.informatik.disco.discowall.packages;

import java.util.HashMap;
import java.util.LinkedList;

public class ConnectionManager {
    private final ConnectionHash<Connections.TcpConnection> tcpConnectionHash = new ConnectionHash();
    private final ConnectionHash<Connections.UdpConnection> udpConnectionHash = new ConnectionHash();

    public Connections.TcpConnection getTcpConnection(Packages.IpPortPair source, Packages.IpPortPair destination) {
        Connections.TcpConnection connection = tcpConnectionHash.get(source, destination);

        if (connection == null) {
            connection = new Connections.TcpConnection(source, destination);
            tcpConnectionHash.put(connection);
        }

        return connection;
    }

    public Connections.TcpConnection getTcpConnection(Packages.TcpPackage tcpPackage) {
        return getTcpConnection(tcpPackage.getSource(), tcpPackage.getDestination());
    }

    public boolean containsTcpConnection(Connections.IConnection connection) {
        return tcpConnectionHash.contains(connection);
    }

    public Connections.UdpConnection getUdpConnection(Packages.IpPortPair source, Packages.IpPortPair destination) {
        Connections.UdpConnection connection = udpConnectionHash.get(source, destination);

        if (connection == null) {
            connection = new Connections.UdpConnection(source, destination);
            udpConnectionHash.put(connection);
        }

        return connection;
    }

    public Connections.UdpConnection getUdpConnection(Packages.UdpPackage udpPackage) {
        return getUdpConnection(udpPackage.getSource(), udpPackage.getDestination());
    }

    public boolean containsUdpConnection(Connections.IConnection connection) {
        return udpConnectionHash.contains(connection);
    }


    private static class ConnectionHash<TConnection extends Connections.Connection> {
        private final HashMap<String, TConnection> connectionIdToConnectionMap = new HashMap<>();

        public LinkedList<TConnection> getConnections() {
            return new LinkedList<>(connectionIdToConnectionMap.values());
        }

        public void put(TConnection connection) {
            connectionIdToConnectionMap.put(connection.getID(), connection);
        }

        public boolean contains(Connections.IConnection connection) {
            return contains(connection.getSource(), connection.getDestination());
        }

        public boolean contains(Packages.IpPortPair source, Packages.IpPortPair destination) {
            return connectionIdToConnectionMap.containsKey(getConnectionID(source, destination));
        }

        public Connections.Connection get(Connections.IConnection connection) {
            return get(connection.getSource(), connection.getDestination());
        }

        public TConnection get(Packages.IpPortPair source, Packages.IpPortPair destination) {
            return connectionIdToConnectionMap.get(getConnectionID(source, destination));
        }

        private String getConnectionID(Packages.IpPortPair source, Packages.IpPortPair destination) {
            String sourceID = source.toString();
            String destinationID = destination.toString();

            if (sourceID.compareTo(destinationID) < 0)
                return sourceID + "->" + destinationID;
            else
                return destinationID + "->" + sourceID;
        }

    }

}
