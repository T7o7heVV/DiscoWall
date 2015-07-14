package de.uni_kl.informatik.disco.discowall.utils.apps;

import android.graphics.drawable.Drawable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AppGroup {
    private final LinkedList<App> apps;
    private final Drawable icon;
    private final String name;
    private final String packageName;

    public AppGroup(App... apps) {
        this(Arrays.asList(apps));
    }

    public AppGroup(List<App> apps) {
        this.apps = new LinkedList<>(apps);
        this.icon = this.apps.getFirst().getIcon();

        String name = "";
        String packageName = "";

        for(App app : apps) {
            if (!name.isEmpty())
                name += ",";
            if (!packageName.isEmpty())
                packageName += ",";

            name += app.getName();
            packageName += app.getPackageName();
        }

        this.name = name;
        this.packageName = packageName;
    }

    public String getName() {
        return name;
    }

    public LinkedList<App> getApps() {
        return new LinkedList<>(apps);
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }

}
