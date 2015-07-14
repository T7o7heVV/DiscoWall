package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsPreferencesManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.utils.apps.App;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class SubsystemWatchedApps extends FirewallSubsystem {
    private static final String LOG_TAG = FirewallSubsystem.class.getSimpleName();

    private final WatchedAppsPreferencesManager watchedAppsManager;
    private final FirewallIptableRulesHandler iptableRulesManager;

    public SubsystemWatchedApps(Firewall firewall, FirewallService firewallServiceContext, FirewallIptableRulesHandler iptableRulesManager) {
        super(firewall, firewallServiceContext);
        this.iptableRulesManager = iptableRulesManager;
        this.watchedAppsManager = new WatchedAppsPreferencesManager(firewallServiceContext);
    }

    public LinkedList<AppUidGroup> getWatchableApps() {
        return watchedAppsManager.getExistingApps();
    }

//    /**
//     * Disables watching of all apps besides those who are specified within the list.
//     * @param appsToWatch
//     * @throws FirewallExceptions.FirewallException
//     */
//    public void setWatchedApps(List<ApplicationInfo> appsToWatch) throws FirewallExceptions.FirewallException {
//        HashMap<String, ApplicationInfo> packageNameToAppInfoMap = WatchedAppsPreferencesManager.packageNameToApplicationInfoMap(getWatchedApps());
//
//        Set<String> appsToWatchSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(appsToWatch);
//        Set<String> currentlyWatchedAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(watchedAppsManager.getWatchedApps());
//
//        // Disable watching of those apps, which are not in the "watchedAppsSet"
//        for(String watchedApp : currentlyWatchedAppsSet) {
//            if (!appsToWatchSet.contains(watchedApp))
//                setAppWatched(packageNameToAppInfoMap.get(watchedApp), false);
//        }
//
//        // List has changed, updating list of currently watched apps:
//        currentlyWatchedAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(watchedAppsManager.getWatchedApps());
//
//        // Enable watching of those apps, which are in the "appsToWatchSet" (and not currently watched)
//        for(ApplicationInfo appToWatch : appsToWatch) {
//            if (!currentlyWatchedAppsSet.contains(appToWatch.packageName)) // checking via Set.contains(), as this can be done within O(n), whereas isAppWatched() takes O(n²)
//                setAppWatched(appToWatch, true);
//        }
//    }

    /**
     * Makes sure the traffic of a specified application will be monitored by the firewall. The configuration is automatically stored persistently.
     * <p></p>
     * <b>Note: </b> If the firewall is not running, this call will have no effect. If the firewall is being started, all watched-states will be restored.
     * @param watchTraffic
     * @throws FirewallExceptions.FirewallException
     */
    public void setAppWatched(AppUidGroup appGroup, boolean watchTraffic) throws FirewallExceptions.FirewallException {
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
        watchedAppsManager.setAppWatched(appGroup, watchTraffic);
    }

    public boolean isAppWatched(AppUidGroup appGroup) {
        return watchedAppsManager.isAppWatched(appGroup);
    }

    public LinkedList<AppUidGroup> getWatchedApps() {
        return watchedAppsManager.getWatchedApps();
    }

    public AppUidGroup getWatchedAppByUid(int uid) {
        return watchedAppsManager.getWatchedAppByUid(uid);
    }

    public AppUidGroup getWatchableAppByUid(int uid) {
        return watchedAppsManager.getExistingAppByUid(uid);
    }
}
