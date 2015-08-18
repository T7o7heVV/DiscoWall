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
    private final DecisionNotificationHelper decisionNotificationHelper;
    private final Context context;

    private final PendingConnectionsManager pendingConnectionsManager = new PendingConnectionsManager();
    private final TemporaryConnectionRulesManager tempRulesManager = new TemporaryConnectionRulesManager();

    public FirewallPackageFilter(Context context, FirewallPolicyManager policyManager, FirewallRulesManager rulesManager, WatchedAppsManager watchedAppsManager) {
        this.context = context;
        this.policyManager = policyManager;
        this.rulesManager = rulesManager;
        this.watchedAppsManager = watchedAppsManager;

        this.decisionNotificationHelper = new DecisionNotificationHelper(context, this, pendingConnectionsManager);
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
        decisionNotificationHelper.createUndecidedConnectionNotification(connection, decisionTimeout, defaultActionAccept);

        // Cannot directly interact with GUI, as this method here is called by the DiscoWall Service!
//        Toast.makeText(context, "= DiscoWall =\n" + "caught connection: " + connection, Toast.LENGTH_SHORT).show();
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
