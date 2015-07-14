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
import de.uni_kl.informatik.disco.discowall.utils.apps.App;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class WatchedAppsPreferencesManager {
    private final Context firewallServiceContext;
    private final HashMap<Integer, AppUidGroup> uidToExistingAppGroupsMap = new HashMap<>();
    private final HashMap<Integer, AppUidGroup> uidToWatchedAppGroupMap = new HashMap<>();

    public WatchedAppsPreferencesManager(FirewallService firewallServiceContext) {
        this.firewallServiceContext = firewallServiceContext;

        updateWatchableAppsList();
        uidToWatchedAppGroupMap.putAll(AppUidGroup.createUidToGroupMap(loadWatchedAppGroups()));
    }

    public void updateWatchableAppsList() {
        List<ApplicationInfo> appInfos = App.fetchAppInfosByLaunchIntent(firewallServiceContext, false);
        LinkedList<AppUidGroup> updatedListOfWatchableApps = AppUidGroup.createGroupsFromAppInfoList(appInfos, firewallServiceContext);
        HashMap<Integer, AppUidGroup> updatedMapOfWatchableApps = AppUidGroup.createUidToGroupMap(updatedListOfWatchableApps);

        uidToExistingAppGroupsMap.clear();
        uidToExistingAppGroupsMap.putAll(updatedMapOfWatchableApps);
    }

    private void storeWatchedAppsUIDs(Set<Integer> uidSet) {
        DiscoWallSettings.getInstance().setWatchedAppsUIDs(firewallServiceContext, uidSet);
    }

    private Set<Integer> loadWatchedAppsUIDs() {
        return new HashSet<>(DiscoWallSettings.getInstance().getWatchedAppsUIDs(firewallServiceContext));
    }

    public void setWatchedApps(List<AppUidGroup> groups) {
        Set<Integer> watchedAppUIDs = new HashSet<>();

        for(AppUidGroup group : groups)
            watchedAppUIDs.add(group.getUid());

        storeWatchedAppsUIDs(watchedAppUIDs);

        uidToWatchedAppGroupMap.clear();
        uidToWatchedAppGroupMap.putAll(AppUidGroup.createUidToGroupMap(groups));
    }

    public LinkedList<AppUidGroup> getExistingApps() {
        return new LinkedList<>(uidToExistingAppGroupsMap.values());
    }

    public LinkedList<AppUidGroup> getWatchedApps() {
        return new LinkedList<>(uidToWatchedAppGroupMap.values());
    }

    private List<AppUidGroup> loadWatchedAppGroups() {
        Set<Integer> appsUIDs = loadWatchedAppsUIDs();
        LinkedList<AppUidGroup> watchedGroups = new LinkedList<>();

        for(AppUidGroup group : uidToExistingAppGroupsMap.values()) {
            if (appsUIDs.contains(group.getUid()))
                watchedGroups.add(group);
        }

        return watchedGroups;
    }

    public void setAppWatched(AppUidGroup group, boolean watched) {
        HashSet<Integer> uidSet = new HashSet<>();

        for(AppUidGroup aGroup : uidToWatchedAppGroupMap.values())
            uidSet.add(aGroup.getUid());

        if (watched) {
            uidSet.add(group.getUid());
            uidToWatchedAppGroupMap.put(group.getUid(), group);
        } else {
            uidSet.remove(group.getUid());
            uidToWatchedAppGroupMap.remove(group.getUid());
        }

        storeWatchedAppsUIDs(uidSet);
    }

    public boolean isAppWatched(int appUID) {
        return uidToWatchedAppGroupMap.get(appUID) != null;
    }

    public boolean isAppWatched(AppUidGroup group) {
        return isAppWatched(group.getUid());
    }

    public AppUidGroup getExistingAppByUid(int uid) {
        return uidToExistingAppGroupsMap.get(uid);
    }

    public AppUidGroup getWatchedAppByUid(int uid) {
        return uidToWatchedAppGroupMap.get(uid);
    }

//    public static boolean applicationListContainsApp(List<ApplicationInfo> applicationInfoList, ApplicationInfo searchedAppInfo) {
//        for(ApplicationInfo info : applicationInfoList)
//            if (info.packageName.equals(searchedAppInfo.packageName))
//                return true;
//
//        return false;
//    }
//
//    public static Set<String> applicationListToPackageNameSet(List<ApplicationInfo> watchedApps) {
//        Set<String> watchedAppsPackageNames = new HashSet<>();
//
//        for(ApplicationInfo info : watchedApps)
//            watchedAppsPackageNames.add(info.packageName);
//
//        return watchedAppsPackageNames;
//    }
//
//    public static HashMap<ApplicationInfo, String> applicationInfoToPackageNameMap(List<ApplicationInfo> apps) {
//        HashMap<ApplicationInfo, String> appInfoToPackageNameHash = new HashMap<>();
//
//        for(ApplicationInfo info : apps)
//            appInfoToPackageNameHash.put(info, info.packageName);
//
//        return appInfoToPackageNameHash;
//    }
//
//    public static HashMap<String, ApplicationInfo> packageNameToApplicationInfoMap(List<ApplicationInfo> apps) {
//        HashMap<String, ApplicationInfo> packageNameToAppMap = new HashMap<>();
//
//        for(ApplicationInfo info : apps)
//            packageNameToAppMap.put(info.packageName, info);
//
//        return packageNameToAppMap;
//    }


}
