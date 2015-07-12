package de.uni_kl.informatik.disco.discowall.gui.adapters;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<ApplicationInfo>{
    public interface AdapterHandler {
        void onRowCreate(AppAdapter adapter, ApplicationInfo appInfo, TextView appNameWidget, TextView appPackageNameWidget, TextView appRuleInfoTextView, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget);

        // Clicks:
        void onAppNameClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview);
        void onAppPackageClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget);
        void onAppOptionalInfoClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appInfoWidget);
        void onAppIconClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget);

        // LongClicks:
        boolean onAppNameLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview);
        boolean onAppPackageLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget);
        boolean onAppOptionalInfoLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget);
        boolean onAppIconLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget);

        // Checkbox-Checks:
        void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, ApplicationInfo appInfo, CheckBox appWatchedCheckboxWidget, boolean isChecked);
    }


    private static final String LOG_TAG = AppAdapter.class.getSimpleName();
    public final int listLayoutResourceId, app_name_viewID, app_package_viewID, app_optionalInfo_viewID, app_icon_viewID, app_checkbox_viewID;

    private List<ApplicationInfo> appList = null;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterView.OnItemLongClickListener onItemLongClickListener;
    private AdapterHandler adapterHandler;

    private Context context;
    private PackageManager packageManager;

    public AppAdapter(Context context, int listLayoutResourceId, int app_name_viewID, int app_package_viewID, int app_optionalInfo_viewID, int app_icon_viewID, int app_checkbox_viewID) {
        this(context, listLayoutResourceId, fetchAppsByLaunchIntent(context), app_name_viewID, app_package_viewID, app_optionalInfo_viewID, app_icon_viewID, app_checkbox_viewID);
    }

    public AppAdapter(Context context, int listLayoutResourceId, List<ApplicationInfo> appsToShow, int app_name_viewID, int app_package_viewID, int app_optionalInfo_viewID, int app_icon_viewID, int app_checkbox_viewID) {
        super(context, listLayoutResourceId, appsToShow);

        this.packageManager = context.getPackageManager();
        this.context = context;
        this.appList = appsToShow;

        this.listLayoutResourceId = listLayoutResourceId;
        this.app_name_viewID = app_name_viewID;
        this.app_package_viewID = app_package_viewID;
        this.app_optionalInfo_viewID = app_optionalInfo_viewID;
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

    public AdapterView.OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
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

    private void onListItemClick(int position, View layoutView, AdapterView<?>  parent, View clickedWidget) {
        if (onItemClickListener != null)
            onItemClickListener.onItemClick(parent, clickedWidget, position, position);
    }

    private boolean onListItemLongClick(AdapterView<?> parent, View view, int position) {
        if (onItemLongClickListener != null)
            return onItemLongClickListener.onItemLongClick(parent, view, position, position);
        else
            return false;
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
        TextView appPackageNameTextView = (TextView) view.findViewById(app_package_viewID);
        TextView appOptionalInfosTextView = (TextView) view.findViewById(app_optionalInfo_viewID);
        ImageView appIconImageView = (ImageView) view.findViewById(app_icon_viewID);
        CheckBox appWatchedCheckBox = (CheckBox) view.findViewById(app_checkbox_viewID);

        // Write App-Info to gui:
        appNameTextView.setText(applicationInfo.loadLabel(packageManager));
        appPackageNameTextView.setText(applicationInfo.packageName);
        appIconImageView.setImageDrawable(applicationInfo.loadIcon(packageManager));

        //--------------------------------------------------------------------------------------------------------------------------------------
        // Checkbox-Checks
        //--------------------------------------------------------------------------------------------------------------------------------------
        appWatchedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; checkboxView check state changed to: " + isChecked);

                onListItemClick(position, view, (AdapterView<?>) parent, buttonView);

                if (adapterHandler != null)
                    adapterHandler.onAppWatchedStateCheckboxCheckedChanged(AppAdapter.this, applicationInfo, (CheckBox) buttonView, isChecked);
            }
        });


        //--------------------------------------------------------------------------------------------------------------------------------------
        // Clicks
        //--------------------------------------------------------------------------------------------------------------------------------------
        appNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; appName click");

                onListItemClick(position, view, (AdapterView<?>) parent, (TextView) view.findViewById(app_name_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppNameClicked(AppAdapter.this, applicationInfo, (TextView) view.findViewById(app_name_viewID));
            }
        });
        appPackageNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; packageName click");

                onListItemClick(position, view, (AdapterView<?>) parent, (TextView) view.findViewById(app_package_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppPackageClicked(AppAdapter.this, applicationInfo, (TextView) view.findViewById(app_package_viewID));
            }
        });
        appOptionalInfosTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; optionalInfo click");

                onListItemClick(position, view, (AdapterView<?>) parent, (TextView) view.findViewById(app_optionalInfo_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppPackageClicked(AppAdapter.this, applicationInfo, (TextView) view.findViewById(app_optionalInfo_viewID));
            }
        });
        appIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; iconView click");

                onListItemClick(position, view, (AdapterView<?>) parent, (ImageView) view.findViewById(app_icon_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppIconClicked(AppAdapter.this, applicationInfo, (ImageView) view.findViewById(app_icon_viewID));
            }
        });


        //--------------------------------------------------------------------------------------------------------------------------------------
        // Long-Presses
        //--------------------------------------------------------------------------------------------------------------------------------------
        appNameTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; appName long-click");
                onListItemLongClick((AdapterView<?>) parent, v, position);

                if (adapterHandler != null)
                    return adapterHandler.onAppNameLongClicked(AppAdapter.this, applicationInfo, (TextView) v);
                else
                    return false;
            }
        });
        appPackageNameTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; packageName long-click");
                onListItemLongClick((AdapterView<?>) parent, v, position);

                if (adapterHandler != null)
                    return adapterHandler.onAppPackageLongClicked(AppAdapter.this, applicationInfo, (TextView) v);
                else
                    return false;
            }
        });
        appOptionalInfosTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; optionalInfo long-click");
                onListItemLongClick((AdapterView<?>) parent, v, position);

                if (adapterHandler != null)
                    return adapterHandler.onAppOptionalInfoLongClicked(AppAdapter.this, applicationInfo, (TextView) v);
                else
                    return false;
            }
        });
        appIconImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; iconView long-click");
                onListItemLongClick((AdapterView<?>) parent, v, position);

                if (adapterHandler != null)
                    return adapterHandler.onAppIconLongClicked(AppAdapter.this, applicationInfo, (ImageView) v);
                else
                    return false;
            }
        });
        //--------------------------------------------------------------------------------------------------------------------------------------


        // Call on-row-create event
        if (adapterHandler != null)
            adapterHandler.onRowCreate(this, applicationInfo, appNameTextView, appPackageNameTextView, appOptionalInfosTextView, appIconImageView, appWatchedCheckBox);

        return view;
    }
}
