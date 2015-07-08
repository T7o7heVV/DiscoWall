package de.uni_kl.informatik.disco.discowall.utils.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.R;

public class AppAdapter extends ArrayAdapter<ApplicationInfo>{
    private static final String LOG_TAG = AppAdapter.class.getSimpleName();
    private final int listLayoutResourceId, app_name_viewID, app_package_viewID, app_icon_viewID;

    private List<ApplicationInfo> appList = null;
    private Context context;
    private PackageManager packageManager;

    public AppAdapter(Context context, int listLayoutResourceId, int app_name_viewID, int app_package_viewID, int app_icon_viewID) {
        this(context, listLayoutResourceId, fetchAppsByLaunchIntent(context), app_name_viewID, app_package_viewID, app_icon_viewID);
    }

    public AppAdapter(Context context, int listLayoutResourceId, List<ApplicationInfo> appsToShow, int app_name_viewID, int app_package_viewID, int app_icon_viewID) {
        super(context, listLayoutResourceId, appsToShow);

        this.packageManager = context.getPackageManager();
        this.context = context;
        this.appList = appsToShow;

        this.listLayoutResourceId = listLayoutResourceId;
        this.app_name_viewID = app_name_viewID;
        this.app_package_viewID = app_package_viewID;
        this.app_icon_viewID = app_icon_viewID;
    }

    public static List<ApplicationInfo> fetchAppsByLaunchIntent(Context context, boolean includeAppItself) {
        if (includeAppItself)
            return fetchAppsByLaunchIntent(context);
        else
            return fetchAppsByLaunchIntentWithoutAppItself(context);
    }

    private static List<ApplicationInfo> fetchAppsByLaunchIntentWithoutAppItself(Context context) {
        LinkedList<ApplicationInfo> apps = new LinkedList<>();
        ApplicationInfo appItself = context.getApplicationInfo();

        for(ApplicationInfo appInfo : fetchAppsByLaunchIntent(context)) {
            // skip app itself
            if (appItself.packageName.equals(appInfo.packageName)) {
                Log.v(LOG_TAG, "Found host-app itself: " + appInfo.packageName + " --> skipping as requested.");
                continue;
            }

            apps.add(appInfo);
        }

        return apps;
    }

    public static List<ApplicationInfo> fetchAppsByLaunchIntent(Context context) {
        final PackageManager pm = context.getPackageManager();

        List<ApplicationInfo> infos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();

        Log.v(LOG_TAG, "Fetching list of installed launchable apps...");

        for(ApplicationInfo info : infos) {
            try{
                if(pm.getLaunchIntentForPackage(info.packageName) != null)
                    appList.add(info);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return appList;
    }

    @Override
    public int getCount() {
        return ((null != appList) ? appList.size() : 0);
    }

    @Override
    public ApplicationInfo getItem(int position) {
        return ((null != appList) ? appList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if(null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(listLayoutResourceId, null);
        }

        ApplicationInfo data = appList.get(position);

        if(null != data) {
            TextView appName = (TextView) view.findViewById(app_name_viewID);
            TextView packageName = (TextView) view.findViewById(app_package_viewID);
            ImageView iconView = (ImageView) view.findViewById(app_icon_viewID);

//            TextView appName = (TextView) view.findViewById(R.id.app_name);
//            TextView packageName = (TextView) view.findViewById(R.id.app_package);
//            ImageView iconView = (ImageView) view.findViewById(R.id.app_icon);

            appName.setText(data.loadLabel(packageManager));
            packageName.setText(data.packageName);
            iconView.setImageDrawable(data.loadIcon(packageManager));
        }
        return view;
    }
}
