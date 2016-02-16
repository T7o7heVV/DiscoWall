package de.uni_kl.informatik.disco.discowall.firewall.packageFilter;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

class PendingConnectionsManager {
    private static final String LOG_TAG = PendingConnectionsManager.class.getSimpleName();

    public static class PendingConnection {
        public final NetfilterBridgeCommunicator.PackageActionCallback pendingActionCallback;
        public final Connections.Connection connection;
        private PendingConnectionTimeoutThread timeoutThread;

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

        public PendingConnectionTimeoutThread getTimeoutThread() {
            return timeoutThread;
        }

        public void setTimeoutThread(PendingConnectionTimeoutThread timeoutThread) {
            this.timeoutThread = timeoutThread;
        }

        @Override
        public String toString() {
            return connection.toString();
        }
    }

    public static interface PendingConnectionTimeoutThread {
        void stopTimeout();
        void startTimeout();
    }

    //================================================================================================================================================

    private final LinkedList<PendingConnection> pendingConnectionsStack = new LinkedList<>();
    private final HashMap<Connections.Connection, Boolean> connectionToInteractiveTempActionMap = new HashMap<>();
    private final Context context;

    PendingConnectionsManager(Context context) {
        this.context = context;
    }

    /**
     * Removes latest pending connection (if any) and returns the removed instance.
     * @return
     */
    public PendingConnection removeLatestPendingConnection() {
        if (pendingConnectionsStack.isEmpty())
            return null;

        PendingConnection pendingConnection = pendingConnectionsStack.get(0);
        pendingConnectionsStack.remove(0); // remove first from stack

        Log.v(LOG_TAG, "latest pending connection removed: " + pendingConnection);

        return pendingConnection;
    }

    public PendingConnection addPendingConnection(Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback pendingActionCallback) {
        PendingConnection pendingConnection = new PendingConnection(connection, pendingActionCallback);
        pendingConnectionsStack.addFirst(pendingConnection); // List used as stack ==> add as first

        Log.v(LOG_TAG, "pending connection added: " + pendingConnection);

        return pendingConnection;
    }

    public PendingConnection getLatestPendingConnection() {
        if (pendingConnectionsStack.isEmpty())
            return null;

        return pendingConnectionsStack.get(0); // adding always from top ==> first element is last one added
    }

    public boolean hasPending() {
        return !pendingConnectionsStack.isEmpty();
    }

    public boolean isPending(Connections.Connection connection) {
        return getPendingConnection(connection) != null;
    }

    private String getConnectionID(Connections.IConnection connection) {
        boolean includePortInfo = DiscoWallSettings.getInstance().isInteractiveTemporaryRulesDistinguishByPorts(context);
        return Connections.Connection.getID(connection, includePortInfo);
    }

    public PendingConnection getPendingConnection(Connections.IConnection connection) {
        final String searchedID = getConnectionID(connection);

        for(PendingConnection pendingConnection : new LinkedList<>(pendingConnectionsStack)) { // copy list to secure it against modification during iteration
            String currentID = getConnectionID(pendingConnection.connection);

            if (currentID.equals(searchedID))
                return pendingConnection;
        }

        return null;
    }

}
