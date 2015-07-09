package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.utils.gui.AppAdapter;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class WatchedAppsPreferencesManager {
    private final Context firewallServiceContext;

    public WatchedAppsPreferencesManager(FirewallService firewallServiceContext) {
        this.firewallServiceContext = firewallServiceContext;
    }

    private void storeWatchedAppsPackages(Set<String> packageNames) {
        DiscoWallSettings.getInstance().setWatchedAppsPackages(firewallServiceContext, packageNames);
    }

    private Set<String> loadWatchedAppsPackages() {
        Set<String> appPackagesSet = DiscoWallSettings.getInstance().getWatchedAppsPackages(firewallServiceContext);
        return appPackagesSet;
    }

    public void setWatchedApps(List<ApplicationInfo> watchedApps) {
        Set<String> watchedAppsPackageNames = new HashSet<>();

        for(ApplicationInfo info : watchedApps)
            watchedAppsPackageNames.add(info.packageName);

        storeWatchedAppsPackages(watchedAppsPackageNames);
    }

    public List<ApplicationInfo> getWatchableApps() {
        return AppAdapter.fetchAppsByLaunchIntent(firewallServiceContext, false); // not buffering so that the apps-list is always up-to-date;
    }

    public List<ApplicationInfo> getWatchedApps() {
        List<ApplicationInfo> installedApps = getWatchableApps();
        Set<String> watchedAppsPackages = loadWatchedAppsPackages();

        LinkedList<ApplicationInfo> watchedApps = new LinkedList<>();

        for(ApplicationInfo app : installedApps) {
            Log.i("MATCH", app.packageName + " --> " + watchedAppsPackages.contains(app.packageName));

            if (watchedAppsPackages.contains(app.packageName))
                watchedApps.add(app);
        }

        return watchedApps;
    }

    public void setAppWatched(ApplicationInfo applicationInfo, boolean watched) {
        // Get Watched-State from discowall settings:
        Set<String> watchedAppsPackages = loadWatchedAppsPackages();

        // Update Watched-State setting
        if (watched)
            watchedAppsPackages.add(applicationInfo.packageName);
        else
            watchedAppsPackages.remove(applicationInfo.packageName);

        // Write-back watched-back setting
        storeWatchedAppsPackages(watchedAppsPackages);
    }

    public boolean isAppWatched(String appPackageName) {
        return loadWatchedAppsPackages().contains(appPackageName);
    }

    public boolean isAppWatched(ApplicationInfo applicationInfo) {
        return isAppWatched(applicationInfo.packageName);
    }

    public static boolean listContainsApp(List<ApplicationInfo> applicationInfoList, ApplicationInfo searchedAppInfo) {
        for(ApplicationInfo info : applicationInfoList)
            if (info.packageName.equals(searchedAppInfo.packageName))
                return true;

        return false;
    }
}
