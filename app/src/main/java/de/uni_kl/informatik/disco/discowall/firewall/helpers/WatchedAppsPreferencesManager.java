package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppAdapter;
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

    public static boolean applicationListContainsApp(List<ApplicationInfo> applicationInfoList, ApplicationInfo searchedAppInfo) {
        for(ApplicationInfo info : applicationInfoList)
            if (info.packageName.equals(searchedAppInfo.packageName))
                return true;

        return false;
    }

    public static Set<String> applicationListToPackageNameSet(List<ApplicationInfo> watchedApps) {
        Set<String> watchedAppsPackageNames = new HashSet<>();

        for(ApplicationInfo info : watchedApps)
            watchedAppsPackageNames.add(info.packageName);

        return watchedAppsPackageNames;
    }

    public static HashMap<ApplicationInfo, String> applicationInfoToPackageNameMap(List<ApplicationInfo> apps) {
        HashMap<ApplicationInfo, String> appInfoToPackageNameHash = new HashMap<>();

        for(ApplicationInfo info : apps)
            appInfoToPackageNameHash.put(info, info.packageName);

        return appInfoToPackageNameHash;
    }

    public static HashMap<String, ApplicationInfo> packageNameToApplicationInfoMap(List<ApplicationInfo> apps) {
        HashMap<String, ApplicationInfo> packageNameToAppMap = new HashMap<>();

        for(ApplicationInfo info : apps)
            packageNameToAppMap.put(info.packageName, info);

        return packageNameToAppMap;
    }

//    public HashMap<ApplicationInfo, Boolean> createAppsToWatchStateMap() {
//        return createAppsToWatchStateMap(getWatchableApps());
//    }
//
//    public static HashMap<ApplicationInfo, Boolean> createAppsToWatchStateMap(List<ApplicationInfo> allApps) {
//        HashMap<ApplicationInfo, Boolean> appToWatchStateMap = new HashMap<>();
//
//        HashMap<String, ApplicationInfo> packageNameToAppMap = packageNameToApplicationInfoMap(allApps);
//
//        Set<String> allAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(allApps);
//        Set<String> currentlyWatchedAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(allApps);
//
//        // Disable watching of those apps, which are not in the "watchedAppsSet"
//        for(String appPackageName : allAppsSet) {
//            ApplicationInfo appInfo = packageNameToAppMap.get(appPackageName);
//            boolean isWatched = currentlyWatchedAppsSet.contains(appPackageName);
//
//            appToWatchStateMap.put(appInfo, isWatched);
//        }
//
//        return appToWatchStateMap;
//    }
}
