package de.uni_kl.informatik.disco.discowall.gui;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.utils.gui.AppAdapter;

public class DiscoWallAppAdapter extends AppAdapter {
    public DiscoWallAppAdapter(Context context) {
        super(context, R.layout.list_item_app_infos, AppAdapter.fetchAppsByLaunchIntent(context, false), R.id.list_item_app_infos__app_name, R.id.list_item_app_infos__app_package, R.id.list_item_app_infos__app_icon);
    }

}
