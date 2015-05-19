package de.uni_kl.informatik.disco.discowall;

import android.content.Context;

public class AppManagement {
    private static AppManagement INSTANCE;
    private Context context;

    public static AppManagement getInstance() {
        return INSTANCE;
    }

    public Context getContext() {
        return context;
    }

    private AppManagement(MainActivity mainActivity) {
        this.context = mainActivity;
    }

    public static void initialize(MainActivity mainActivity) {
        if (INSTANCE == null)
            INSTANCE = new AppManagement(mainActivity);
    }
}
