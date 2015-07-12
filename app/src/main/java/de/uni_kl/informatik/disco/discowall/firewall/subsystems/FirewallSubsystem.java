package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import android.content.Context;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;

abstract class FirewallSubsystem {
    private static final String LOG_TAG = FirewallSubsystem.class.getSimpleName();
    protected final Context firewallServiceContext;
    protected final Firewall firewall;

    protected FirewallSubsystem(Firewall firewall, FirewallService firewallServiceContext) {
        this.firewall = firewall;
        this.firewallServiceContext = firewallServiceContext;
    }
}
