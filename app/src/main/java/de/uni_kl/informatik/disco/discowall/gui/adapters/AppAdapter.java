package de.uni_kl.informatik.disco.discowall.gui.adapters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
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
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;

public class AppAdapter extends ArrayAdapter<AppUidGroup>{
    public interface AdapterHandler {
        void onRowCreate(AppAdapter adapter, AppUidGroup appGroup, TextView appNameWidget, TextView appPackageNameWidget, TextView appRuleInfoTextView, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget);

        // Clicks:
        void onAppNameClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appNameWidgetview);
        void onAppPackageClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget);
        void onAppOptionalInfoClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appInfoWidget);
        void onAppIconClicked(AppAdapter appAdapter, AppUidGroup appGroup, ImageView appIconImageWidget);

        // LongClicks:
        boolean onAppNameLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appNameWidgetview);
        boolean onAppPackageLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget);
        boolean onAppOptionalInfoLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget);
        boolean onAppIconLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, ImageView appIconImageWidget);

        // Checkbox-Checks:
        void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, AppUidGroup appGroup, CheckBox appWatchedCheckboxWidget, boolean isChecked);
    }


    private static final String LOG_TAG = AppAdapter.class.getSimpleName();
    public final int listLayoutResourceId, app_name_viewID, app_package_viewID, app_optionalInfo_viewID, app_icon_viewID, app_checkbox_viewID;

    private final List<AppUidGroup> appList;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterView.OnItemLongClickListener onItemLongClickListener;
    private AdapterHandler adapterHandler;

    private Context context;

    public AppAdapter(Context context, int listLayoutResourceId, List<AppUidGroup> appsToShow, int app_name_viewID, int app_package_viewID, int app_optionalInfo_viewID, int app_icon_viewID, int app_checkbox_viewID) {
        super(context, listLayoutResourceId, appsToShow);

        this.context = context;
        this.appList = appsToShow;

        this.listLayoutResourceId = listLayoutResourceId;
        this.app_name_viewID = app_name_viewID;
        this.app_package_viewID = app_package_viewID;
        this.app_optionalInfo_viewID = app_optionalInfo_viewID;
        this.app_icon_viewID = app_icon_viewID;
        this.app_checkbox_viewID = app_checkbox_viewID;
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

    public LinkedList<AppUidGroup> getAppList() {
        return new LinkedList<>(appList);
    }

    @Override
    public int getCount() {
        return ((null != appList) ? appList.size() : 0);
    }

    @Override
    public AppUidGroup getItem(int position) {
        return appList.get(position);
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

        final AppUidGroup appUidGroup = appList.get(position);
        if (appUidGroup == null)
            return view;

        final TextView appNameTextView = (TextView) view.findViewById(app_name_viewID);
        TextView appPackageNameTextView = (TextView) view.findViewById(app_package_viewID);
        TextView appOptionalInfosTextView = (TextView) view.findViewById(app_optionalInfo_viewID);
        ImageView appIconImageView = (ImageView) view.findViewById(app_icon_viewID);
        CheckBox appWatchedCheckBox = (CheckBox) view.findViewById(app_checkbox_viewID);

        // Write App-Info to gui:
        appNameTextView.setText(appUidGroup.getName());
        appPackageNameTextView.setText(appUidGroup.getPackageName());
        appIconImageView.setImageDrawable(appUidGroup.getIcon());


        //--------------------------------------------------------------------------------------------------------------------------------------
        // Checkbox-Checks
        //--------------------------------------------------------------------------------------------------------------------------------------
        appWatchedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; checkboxView check state changed to: " + isChecked);

                onListItemClick(position, view, (AdapterView<?>) parent, buttonView);

                if (adapterHandler != null)
                    adapterHandler.onAppWatchedStateCheckboxCheckedChanged(AppAdapter.this, appUidGroup, (CheckBox) buttonView, isChecked);
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
                    adapterHandler.onAppNameClicked(AppAdapter.this, appUidGroup, (TextView) view.findViewById(app_name_viewID));
            }
        });
        appPackageNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; packageName click");

                onListItemClick(position, view, (AdapterView<?>) parent, (TextView) view.findViewById(app_package_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppPackageClicked(AppAdapter.this, appUidGroup, (TextView) view.findViewById(app_package_viewID));
            }
        });
        appOptionalInfosTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; optionalInfo click");

                onListItemClick(position, view, (AdapterView<?>) parent, (TextView) view.findViewById(app_optionalInfo_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppPackageClicked(AppAdapter.this, appUidGroup, (TextView) view.findViewById(app_optionalInfo_viewID));
            }
        });
        appIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "app '" + appNameTextView.getText() + "'; iconView click");

                onListItemClick(position, view, (AdapterView<?>) parent, (ImageView) view.findViewById(app_icon_viewID));

                if (adapterHandler != null)
                    adapterHandler.onAppIconClicked(AppAdapter.this, appUidGroup, (ImageView) view.findViewById(app_icon_viewID));
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
                    return adapterHandler.onAppNameLongClicked(AppAdapter.this, appUidGroup, (TextView) v);
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
                    return adapterHandler.onAppPackageLongClicked(AppAdapter.this, appUidGroup, (TextView) v);
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
                    return adapterHandler.onAppOptionalInfoLongClicked(AppAdapter.this, appUidGroup, (TextView) v);
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
                    return adapterHandler.onAppIconLongClicked(AppAdapter.this, appUidGroup, (ImageView) v);
                else
                    return false;
            }
        });
        //--------------------------------------------------------------------------------------------------------------------------------------

        // Call on-row-create event
        if (adapterHandler != null)
            adapterHandler.onRowCreate(this, appUidGroup, appNameTextView, appPackageNameTextView, appOptionalInfosTextView, appIconImageView, appWatchedCheckBox);

        return view;
    }
}
