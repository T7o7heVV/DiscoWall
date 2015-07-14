package de.uni_kl.informatik.disco.discowall.utils.apps;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class App {
    private final Context context;

    private final String name;
    private final String packageName;
    private final int uid;
    private final Drawable icon;

    public App(Context context, String name, String packageName, int uid, Drawable icon) {
        this.context = context;
        this.name = name;
        this.packageName = packageName;
        this.uid = uid;
        this.icon = icon;
    }

    public App(ApplicationInfo appInfo, Context context) {
        this.context = context;

        PackageManager packageManager = context.getPackageManager();
        this.name = appInfo.loadLabel(packageManager) + "";
        this.packageName = appInfo.packageName;
        this.uid = appInfo.uid;
        this.icon = appInfo.loadIcon(packageManager);
//        this.description = appInfo.loadDescription(packageManager);
    }

    public static LinkedList<App> createAppsList(List<ApplicationInfo> appInfos, Context context) {
        LinkedList<App> apps = new LinkedList<>();

        for(ApplicationInfo appInfo : appInfos)
            apps.add(new App(appInfo, context));

        return apps;
    }

    public static LinkedList<App> fetchAppsByLaunchIntent(Context context, boolean includeAppItself) {
        return createAppsList(fetchAppInfosByLaunchIntent(context, includeAppItself), context);
    }

    public static List<ApplicationInfo> fetchAppInfosByLaunchIntent(Context context, boolean includeAppItself) {
        final PackageManager pm = context.getPackageManager();

//        List<ApplicationInfo> infos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> infos = pm.getInstalledApplications(0); // no need to fetch meta-data, as it takes extra cpu-time
        ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();

        ApplicationInfo appItself = context.getApplicationInfo();

        for(ApplicationInfo appInfo : infos) {
            if (!includeAppItself) {
                if (appItself.packageName.equals(appInfo.packageName))
                    continue;
            }

            if(pm.getLaunchIntentForPackage(appInfo.packageName) != null)
                appList.add(appInfo);
        }

        return appList;
    }

    public Context getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getUid() {
        return uid;
    }

    public Drawable getIcon() {
        return icon;
    }
}
