package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallPolicyManager {
    private static final String LOG_TAG = FirewallPolicyManager.class.getSimpleName();
    public enum FirewallPolicy { ALLOW, BLOCK, INTERACTIVE }

    private final FirewallIptableRulesHandler firewallIptableRulesHandler;
    private FirewallPolicyManager.FirewallPolicy firewallUnknownConnectionPolicy;

    public FirewallPolicyManager(FirewallIptableRulesHandler firewallIptableRulesHandler) {
        this.firewallIptableRulesHandler = NetfilterFirewallRulesHandler.instance;
        this.firewallUnknownConnectionPolicy = FirewallPolicyManager.FirewallPolicy.INTERACTIVE; // Is default-policy as defined by NetfilterBridgeIptablesHandler
    }

    public FirewallPolicyManager.FirewallPolicy getFirewallPolicy() {
        return firewallUnknownConnectionPolicy;
    }

    /**
     * Changes the {@link FirewallPolicy}. It can be decided whether to only change the stored variable state, or to actually write the changes to iptables.
     * @param policy policy to be applied. Has no effect if <b>applyRuleToIptables</b> is set to false.
     * @param applyRuleToIptables if false, the changes are only stored within the variable.
     * @throws FirewallExceptions.FirewallException
     */
    public void setFirewallPolicy(FirewallPolicyManager.FirewallPolicy policy, boolean applyRuleToIptables) throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "changing firewall policy: " + firewallUnknownConnectionPolicy + " --> " + policy);
        // it is allowed to re-apply the same policy, so that it is possible to re-write the iptable rule

        /* IMPORTANT NOTE:
         * As usually only SYN/RST/FIN packages are of interest to the firewall,
         * changing the firewall-policy will NOT effect already established  connections.
         *    [ Theoretically only relevant, when current policy is INTERACTIVE/ALLOW (i.e. when there can be active connections)
         *      BUT: Connections could have been established even before changing to BLOCK, so also in BLOCK there could be existing connections ]
         *
         * ==> When changing to BLOCKED policy,ANY package must be blocked, so that running connections are closed too.
         *    BUT: If all packages were to be forwarded into the firewall, the interactive-
         *    ==> Only filter flags within interactive-chain
         */

        if (applyRuleToIptables) {
            try {
                switch(policy) {
                    case ALLOW:
                        firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.ACCEPT_PACKAGE);
                        break;
                    case BLOCK:
                        firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.REJECT_PACKAGE);
                        break;
                    case INTERACTIVE:
                        firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.INTERACTIVE);
                        break;
                }
            } catch (ShellExecuteExceptions.ShellExecuteException e) {
                throw new FirewallExceptions.FirewallException("Unable to change firewall policy du to error: " + e.getMessage(), e);
            }
        }

        firewallUnknownConnectionPolicy = policy;
    }
}
