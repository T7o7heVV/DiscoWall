package de.uni_kl.informatik.disco.discowall.firewall;

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
import de.uni_kl.informatik.disco.discowall.firewall.subsystems.SubsystemWatchedApps;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class FirewallPackageFilter {
    private static final String LOG_TAG = FirewallPackageFilter.class.getSimpleName();
    private final FirewallPolicyManager policyManager;
    private final FirewallRulesManager rulesManager;
    private final WatchedAppsManager watchedAppsManager;
    private final Context context;

    public FirewallPackageFilter(Context context, FirewallPolicyManager policyManager, FirewallRulesManager rulesManager, WatchedAppsManager watchedAppsManager) {
        this.context = context;
        this.policyManager = policyManager;
        this.rulesManager = rulesManager;
        this.watchedAppsManager = watchedAppsManager;
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
        boolean defaultActionAccept = DiscoWallSettings.getInstance().isNewConnectionDefaultDecisionAccept(context); // default-action (ACCEPT/BLOCK)
        int decisionTimeout = DiscoWallSettings.getInstance().getNewConnectionDecisionTimeoutMS(context); // time after which the default-action will be taken

        AppUidGroup appUidGroup = watchedAppsManager.getWatchedAppGroupByUid(tlPackage.getUserId());
        ShowAppRulesActivity.showNewRuleDialog(context, appUidGroup, tlPackage, true);

//        Intent intent = new Intent(context, ShowAppRulesActivity.class);
//        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
//
//        // build notification
//        // the addAction re-use the same intent to keep the example short
//        Notification n  = new Notification.Builder(this)
//                .setContentTitle("New mail from " + "test@gmail.com")
//                .setContentText("Subject")
//                .setSmallIcon(R.drawable.icon)
//                .setContentIntent(pIntent)
//                .setAutoCancel(true)
//                .addAction(R.drawable.icon, "Call", pIntent)
//                .addAction(R.drawable.icon, "More", pIntent)
//                .addAction(R.drawable.icon, "And more", pIntent).build();
//
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0, n);


        // TODO!
        Log.w(LOG_TAG, "INTERACTIVE decision is NOT impelmented yet! Accepting package...");
        actionCallback.acceptPackage(tlPackage);
    }

    public void decidePackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection, NetfilterBridgeCommunicator.PackageActionCallback actionCallback) {
        FirewallRules.IFirewallPolicyRule packagePolicyRule = getPackageRule(tlPackage, connection);

        Log.i("RULE-MATCH", "Package: " + tlPackage);
        Log.i("RULE-MATCH", "Matching Rule: " + packagePolicyRule);

        if (packagePolicyRule != null) {
            // Apply rule-policy:

            switch(packagePolicyRule.getRulePolicy()) {
                case ALLOW:
                    actionCallback.acceptPackage(tlPackage);
                    break;
                case BLOCK:
                    actionCallback.blockPackage(tlPackage);
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
                    actionCallback.acceptPackage(tlPackage);
                    break;
                case BLOCK:
                    actionCallback.blockPackage(tlPackage);
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
