package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import android.util.Log;

import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class SubsystemWatchedApps extends FirewallSubsystem {
    private static final String LOG_TAG = FirewallSubsystem.class.getSimpleName();

    private final WatchedAppsManager watchedAppsManager;
    private final FirewallIptableRulesHandler iptableRulesManager;

    public SubsystemWatchedApps(Firewall firewall, FirewallService firewallServiceContext, FirewallIptableRulesHandler iptableRulesManager) {
        super(firewall, firewallServiceContext);
        this.iptableRulesManager = iptableRulesManager;
        this.watchedAppsManager = new WatchedAppsManager(firewallServiceContext);
    }

    /**
     * Makes sure the traffic of a specified application will be monitored by the firewall. The configuration is automatically stored persistently.
     * <p></p>
     * <b>Note: </b> If the firewall is not running, this call will have no effect. If the firewall is being started, all watched-states will be restored.
     * @param watchTraffic
     * @throws FirewallExceptions.FirewallException
     */
    public void setAppGroupWatched(AppUidGroup appGroup, boolean watchTraffic) throws FirewallExceptions.FirewallException {
        Log.d(LOG_TAG, "enabling traffic-monitoring for app-group " + appGroup.getName() + " with uid " + appGroup.getUid() + ".");

        if (!firewall.isFirewallStopped()) {
            Log.v(LOG_TAG, "Firewall is running, iptable-rules will be created...");

            try {
                iptableRulesManager.setUserPackagesForwardToFirewall(appGroup.getUid(), watchTraffic);
            } catch (ShellExecuteExceptions.ShellExecuteException e) {
                throw new FirewallExceptions.FirewallException("Error changing watched-state for app(s) by user id " + appGroup.getUid() + ": " + e.getMessage(), e);
            }
        } else {
            Log.v(LOG_TAG, "Firewall not running, iptable-rules will be created on next firewall-activation.");
        }

        // updating watched apps persistent preferences
        watchedAppsManager.setAppGroupWatched(appGroup, watchTraffic);
    }

    public boolean isAppWatched(AppUidGroup appGroup) {
        return watchedAppsManager.isAppGroupWatched(appGroup);
    }

    public LinkedList<AppUidGroup> getWatchedAppGroups() {
        return watchedAppsManager.getWatchedAppGroups();
    }

    public LinkedList<AppUidGroup> getInstalledAppGroups() {
        return watchedAppsManager.getInstalledAppGroups();
    }

    public AppUidGroup getWatchedAppGroupByUid(int uid) {
        return watchedAppsManager.getWatchedAppGroupByUid(uid);
    }

    public AppUidGroup getInstalledAppGroupByUid(int uid) {
        return watchedAppsManager.getInstalledAppGroupByUid(uid);
    }

    public void updateInstalledAppsList() {
        watchedAppsManager.updateInstalledAppsList();
    }
}
