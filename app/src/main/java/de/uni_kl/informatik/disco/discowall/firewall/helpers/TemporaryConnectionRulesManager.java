package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import java.util.HashMap;

import de.uni_kl.informatik.disco.discowall.packages.Connections;

public class TemporaryConnectionRulesManager {
    private static class TempRule {
        private final boolean accept;
        private final Connections.Connection connection;

        private TempRule(Connections.Connection connection, boolean accept) {
            this.accept = accept;
            this.connection = connection;
        }

        public Connections.Connection getConnection() {
            return connection;
        }

        public boolean isAccept() {
            return accept;
        }

        public boolean isBlock() {
            return !accept;
        }
    }

    //================================================================================================================================================

    private final HashMap<String, TempRule> connectionToInteractiveTempActionMap = new HashMap<>();

    public boolean hasRule(Connections.Connection connection) {
        return connectionToInteractiveTempActionMap.containsKey(connection.getID());
    }

    public void putRule(Connections.Connection connection, boolean accept) {
        connectionToInteractiveTempActionMap.put(connection.getID(), new TempRule(connection, accept));
    }

    public boolean isAccepted(Connections.Connection connection) {
        if (!hasRule(connection))
            throw new RuntimeException("Trying to fetch rule for connection, which has no rule defined yet: " + connection);

        return connectionToInteractiveTempActionMap.get(connection.getID()).isAccept();
    }
}
