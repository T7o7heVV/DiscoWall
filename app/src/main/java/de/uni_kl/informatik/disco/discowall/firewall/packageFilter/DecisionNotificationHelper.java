package de.uni_kl.informatik.disco.discowall.firewall.packageFilter;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.ShowAppRulesActivity;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.utils.AppUtils;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

class DecisionNotificationHelper {
    private static final String LOG_TAG = FirewallPackageFilter.class.getSimpleName(); // LOG_TAG is the one used by the PackageFilter on purpose

    private final Context context;
    private final FirewallPackageFilter packageFilter;
    private final PendingConnectionsManager pendingConnectionsManager;

    private class ConnectionDecisionTimeoutThread implements Runnable, PendingConnectionsManager.PendingConnectionTimeoutThread {
        private final Connections.Connection connection;
        private final int decisionTimeoutInSeconds;
        private final boolean defaultActionAccept;
        private final PendingIntent pendingClickIntent, pendingActionIntentAccept, pendingActionIntentBlock;
        private final NotificationManager notificationManager;

        private Thread thread;
        private boolean runTimeout = false;

        public ConnectionDecisionTimeoutThread(Connections.Connection connection, int decisionTimeoutInSeconds, boolean defaultActionAccept, PendingIntent pendingClickIntent, PendingIntent pendingActionIntentAccept, PendingIntent pendingActionIntentBlock) {
            this.connection = connection;
            this.decisionTimeoutInSeconds = decisionTimeoutInSeconds;
            this.defaultActionAccept = defaultActionAccept;
            this.pendingClickIntent = pendingClickIntent;
            this.pendingActionIntentAccept = pendingActionIntentAccept;
            this.pendingActionIntentBlock = pendingActionIntentBlock;

            this.notificationManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
        }

        @Override
        public void run() {
            int decisionTimeout = decisionTimeoutInSeconds;
            Log.v(LOG_TAG, "Decision Timeout: thread created. timeout = " + decisionTimeout + " seconds.");

            while (pendingConnectionsManager.isPending(connection) && runTimeout) {
                if (decisionTimeout <= 0) {
                    Log.d(LOG_TAG, "Decision Timeout: time is up. Default-Action = " + (defaultActionAccept ? "ACCEPT" : "BLOCK"));

                    // Canceling the notification here, so that the user cannot click AFTER the time is up.
                    notificationManager.cancel(DiscoWallConstants.NotificationIDs.pendingPackage);

                    // Perform default-action
                    if (defaultActionAccept)
                        packageFilter.acceptPendingPackage();
                    else
                        packageFilter.blockPendingPackage();

                    break;
                }

                Notification notification = createUndecidedConnectionNotificationEx(connection, pendingClickIntent, pendingActionIntentAccept, pendingActionIntentBlock, decisionTimeout, defaultActionAccept);
                notificationManager.notify(DiscoWallConstants.NotificationIDs.pendingPackage, notification);

                // one second per iteration
                decisionTimeout--;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Decision Timeout: Thread.sleep(int) was interrupted! Message was: " + e.getMessage(), e);
                }
            }

            Log.v(LOG_TAG, "Decision Timeout: thread done.");
        }

        @Override
        public void stopTimeout() {
            if (!runTimeout) {
                Log.d(LOG_TAG, "trying to stop notification-timeout when it is already finished. Connection: " + connection);
            } else {
                runTimeout = false;
                thread = null;
                Log.d(LOG_TAG, "notification decision-timeout has been stopped for connection: " + connection);
            }
        }

        @Override
        public void startTimeout() {
            if (runTimeout) {
                Log.d(LOG_TAG, "trying to start notification-timeout when it is already running. Connection: " + connection);
            } else {
                runTimeout = true;

                thread = new Thread(this, "Connection-Decision Thread for Connection: " + connection);
                thread.setDaemon(true);
                thread.start(); // start thread

                Log.d(LOG_TAG, "notification decision-timeout started for connection: " + connection);
            }
        }
    }


    public DecisionNotificationHelper(Context context, FirewallPackageFilter packageFilter, PendingConnectionsManager pendingConnectionsManager) {
        this.context = context;
        this.packageFilter = packageFilter;
        this.pendingConnectionsManager = pendingConnectionsManager;
    }

    public void createUndecidedConnectionNotification(final Connections.Connection connection, final int decisionTimeoutInSeconds, final boolean defaultActionAccept) {
        /* IMPORTANT notion about using multiple different (!) PendingIntents at the same time:
         * [ from Android API @ http://developer.android.com/reference/android/app/PendingIntent.html ]
         *
         * If you truly need multiple distinct PendingIntent objects active at the same time
         * (such as to use as two notifications that are both shown at the same time),
         * then you will need to ensure there is something that is different about them to associate them with different PendingIntents.
         * This may be any of the Intent attributes considered by Intent.filterEquals, or different request code integers supplied to
         * getActivity(Context, int, Intent, int), getActivities(Context, int, Intent[], int), getBroadcast(Context, int, Intent, int), or getService(Context, int, Intent, int).
         */

        // IMPORTANT: each pending intent has to use its own request-code! Otherwise android might treat them as the same (if they don't differ in their attributes).

        // Intent: Notification Click
        final PendingIntent pendingClickIntent;
        {
            Intent clickIntent = ShowAppRulesActivity.createActionIntent_decideConnection(context, connection);
            pendingClickIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT); // request-code (= 0) has to differ from other pending intents of this notification!
        }

        // Intent: Action ACCEPT
        final PendingIntent pendingActionIntentAccept;
        {
            Intent actionIntentAccept = ShowAppRulesActivity.createActionIntent_handleConnection(context, connection, true);
            pendingActionIntentAccept = PendingIntent.getActivity(context.getApplicationContext(), 1, actionIntentAccept, PendingIntent.FLAG_UPDATE_CURRENT);  // request-code (= 1) has to differ from other pending intents of this notification!
        }

        // Intent: Action BLOCK
        final PendingIntent pendingActionIntentBlock;
        {
            Intent actionIntentBlock = ShowAppRulesActivity.createActionIntent_handleConnection(context, connection, false);
            pendingActionIntentBlock = PendingIntent.getActivity(context.getApplicationContext(), 2, actionIntentBlock, PendingIntent.FLAG_UPDATE_CURRENT); // request-code (= 2) has to differ from other pending intents of this notification!
        }

        // Create actual Notification:
        final Notification notification = createUndecidedConnectionNotificationEx(connection, pendingClickIntent, pendingActionIntentAccept, pendingActionIntentBlock, decisionTimeoutInSeconds, defaultActionAccept);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
        notificationManager.notify(DiscoWallConstants.NotificationIDs.pendingPackage, notification); // only one fixed NotificationID required, since there will always be only one pending package (as the NetfilterBridge waits for the response)

        // Expand Statusbar, so that the user can decide on the connection (if setting enabled)
        if (DiscoWallSettings.getInstance().isConnectionDecisionNotificationExpandStatusbar(context)) {
            Log.v(LOG_TAG, "expanding StatusBar in order to show notification (according to user-settings)...");

            if (AppUtils.isAppInForeground(context)) {
                Log.v(LOG_TAG, "DiscoWall is in foreground. Therefore the StatusBar will not be expanded.");
//            if (ShowAppRulesActivity.isConnectionDecisionDialogOpen()) {
//                Log.v(LOG_TAG, "Connection Decision Dialog is currently being displayed. Therefore the StatusBar will not be expanded.");
            } else {
                try {
                    GuiUtils.expandStatusbar(context);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception trying to expand statusbar: " + e.getMessage(), e);
                }
            }
        }

        // Create & start Auto-Timeout Thread
        ConnectionDecisionTimeoutThread timeoutThread = new ConnectionDecisionTimeoutThread(connection, decisionTimeoutInSeconds, defaultActionAccept, pendingClickIntent, pendingActionIntentAccept, pendingActionIntentBlock);

        // register timeout-thread, so that it can be stopped when the user starts the policy-decision for this action (i.e. when he opens the decision-dialog)
        PendingConnectionsManager.PendingConnection pendingConnection = pendingConnectionsManager.getPendingConnection(connection);
        pendingConnection.setTimeoutThread(timeoutThread);

        // start decision timeout
        timeoutThread.startTimeout();
    }

    private Notification createUndecidedConnectionNotificationEx(Connections.Connection connection, PendingIntent pendingClickIntent, PendingIntent pendingActionIntentAccept, PendingIntent pendingActionIntentBlock, int timeoutSecondsRemain, boolean timeoutActionAccept) {
        String bigMessage = "Client: " + connection.getSource()
                + "\n" + "Server: " + connection.getDestination()
                + "\n" + timeoutSecondsRemain + " seconds to " + (timeoutActionAccept ? "accept" : "reject");

        return new Notification.Builder(context)
                .setContentTitle(connection.toString())
                .setContentText("DiscoWall: ACCEPT/BLOCK pending connection.")
                .setSmallIcon(R.mipmap.notification_new_connection)
                .setContentIntent(pendingClickIntent)
//                .setAutoCancel(true)  // notification will be removed on click // Notification-Removal will be done on accept/blockPendingPackage()
                .setOngoing(true) // notification cannot be deleted by statusbar "clear action" or user swipe-action (only by click or code)
                .setStyle(new Notification.BigTextStyle().bigText(bigMessage)) // Expand Notification
                .addAction(R.drawable.notification_symbol_rule_policy_allow, "accept", pendingActionIntentAccept) // Add Action "ACCEPT"; Actions cannot auto-remove notification - is being done on accept/blockPendingPackage()
                .addAction(R.drawable.notification_symbol_rule_policy_block, "block", pendingActionIntentBlock) // Add Action "BLOCK"
                .build();
    }

    public void stopDecisionTimeout(AppUidGroup appUidGroup, Connections.IConnection connection) {
        PendingConnectionsManager.PendingConnection pendingConnection = pendingConnectionsManager.getPendingConnection(connection);

        if (pendingConnection == null) {
            Log.w(LOG_TAG, "Expected connection to be pending when it is not: " + connection);
            return;
        }

        if (pendingConnection.getTimeoutThread() == null) {
            Log.w(LOG_TAG, "Trying to access pending connection timeout-thread before it has been created. Bad thread-scheduling android... real bad. Connection is: " + connection);
            return;
        }

        pendingConnection.getTimeoutThread().stopTimeout();
    }

    public void restartDecisionTimeout(Connections.IConnection connection) {
        PendingConnectionsManager.PendingConnection pendingConnection = pendingConnectionsManager.getPendingConnection(connection);

        if (pendingConnection == null) {
            Log.w(LOG_TAG, "Expected connection to be pending when it is not: " + connection);
            return;
        }

        if (pendingConnection.getTimeoutThread() == null) {
            Log.w(LOG_TAG, "Trying to access pending connection timeout-thread when there is none. Connection is: " + connection);
            return;
        }

        pendingConnection.getTimeoutThread().startTimeout();
    }

}
