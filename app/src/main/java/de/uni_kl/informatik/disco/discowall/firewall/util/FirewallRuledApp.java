package de.uni_kl.informatik.disco.discowall.firewall.util;

import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;

public class FirewallRuledApp {
    private final AppUidGroup uidGroup;
    private final List<FirewallRules.IFirewallRule> rules;
    private final boolean isMonitored;

    public FirewallRuledApp(AppUidGroup uidGroup, List<FirewallRules.IFirewallRule> rules, boolean isMonitored) {
        this.uidGroup = uidGroup;
        this.rules = rules;
        this.isMonitored = isMonitored;
    }

    public AppUidGroup getUidGroup() {
        return uidGroup;
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules() {
        return new LinkedList<>(rules);
    }

    public boolean isMonitored() {
        return isMonitored;
    }
}
