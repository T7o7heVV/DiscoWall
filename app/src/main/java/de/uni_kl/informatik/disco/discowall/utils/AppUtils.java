package de.uni_kl.informatik.disco.discowall.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class AppUtils {

    public static String getRunningAppNameByPID(Context context, int pid) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if(processInfo.pid == pid)
                return processInfo.processName;
        }

        return null;
    }

    public static List<ResolveInfo> getInstalledAppsByActionMainIntent(Context context) {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return context.getPackageManager().queryIntentActivities(mainIntent, 0);
    }

    public static List<ApplicationInfo> getInstalledApps(Context context) {
        final PackageManager pm = context.getPackageManager();

        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

//        for (ApplicationInfo packageInfo : packages) {
//            Log.d(TAG, "Installed package :" + packageInfo.packageName);
//            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
//            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
//        }

        return packages;
    }

    public static LinkedList<Intent> getInstalledAppsLaunchActivities(Context context) {
        final PackageManager pm = context.getPackageManager();

        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        LinkedList<Intent> launchActivities = new LinkedList<>();

        for (ApplicationInfo packageInfo : packages) {
//            Log.d(TAG, "Installed package :" + packageInfo.packageName);
//            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
//            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));

            launchActivities.add(pm.getLaunchIntentForPackage(packageInfo.packageName));
        }

        return launchActivities;
    }
}
