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
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallPolicyManager;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.subsystems.SubsystemPendingPackagesManager;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class FirewallPackageFilter implements SubsystemPendingPackagesManager {
    private static final String LOG_TAG = FirewallPackageFilter.class.getSimpleName();
    private final FirewallPolicyManager policyManager;
    private final FirewallRulesManager rulesManager;
    private final WatchedAppsManager watchedAppsManager;
    private final Context context;

    private final PendingConnectionsManager pendingConnectionsManager = new PendingConnectionsManager();
    private final TemporaryConnectionRulesManager tempRulesManager = new TemporaryConnectionRulesManager();

    public FirewallPackageFilter(Context context, FirewallPolicyManager policyManager, FirewallRulesManager rulesManager, WatchedAppsManager watchedAppsManager) {
        this.context = context;
        this.policyManager = policyManager;
        this.rulesManager = rulesManager;
        this.watchedAppsManager = watchedAppsManager;
    }

    @Override
    public void acceptPendingPackage() {
        removePendingConnectionNotification(); // remove notification (if any)

        if (!pendingConnectionsManager.hasPending()) {
            Log.w(LOG_TAG, "Trying to accept pending package while there is none. Connection has probably already being handled.");
            return;
        }

        Log.i(LOG_TAG, "Pending Connection: [User-Decision] ACCEPT");
        PendingConnectionsManager.PendingConnection pendingConnection = pendingConnectionsManager.removeLatestPendingConnection();

        // rule has to be added BEFORE accepting/blocking connection, as the next package will be handled immediately after the current one is handled.
        tempRulesManager.putRule(pendingConnection.connection, true); // mark action as "temp accepted" - if the user adds a rule for this action, this temp-value will become irrelevant
        pendingConnection.accept();
    }

    @Override
    public void blockPendingPackage() {
        removePendingConnectionNotification(); // remove notification (if any)

        if (!pendingConnectionsManager.hasPending()) {
            Log.w(LOG_TAG, "Trying to block pending package while there is none. Connection has probably already being handled.");
            return;
        }

        Log.i(LOG_TAG, "Pending Connection: [User-Decision] BLOCK");

        PendingConnectionsManager.PendingConnection pendingConnection = pendingConnectionsManager.removeLatestPendingConnection();

        // rule has to be added BEFORE accepting/blocking connection, as the next package will be handled immediately after the current one is handled.
        tempRulesManager.putRule(pendingConnection.connection, false); // mark action as "temp accepted" - if the user adds a rule for this action, this temp-value will become irrelevant
        pendingConnection.block();
    }

    private void removePendingConnectionNotification() {
        final int notificationID = DiscoWallConstants.NotificationIDs.pendingPackage; // There will always be only one pending package (as the NetfilterBridge waits for the response)
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }


    private FirewallRules.IFirewallPolicyRule getPackageRule(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        // Find first matching rule for package:
        for(FirewallRules.IFirewallPolicyRule rule : rulesManager.getPolicyRules(tlPackage.getUserId())) {
            if (rule.appliesTo(tlPackage))
                return rule;
        }

        return null;
    }

    private void decidePackageAcceptedInteractively(Packages.TransportLayerPackage tlPackage, Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback actionCallback) {
        Log.i(LOG_TAG, "interactive choice for connection: " + connection);

        final boolean defaultActionAccept = DiscoWallSettings.getInstance().isNewConnectionDefaultDecisionAccept(context); // default-action (ACCEPT/BLOCK)
        final int decisionTimeout = DiscoWallSettings.getInstance().getNewConnectionDecisionTimeoutSeconds(context); // time after which the default-action will be taken

        // If the user simply clicked "accept" or "block", but did not create a permanent rule,
        // this decision will be stored here:
        if (tempRulesManager.hasRule(connection)) {
            boolean accept = tempRulesManager.isAccepted(connection);

            Log.d(LOG_TAG, "performing temporary connection rule: " + (accept ? "accept" : "block"));

            // Perform temp-action:
            if (accept)
                actionCallback.acceptPendingPackage();
            else
                actionCallback.blockPendingPackage();

            return;
        }

        // TODO: PendingConnections has to be a stack

        Log.d(LOG_TAG, "no temporary connection rule set. User will decide (or timeout will select defaulta action)...");

        /* How the package-decision answering works:
         * 1) The NetfilterBridge blocks until it receives a response for the pending package: accept or block/reject
         * 2) The NetfilterBridgeCommunicator calls this method (through the firewall) and provides the "PackageActionCallback" instance, which can either accept or block the package
         * 3) Here (FirewallPackageFilter) a notification is being created, which will do the following
         *    (1) count down the seconds (i.e. refresh notification each second) - if 0 is reached, the package will automatically be accepted/blocked (according to settings)
         *    (2) on notification click, a dialog will be opened, which will call FirewallPackageFilter.acceptPendingPackage() or .rejectPendingPackage() - according to user decision.
         */

        Log.v(LOG_TAG, "showing notification for user-decision...");

        pendingConnectionsManager.addPendingConnection(connection, actionCallback);
        createUndecidedConnectionNotification(connection, decisionTimeout, defaultActionAccept);

        // Cannot directly interact with GUI, as this method here is called by the DiscoWall Service!
//        Toast.makeText(context, "= DiscoWall =\n" + "caught connection: " + connection, Toast.LENGTH_SHORT).show();
    }

    private void createUndecidedConnectionNotification(final Connections.Connection connection, final int decisionTimeoutInSeconds, final boolean defaultActionAccept) {
        AppUidGroup appUidGroup = watchedAppsManager.getWatchedAppGroupByUid(connection.getUserId());

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
        Intent clickIntent = ShowAppRulesActivity.createActionIntent_decideConnection(context, appUidGroup, connection);
        final PendingIntent pendingClickIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT); // request-code (= 0) has to differ from other pending intents of this notification!

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

        final Notification notification = createUndecidedConnectionNotificationEx(connection, pendingClickIntent, pendingActionIntentAccept, pendingActionIntentBlock, decisionTimeoutInSeconds, defaultActionAccept);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
        notificationManager.notify(DiscoWallConstants.NotificationIDs.pendingPackage, notification); // only one fixed NotificationID required, since there will always be only one pending package (as the NetfilterBridge waits for the response)

        // Expand Statusbar, so that the user can decide on the connection (if setting enabled)
        if (DiscoWallSettings.getInstance().isConnectionDecisionNotificationExpandStatusbar(context)) {
            Log.v(LOG_TAG, "expanding StatusBar in order to show notification (according to user-settings)...");

            try {
                GuiUtils.expandStatusbar(context);
            } catch(Exception e) {
                Log.e(LOG_TAG, "Exception trying to expand statusbar: " + e.getMessage(), e);
            }
        }

        Thread autoTimeoutThread = new Thread() {
            @Override
            public void run() {
                int decisionTimeout = decisionTimeoutInSeconds;
                Log.v(LOG_TAG, "Decision Timeout: thread created. timeout = " + decisionTimeout + " seconds.");

                while (pendingConnectionsManager.isPending(connection)) {
                    if (decisionTimeout <= 0) {
                        Log.d(LOG_TAG, "Decision Timeout: time is up. Default-Action = " + (defaultActionAccept ? "ACCEPT" : "BLOCK"));

                        // Canceling the notification here, so that the user cannot click AFTER the time is up.
                        notificationManager.cancel(DiscoWallConstants.NotificationIDs.pendingPackage);

                        // Perform default-action
                        if (defaultActionAccept)
                            acceptPendingPackage();
                        else
                            blockPendingPackage();

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
        };

        autoTimeoutThread.setDaemon(true); // close thread with app
        autoTimeoutThread.start();
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

    public void decidePackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback actionCallback) {
        FirewallRules.IFirewallPolicyRule packagePolicyRule = getPackageRule(tlPackage, connection);

        Log.d(LOG_TAG, "Matching Rule: " + packagePolicyRule + " @ package: " + tlPackage);

        if (packagePolicyRule != null) {
            // Apply rule-policy:

            switch(packagePolicyRule.getRulePolicy()) {
                case ALLOW:
                    actionCallback.acceptPendingPackage();
                    break;
                case BLOCK:
                    actionCallback.blockPendingPackage();
                    break;
                case INTERACTIVE:
                    decidePackageAcceptedInteractively(tlPackage, connection, actionCallback);
                    break;
                default:
                    throw new RuntimeException("Rule-Policy filter-behavior not implemented: " + packagePolicyRule.getRulePolicy());
            }

        } else {
            // Apply firewall-policy:
            switch (policyManager.getFirewallPolicy()) {
                case ALLOW:
                    actionCallback.acceptPendingPackage();
                    break;
                case BLOCK:
                    actionCallback.blockPendingPackage();
                    break;
                case INTERACTIVE:
                    decidePackageAcceptedInteractively(tlPackage, connection, actionCallback);
                    break;
                default:
                    throw new RuntimeException("Firewall-Policy filter-behavior not implemented: " + policyManager.getFirewallPolicy());
            }

        }
    }

}
