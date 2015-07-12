package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import android.content.pm.ApplicationInfo;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class SubsystemRulesManager extends FirewallSubsystem{
    private final FirewallRulesManager rulesManager;
    private final FirewallIptableRulesHandler iptableRulesManager = NetfilterFirewallRulesHandler.instance;

    public SubsystemRulesManager(Firewall firewall, FirewallService firewallServiceContext, FirewallRulesManager rulesManager) {
        super(firewall, firewallServiceContext);
        this.rulesManager = rulesManager;
    }

    public String getIptableRules(boolean all) throws FirewallExceptions.FirewallException {
        try {
            if (all) {
                return IptablesControl.getRuleInfoText(true, true);
            } else {
                if (!firewall.isFirewallRunning())
                    return "< firewall has to be enabled in order to retrieve firewall rules >";
                return iptableRulesManager.getFirewallRulesText();
            }
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching iptable rules: " + e.getMessage(), e);
        }
    }

    public LinkedList<FirewallRules.IFirewallRule> getAllRules() {
        return new LinkedList<>(rulesManager.getRules());
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules(ApplicationInfo appInfo) {
        return new LinkedList<>(rulesManager.getRules(appInfo.uid));
    }

    public LinkedList<FirewallRules.IFirewallPolicyRule> getPolicyRules(ApplicationInfo appInfo) {
        return rulesManager.getPolicyRules(appInfo.uid);
    }

    public LinkedList<FirewallRules.IFirewallRedirectRule> getRedirectionRules(ApplicationInfo appInfo) {
        return rulesManager.getRedirectionRules(appInfo.uid);
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(ApplicationInfo appInfo, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy rulePolicy) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return rulesManager.createTransportLayerRule(appInfo.uid, sourceFilter, destinationFilter, deviceFilter, protocolFilter, rulePolicy);
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(ApplicationInfo appInfo, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        return rulesManager.createTransportLayerRedirectionRule(appInfo.uid, sourceFilter, destinationFilter, deviceFilter, protocolFilter, redirectTo);
    }
}
