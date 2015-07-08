package de.uni_kl.informatik.disco.discowall.utils.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.R;

public class AppAdapter extends ArrayAdapter<ApplicationInfo>{
    public interface AdapterHandler {
        void onRowCreate(AppAdapter adapter, ApplicationInfo appInfo, TextView appNameWidget, TextView appPackageNameWidget, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget);

        void onAppNameClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview);
        void onAppPackageClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget);
        void onAppIconClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget);
        void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, ApplicationInfo appInfo, CheckBox appWatchedCheckboxWidget, boolean isChecked);
    }


    private static final String LOG_TAG = AppAdapter.class.getSimpleName();
    public final int listLayoutResourceId, app_name_viewID, app_package_viewID, app_icon_viewID, app_checkbox_viewID;

    private List<ApplicationInfo> appList = null;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterHandler adapterHandler;

    private Context context;
    private PackageManager packageManager;

    public AppAdapter(Context context, int listLayoutResourceId, int app_name_viewID, int app_package_viewID, int app_icon_viewID, int app_checkbox_viewID) {
        this(context, listLayoutResourceId, fetchAppsByLaunchIntent(context), app_name_viewID, app_package_viewID, app_icon_viewID, app_checkbox_viewID);
    }

    public AppAdapter(Context context, int listLayoutResourceId, List<ApplicationInfo> appsToShow, int app_name_viewID, int app_package_viewID, int app_icon_viewID, int app_checkbox_viewID) {
        super(context, listLayoutResourceId, appsToShow);

        this.packageManager = context.getPackageManager();
        this.context = context;
        this.appList = appsToShow;

        this.listLayoutResourceId = listLayoutResourceId;
        this.app_name_viewID = app_name_viewID;
        this.app_package_viewID = app_package_viewID;
        this.app_icon_viewID = app_icon_viewID;
        this.app_checkbox_viewID = app_checkbox_viewID;
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

    public AdapterHandler getAdapterHandler() {
        return adapterHandler;
    }

    public void setAdapterHandler(AdapterHandler adapterHandler) {
        this.adapterHandler = adapterHandler;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public LinkedList<ApplicationInfo> getAppList() {
        return new LinkedList<>(appList);
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

    private void onListItemClick(int position, View layoutView, ViewGroup parent, View clickedWidget) {
        if (onItemClickListener != null)
            onItemClickListener.onItemClick((AdapterView<?>) parent, clickedWidget, position, position);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;

        if(convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(listLayoutResourceId, null);
        } else {
            view = convertView;
        }

        final ApplicationInfo applicationInfo = appList.get(position);
        if (applicationInfo == null)
            return view;

        final TextView appNameTextView = (TextView) view.findViewById(app_name_viewID);
        final TextView appPackageNameTextView = (TextView) view.findViewById(app_package_viewID);
        final ImageView appIconImageView = (ImageView) view.findViewById(app_icon_viewID);
        final CheckBox appWatchedCheckBox = (CheckBox) view.findViewById(app_checkbox_viewID);

        // Write App-Info to gui:
        appNameTextView.setText(applicationInfo.loadLabel(packageManager));
        appPackageNameTextView.setText(applicationInfo.packageName);
        appIconImageView.setImageDrawable(applicationInfo.loadIcon(packageManager));

        appNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; appName click");

                onListItemClick(position, view, parent, appNameTextView);

                if (adapterHandler != null)
                    adapterHandler.onAppNameClicked(AppAdapter.this, applicationInfo, (TextView) view.findViewById(app_name_viewID));
            }
        });
        appPackageNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; packageName click");

                onListItemClick(position, view, parent, appPackageNameTextView);

                if (adapterHandler != null)
                    adapterHandler.onAppPackageClicked(AppAdapter.this, applicationInfo, (TextView) view.findViewById(app_package_viewID));
            }
        });
        appIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; iconView click");

                onListItemClick(position, view, parent, appIconImageView);

                if (adapterHandler != null)
                    adapterHandler.onAppIconClicked(AppAdapter.this, applicationInfo, (ImageView) view.findViewById(app_icon_viewID));
            }
        });
        appWatchedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; checkboxView check state changed to: " + isChecked);

                onListItemClick(position, view, parent, appWatchedCheckBox);

                if (adapterHandler != null)
                    adapterHandler.onAppWatchedStateCheckboxCheckedChanged(AppAdapter.this, applicationInfo, (CheckBox) buttonView, isChecked);
            }
        });

        // Call on-row-create event
        if (adapterHandler != null)
            adapterHandler.onRowCreate(this, applicationInfo, appNameTextView, appPackageNameTextView, appIconImageView, appWatchedCheckBox);

        return view;
    }
}
