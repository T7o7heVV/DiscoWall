package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;

public class PendingConnectionsManager {
    public static class PendingConnection {
        public final NetfilterBridgeCommunicator.PackageActionCallback pendingActionCallback;
        public final Connections.Connection connection;

        private PendingConnection(Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback pendingActionCallback) {
            this.pendingActionCallback = pendingActionCallback;
            this.connection = connection;
        }

        public void accept() {
            pendingActionCallback.acceptPendingPackage();
        }

        public void block() {
            pendingActionCallback.blockPendingPackage();
        }
    }

    //================================================================================================================================================

    private final LinkedList<PendingConnection> pendingConnectionsStack = new LinkedList<>();
    private final HashMap<Connections.Connection, Boolean> connectionToInteractiveTempActionMap = new HashMap<>();

    /**
     * Removes latest pending connection (if any) and returns the removed instance.
     * @return
     */
    public PendingConnection removeLatestPendingConnection() {
        if (pendingConnectionsStack.isEmpty())
            return null;

        PendingConnection pendingConnection = pendingConnectionsStack.get(0);
        pendingConnectionsStack.remove(0); // remove first from stack

        return pendingConnection;
    }

    public PendingConnection addPendingConnection(Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback pendingActionCallback) {
        PendingConnection pendingConnection = new PendingConnection(connection, pendingActionCallback);
        pendingConnectionsStack.addFirst(pendingConnection); // List used as stack ==> add as first

        return pendingConnection;
    }

    public PendingConnection getLatestPendingConnection() {
        if (pendingConnectionsStack.isEmpty())
            return null;

        return pendingConnectionsStack.get(0); // adding always from top ==> first element is last one added
    }

    public boolean hasPendingConnections() {
        return pendingConnectionsStack.size() > 0;
    }
}
