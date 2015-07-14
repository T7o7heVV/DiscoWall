package de.uni_kl.informatik.disco.discowall.gui.adapters;

import android.content.Context;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.utils.apps.App;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;

public class DiscoWallAppAdapter extends AppAdapter {
    public DiscoWallAppAdapter(Context context) {
        super(
                context,
                R.layout.list_item_app_infos,
                getInstalledApps(context),
                R.id.list_item_app_infos__app_name,
                R.id.list_item_app_infos__app_package,
                R.id.list_item_app_infos__rules_infos,
                R.id.list_item_app_infos__app_icon,
                R.id.list_item_app_infos__app_checkbox
        );
    }

    private static List<AppUidGroup> getInstalledApps(Context context) {
        LinkedList<AppUidGroup> uidGroups = AppUidGroup.createGroupsFromAppList(App.fetchAppsByLaunchIntent(context, false), context);

        // sort groups by name
        Collections.sort(uidGroups, new Comparator<AppUidGroup>() {
            @Override
            public int compare(AppUidGroup lhs, AppUidGroup rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        return uidGroups;
    }
}
