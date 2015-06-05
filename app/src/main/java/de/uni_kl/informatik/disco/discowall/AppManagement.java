package de.uni_kl.informatik.disco.discowall;

import android.content.Context;

public class AppManagement {
    private Context context;
    private DiscoWallSettings settings;

    public Context getContext() {
        return context;
    }
    public DiscoWallSettings getSettings() { return settings;}

    public AppManagement(Context context) {
        this.context = context;
        this.settings = DiscoWallSettings.getInstance();
    }

}
