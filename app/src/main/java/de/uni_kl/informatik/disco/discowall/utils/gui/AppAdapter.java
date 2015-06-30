package de.uni_kl.informatik.disco.discowall.utils.gui;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.R;

public class AppAdapter extends ArrayAdapter<ApplicationInfo>{
    private final int list_item_layoudID, app_name_viewID, app_package_viewID, app_icon_viewID;


    private List<ApplicationInfo> appList = null;
    private Context context;
    private PackageManager packageManager;

    public AppAdapter(Context context, int resource, List<ApplicationInfo> objects,
                      int list_item_layoudID, int app_name_viewID, int app_package_viewID, int app_icon_viewID
    ) {
        super(context, resource, objects);

        this.packageManager = context.getPackageManager();
        this.context = context;
        this.appList = objects;

        this.list_item_layoudID = list_item_layoudID;
        this.app_name_viewID = app_name_viewID;
        this.app_package_viewID = app_package_viewID;
        this.app_icon_viewID = app_icon_viewID;
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
            view = layoutInflater.inflate(list_item_layoudID, null);
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
