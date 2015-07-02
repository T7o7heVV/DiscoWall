package de.uni_kl.informatik.disco.discowall;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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

    public boolean isFirewallEnabled(Context context) {
        return getSettingBool(context, R.string.preference_id__firewall_enabled, false);
    }

    public void setFirewallEnabled(Context context, boolean enabled) {
        setSettingBool(context, R.string.preference_id__firewall_enabled, enabled);
    }


    private String getSetting(Context context, int preferenceKeyStringId, String defaultValue) {
//        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_file_main), Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(context.getString(preferenceKeyStringId), defaultValue);
    }

    /****************************************************************************************************/
    // Methods to return preferences as native types, if they have been created by code using "setBool()" etc.
    /****************************************************************************************************/

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
}
