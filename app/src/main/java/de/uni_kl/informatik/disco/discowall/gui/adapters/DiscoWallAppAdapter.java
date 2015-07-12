package de.uni_kl.informatik.disco.discowall.gui.adapters;

import android.content.Context;

import de.uni_kl.informatik.disco.discowall.R;

public class DiscoWallAppAdapter extends AppAdapter {
    public DiscoWallAppAdapter(Context context) {
        super(
                context,
                R.layout.list_item_app_infos,
                AppAdapter.fetchAppsByLaunchIntent(context, false),
                R.id.list_item_app_infos__app_name,
                R.id.list_item_app_infos__app_package,
                R.id.list_item_app_infos__rules_infos,
                R.id.list_item_app_infos__app_icon,
                R.id.list_item_app_infos__app_checkbox
        );
    }
}
