package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;

public class SubsystemRulesManager extends FirewallSubsystem{
    private final FirewallRulesManager rulesManager;

    protected SubsystemRulesManager(Firewall firewall, FirewallService firewallServiceContext, FirewallRulesManager rulesManager) {
        super(firewall, firewallServiceContext);
        this.rulesManager = rulesManager;
    }
}
