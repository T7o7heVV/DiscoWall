package de.uni_kl.informatik.disco.discowall.firewall;

import android.content.Context;
import android.util.Log;

import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallPolicyManager;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class FirewallPackageFilter {
    private static final String LOG_TAG = FirewallPackageFilter.class.getSimpleName();
    private final FirewallPolicyManager policyManager;
    private final FirewallRulesManager rulesManager;
    private final Context context;

    public FirewallPackageFilter(Context context, FirewallPolicyManager policyManager, FirewallRulesManager rulesManager) {
        this.context = context;
        this.policyManager = policyManager;
        this.rulesManager = rulesManager;
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
        }

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
