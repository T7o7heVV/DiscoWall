package de.uni_kl.informatik.disco.discowall.firewall.packageFilter;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

class TemporaryConnectionRulesManager {
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
    private final Context context;

    TemporaryConnectionRulesManager(Context context) {
        this.context = context;
    }

    private String getConnectionID(Connections.IConnection connection) {
        boolean includePortInfo = DiscoWallSettings.getInstance().isInteractiveTemporaryRulesDistinguishByPorts(context);
        return Connections.Connection.getID(connection, includePortInfo);
    }

    public boolean hasRule(Connections.Connection connection) {
        return connectionToInteractiveTempActionMap.containsKey(getConnectionID(connection));
    }

    public void putRule(Connections.Connection connection, boolean accept) {
        connectionToInteractiveTempActionMap.put(getConnectionID(connection), new TempRule(connection, accept));
    }

    public boolean isAccepted(Connections.Connection connection) {
        if (!hasRule(connection))
            throw new RuntimeException("Trying to fetch rule for connection, which has no rule defined yet: " + connection);

        return connectionToInteractiveTempActionMap.get(getConnectionID(connection)).isAccept();
    }
}
