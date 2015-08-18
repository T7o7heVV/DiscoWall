package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallPolicyManager;

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

    public int getNewConnectionDecisionTimeoutSeconds(Context context) {
        return getSettingIntFromStr(context, R.string.preference_id__firewall_connection_decision_timeoutMS, 30);
    }

    public boolean isAutostartFirewallService(Context context) {
        return getSettingBool(context, R.string.preference_id__service_autostart, true);
    }

    public boolean isFirewallEnabled(Context context) {
        return getSettingBool(context, R.string.preference_id__firewall_enabled, false);
    }

    public boolean isConnectionDecisionNotificationExpandStatusbar(Context context) {
        return getSettingBool(context, R.string.preference_id__firewall_connection_decision_expand_statusbar, true);
    }

    public void setFirewallEnabled(Context context, boolean enabled) {
        setSettingBool(context, R.string.preference_id__firewall_enabled, enabled);
    }

    public FirewallPolicyManager.FirewallPolicy getFirewallPolicy(Context context) {
        String policyAsString = getSetting(context, R.string.preference_id__firewall_policy, FirewallPolicyManager.FirewallPolicy.INTERACTIVE.toString());
        return FirewallPolicyManager.FirewallPolicy.valueOf(policyAsString);
    }

    public void setFirewallPolicy(Context context, FirewallPolicyManager.FirewallPolicy policy) {
        setSetting(context, R.string.preference_id__firewall_policy, policy.toString());
    }

    public Set<Integer> getWatchedAppsUIDs(Context context) {
        return getSettingIntegerSet(context, R.string.preference_id__watched_apps_uids, new HashSet<Integer>());
    }

    public void setWatchedAppsUIDs(Context context, Set<Integer> watchedAppPackages) {
        setSettingIntegerSet(context, R.string.preference_id__watched_apps_uids, watchedAppPackages);
    }

    public boolean isNewConnectionDefaultDecisionAccept(Context context) {
        return getSettingBool(context, R.string.preference_id__firewall_connection_decision_default_action, true);
    }

    public boolean isHandleConnectionDialogDefaultCreateRule(Context context) {
        return getSettingBool(context, R.string.preference_id__handle_connection_dialog__create_rule_default_checked, true);
    }

    public void setHandleConnectionDialogDefaultCreateRule(Context context, boolean value) {
        setSettingBool(context, R.string.preference_id__handle_connection_dialog__create_rule_default_checked, value);
    }

    /****************************************************************************************************/
    // Methods to return preferences as native types, if they have been created by code using "setBool()" etc.
    /****************************************************************************************************/

    private String getSetting(Context context, int preferenceKeyStringId, String defaultValue) {
//        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preferences_file_main), Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(context.getString(preferenceKeyStringId), defaultValue);
    }

    private HashSet<Integer> getSettingIntegerSet(Context context, int preferenceKeyStringId, Set<Integer> defaultValue) {
        Set<String> set = getSettingStringSet(context, preferenceKeyStringId, intSetToStringSet(defaultValue));
        HashSet<Integer> intSet = new HashSet<>();

        for(String str : set)
            intSet.add(Integer.parseInt(str));

        return intSet;
    }

    private Set<String> getSettingStringSet(Context context, int preferenceKeyStringId, Set<String> defaultValue) {
        /* Important Note: Known String-Set Android-Bug
         * It is important to always return A NEW COPY of a string-set, when loading the string-set-preference.
         * This is because android compares the object, which you wish to store, with the stored one - and if they match, android doesn't to anything.
         * http://stackoverflow.com/questions/14034803/misbehavior-when-trying-to-store-a-string-set-using-sharedpreferences/14034804#14034804
         *
         * Android-Developer note on SharedPreference.getStringSet():
         * "Note that you must not modify the set instance returned by this call. The consistency of the stored data is not guaranteed if you do, nor is your ability to modify the instance at all."
         */

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return new HashSet<>(sharedPref.getStringSet(context.getString(preferenceKeyStringId), defaultValue));
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
        editor.putStringSet(context.getString(preferenceKeyStringId), new HashSet<>(value));
        editor.commit();
    }

    private static HashSet<String> intSetToStringSet(Set<Integer> intSet) {
        HashSet<String> intSetAsStringSet = new HashSet<>();

        for(Integer intValue : intSet)
            intSetAsStringSet.add(intValue + "");

        return intSetAsStringSet;
    }

    private void setSettingIntegerSet(Context context, int preferenceKeyStringId, Set<Integer> value) {
        setSettingStringSet(context, preferenceKeyStringId, intSetToStringSet(value));
    }
}
