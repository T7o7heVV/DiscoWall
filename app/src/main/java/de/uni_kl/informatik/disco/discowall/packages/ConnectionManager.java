package de.uni_kl.informatik.disco.discowall.packages;

import java.util.HashMap;
import java.util.LinkedList;

public class ConnectionManager {
    private final ConnectionHash connectionHash = new ConnectionHash();

    public Connections.Connection getConnection(Packages.IpPortPair source, Packages.IpPortPair destination) {
        Connections.Connection connection = connectionHash.get(source, destination);

        if (connection == null) {
            connection = new Connections.Connection(source, destination);
            connectionHash.put(connection);
        }

        return connection;
    }

    public Connections.Connection getConnection(Connections.IConnection connection) {
        return getConnection(connection.getSource(), connection.getDestination());
    }

    public boolean containsConnection(Connections.IConnection connection) {
        return connectionHash.contains(connection);
    }

    private static class ConnectionHash {
        private final HashMap<String, Connections.Connection> connectionIdToConnectionMap = new HashMap<>();

        public LinkedList<Connections.Connection> getConnections() {
            return new LinkedList<>(connectionIdToConnectionMap.values());
        }

        public void put(Connections.Connection connection) {
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

        public Connections.Connection get(Packages.IpPortPair source, Packages.IpPortPair destination) {
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
