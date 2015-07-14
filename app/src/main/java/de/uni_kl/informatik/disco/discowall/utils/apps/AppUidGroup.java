package de.uni_kl.informatik.disco.discowall.utils.apps;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AppUidGroup extends AppGroup {
    private final int uid;

    public AppUidGroup(List<App> appsWithSameUid) {
        super(appsWithSameUid);

        if (appsWithSameUid.isEmpty())
            throw new IllegalArgumentException("An " + this.getClass().getName() + " cannot be empty!");

        this.uid = this.getApps().getFirst().getUid();

        for(App app : appsWithSameUid) {
            if (app.getUid() != uid)
                throw new IllegalArgumentException("All apps within a group must share the same UID! Found inconsistent UIDs: " + app.getUid() + " vs. " + uid);
        }
    }

    public int getUid() {
        return uid;
    }

    public static LinkedList<AppUidGroup> createGroupsFromAppInfoList(List<ApplicationInfo> appInfos, Context context) {
        LinkedList<App> appsList = new LinkedList<>();

        // group apps by uid
        for(ApplicationInfo appInfo : appInfos)
            appsList.add(new App(appInfo, context));

        return createGroupsFromAppList(appsList, context);
    }

    public static LinkedList<AppUidGroup> createGroupsFromAppList(List<App> apps, Context context) {
        HashMap<Integer, List<App>> uidToInstalledAppMap = new HashMap<>();

        // group apps by uid
        for(App app : apps) {
            List<App> appsWithSameUid = uidToInstalledAppMap.get(app.getUid());

            if (appsWithSameUid == null) {
                appsWithSameUid = new LinkedList<>();
                uidToInstalledAppMap.put(app.getUid(), appsWithSameUid);
            }

            appsWithSameUid.add(app);
        }

        LinkedList<AppUidGroup> groups = new LinkedList<>();

        for(List<App> appsWithSameUid : uidToInstalledAppMap.values())
            groups.add(new AppUidGroup(appsWithSameUid));

        return groups;
    }

    public static HashMap<Integer, AppUidGroup> createUidToGroupMap(List<AppUidGroup> groups) {
        HashMap<Integer, AppUidGroup> uidToGroupMap = new HashMap<>();

        for(AppUidGroup group : groups)
            uidToGroupMap.put(group.getUid(), group);

        return uidToGroupMap;
    }

}
