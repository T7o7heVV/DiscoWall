package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.R;

public class DiscoWallSettings {
    private DiscoWallSettings() {}
    private static DiscoWallSettings INSTANCE;

    public static DiscoWallSettings getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DiscoWallSettings();

        return INSTANCE;
    }

    public int getFirewallPort(Context context) {
        return getSettingIntFromStr(context, R.string.preference_id__nfqueue_bridge_port, DiscoWallConstants.Firewall.defaultPort);
    }

    public boolean isAutostartFirewallService(Context context) {
        return getSettingBool(context, R.string.preference_id__service_autostart, true);
    }

    public boolean isFirewallEnabled(Context context) {
        return getSettingBool(context, R.string.preference_id__firewall_enabled, false);
    }

    public void setFirewallEnabled(Context context, boolean enabled) {
        setSettingBool(context, R.string.preference_id__firewall_enabled, enabled);
    }

    public Set<String> getWatchedAppsPackages(Context context) {
        return getSettingStringSet(context, R.string.preference_id__watched_apps_packages, new HashSet<String>());
    }

    public void setWatchedAppsPackages(Context context, Set<String> watchedAppPackages) {
        setSettingStringSet(context, R.string.preference_id__watched_apps_packages, watchedAppPackages);
    }

    private String getSetting(Context context, int preferenceKeyStringId, String defaultValue) {
//        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_file_main), Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(context.getString(preferenceKeyStringId), defaultValue);
    }

    /****************************************************************************************************/
    // Methods to return preferences as native types, if they have been created by code using "setBool()" etc.
    /****************************************************************************************************/

    private Set<String> getSettingStringSet(Context context, int preferenceKeyStringId, Set<String> defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getStringSet(context.getString(preferenceKeyStringId), defaultValue);
    }

    private int getSettingInt(Context context, int preferenceKeyStringId, int defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt(context.getString(preferenceKeyStringId), defaultValue);
    }

    private boolean getSettingBool(Context context, int preferenceKeyStringId, boolean defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(context.getString(preferenceKeyStringId), defaultValue);
    }

    /****************************************************************************************************/
    // Methods used to convert gui-preferences (from the settings activity) into native types,
    // as they are stored as strings by android.
    /****************************************************************************************************/

    private boolean getSettingBoolFromStr(Context context, int preferenceKeyStringId, boolean defaultValue) {
        return Boolean.parseBoolean(getSetting(context, preferenceKeyStringId, defaultValue + ""));
    }

    private int getSettingIntFromStr(Context context, int preferenceKeyStringId, int defaultValue) {
        return Integer.parseInt(getSetting(context, preferenceKeyStringId, defaultValue + ""));
    }

    /****************************************************************************************************/




    private void setSetting(Context context, int preferenceKeyStringId, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(preferenceKeyStringId), value);
        editor.commit();
    }

    private void setSettingBool(Context context, int preferenceKeyStringId, boolean value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(context.getString(preferenceKeyStringId), value);
        editor.commit();
    }

    private void setSettingInt(Context context, int preferenceKeyStringId, int value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(context.getString(preferenceKeyStringId), value);
        editor.commit();
    }

    private void setSettingStringSet(Context context, int preferenceKeyStringId, Set<String> value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(context.getString(preferenceKeyStringId), value);
        editor.commit();
    }
}
