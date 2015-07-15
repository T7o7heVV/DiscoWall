package de.uni_kl.informatik.disco.discowall.gui.handlers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.ShowAppRulesActivity;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsManager;
import de.uni_kl.informatik.disco.discowall.gui.adapters.DiscoWallAppAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppAdapter;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class MainActivityGuiHandlerWatchedApps extends MainActivityGuiHandler {
    private DiscoWallAppAdapter watchedAppsListAdapter;

    public MainActivityGuiHandlerWatchedApps(MainActivity mainActivity) {
        super(mainActivity);
    }

    @Override
    protected void onAfterFirewallEnabledStateChanged(boolean firewallEnabled) {

    }

    public void setupWatchedAppsList() {
        // Fetch ListView for watchedApps and create adapter
        ListView appsListView = (ListView) mainActivity.findViewById(R.id.listViewFirewallMonitoredApps);
        final DiscoWallAppAdapter appsAdapter = new DiscoWallAppAdapter(mainActivity, mainActivity.firewall.subsystem.watchedApps.getInstalledAppGroups());
        appsListView.setAdapter(appsAdapter);

        // Storing reference, so that the list can be updated when enabling/disabling the firewall
        watchedAppsListAdapter = appsAdapter;

        // These click-handlers are being called, as widgets within the row are being clicked
        appsAdapter.setAdapterHandler(new AppAdapter.AdapterHandler() {
            // Adapter-Handler for manipulating list-view while it is being created etc.
            @Override
            public void onRowCreate(AppAdapter adapter, AppUidGroup appGroup, TextView appNameWidget, TextView appPackageNameWidget, TextView appRuleInfoTextView, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget) {
                /* This method is being called when...
                 * - the individual rows are being written when creating the list
                 * - the list is being scrolled and therefore updated
                 */

                // IMPORTANT: If I would buffer the "watched apps" at any point, scrolling the list will reset the value to the buffered state!
                appWatchedCheckboxWidget.setChecked(mainActivity.firewall.subsystem.watchedApps.isAppWatched(appGroup));

                int totalRulesCount = mainActivity.firewall.subsystem.rulesManager.getRules(appGroup).size();
                int policyRulesCount = mainActivity.firewall.subsystem.rulesManager.getPolicyRules(appGroup).size();
                int redirectionRulesCount = mainActivity.firewall.subsystem.rulesManager.getRedirectionRules(appGroup).size();

                // Creating String like: "3 rules:  2 policy, 1 redirection"
                String ruleInfo = totalRulesCount + (totalRulesCount != 1 ? " rules" : " rule");
                if (totalRulesCount > 0)
                    ruleInfo += ":  " + policyRulesCount + " policy, " + redirectionRulesCount + " redirection";

                // Layout for symbolic root-app:
                if (appGroup.getUid() == WatchedAppsManager.UID_ROOT) { // yeah, obviously "0", but a constant won't hurt anyone ;-)
                    // Making appname bold+italic
                    appNameWidget.setTypeface(null, Typeface.BOLD_ITALIC);
                }

                appRuleInfoTextView.setText(ruleInfo);
            }

            @Override
            public void onAppNameClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appNameWidgetview) {
                actionWatchedAppShowFirewallRules(appGroup);
            }

            @Override
            public void onAppPackageClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget) {
                actionWatchedAppShowFirewallRules(appGroup);
            }

            @Override
            public void onAppOptionalInfoClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appInfoWidget) {
                actionWatchedAppShowFirewallRules(appGroup);
            }

            @Override
            public void onAppIconClicked(AppAdapter appAdapter, AppUidGroup appGroup, ImageView appIconImageWidget) {
                actionWatchedAppShowFirewallRules(appGroup);
            }

            @Override
            public boolean onAppNameLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appNameWidgetview) {
                return false;
            }

            @Override
            public boolean onAppPackageLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget) {
                return false;
            }

            @Override
            public boolean onAppOptionalInfoLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, TextView appPackageNameWidget) {
                return false;
            }

            @Override
            public boolean onAppIconLongClicked(AppAdapter appAdapter, AppUidGroup appGroup, ImageView appIconImageWidget) {
                return false;
            }

            @Override
            public void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, AppUidGroup appGroup, CheckBox appWatchedCheckboxWidget, boolean isChecked) {
                actionSetAppWatched(appGroup, appWatchedCheckboxWidget.isChecked());
            }
        });
    }

    private void actionWatchedAppShowFirewallRules(AppUidGroup appGroup) {
        boolean createRuleIsDefaultChecked = DiscoWallSettings.getInstance().isHandleConnectionDialogDefaultCreateRule(mainActivity);

        mainActivity.firewall.DEBUG_TEST(appGroup); // DEBUG! Adding rules for testing

        ShowAppRulesActivity.showAppRules(mainActivity, appGroup);

//        DecideConnectionDialog.show(mainActivity, "example tag", appGroup, new Packages.IpPortPair("192.168.178.100", 1337), new Packages.IpPortPair("192.168.178.200", 4200), Connections.TransportLayerProtocol.TCP, createRuleIsDefaultChecked);
    }

    public void actionSetAllAppsWatched(boolean watched) {
        Log.i(LOG_TAG, "set " + (watched ? "all" : "no") + " apps to be watched by firewall...");

        HashMap<AppUidGroup, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(AppUidGroup appGroup : mainActivity.firewall.subsystem.watchedApps.getInstalledAppGroups())
            appsToWatchedStateMap.put(appGroup, watched);

        setAppsWatched(appsToWatchedStateMap, watched ? R.string.action_main_menu_monitor_all_apps : R.string.action_main_menu_monitor_no_apps);
    }

    public void actionInvertAllAppsWatched() {
        Log.i(LOG_TAG, "invert apps to be watched by firewall...");

        List<AppUidGroup> watchedApps = mainActivity.firewall.subsystem.watchedApps.getWatchedAppGroups();

        HashMap<AppUidGroup, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(AppUidGroup appGroup : mainActivity.firewall.subsystem.watchedApps.getInstalledAppGroups())
            appsToWatchedStateMap.put(appGroup, !mainActivity.firewall.subsystem.watchedApps.isAppWatched(appGroup));

        setAppsWatched(appsToWatchedStateMap, R.string.action_main_menu_monitor_invert_monitored);
    }

    private void setAppsWatched(final HashMap<AppUidGroup, Boolean> appsToWatchedStateMap, final int updateDialogTitleStringRessourceId) {
        // Since this operation might take up to a minute on slow devides ==> run with progress-bar etc..
        new AsyncTask<List<AppUidGroup>, Integer, Boolean>() {
            private String errorMessage;
            private ProgressDialog progressDialog;
            private LinkedList<AppUidGroup> apps = new LinkedList<AppUidGroup>(appsToWatchedStateMap.keySet()); // a list, so that I can iterate over it and use integers to show progress

            @Override
            protected Boolean doInBackground(List<AppUidGroup>... params) {
                int i=0;

                for(AppUidGroup appGroup : apps) {
                    publishProgress(i++);
                    boolean watchApp = appsToWatchedStateMap.get(appGroup);

                    try {
                        if (mainActivity.firewall.subsystem.watchedApps.isAppWatched(appGroup) != watchApp)
                            mainActivity.firewall.subsystem.watchedApps.setAppGroupWatched(appGroup, watchApp);
                    } catch(FirewallExceptions.FirewallException e) {
                        if (!errorMessage.isEmpty())
                            errorMessage += "\n";
                        errorMessage += "Error changing watched-state for app '" + appGroup.getPackageName() + "': " + e.getMessage();

                        Log.e(LOG_TAG, "Error changing watched-state for app '" + appGroup.getPackageName() + "': " + e.getMessage(), e);
                    }
                }

                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                progressDialog = new ProgressDialog(mainActivity);

                /* IMPORTANT:
                 * The design of the progress-dialog has to be defined BEFORE showing.
                 * ==> Any Message, Title and Icon have to be defined so that the dialog can be updated with new information after it's visible.
                 *     If the dialog is shown without an icon/title/message, it cannot show one later on.
                 */

                progressDialog.setTitle(updateDialogTitleStringRessourceId);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("");
                progressDialog.setIcon(apps.get(0).getIcon());
                progressDialog.setMax(appsToWatchedStateMap.size());
                progressDialog.setProgress(0);
                progressDialog.setCancelable(false);

                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                progressDialog.setProgress(progressDialog.getMax());
                progressDialog.dismiss();

                // So that the checkboxes for watched-state are updated
//                watchedAppsListAdapter.notifyDataSetChanged(); // sometimes not working

                // Restart Main-Activity to update gui:
                refreshMainActivity();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                final PackageManager packageManager = mainActivity.getPackageManager();
                final int appIndex = values[0];

                AppUidGroup appGroup = apps.get(appIndex);
                String appName = appGroup.getIcon() + "";

                progressDialog.setMessage(appName + "\n" + appGroup.getPackageName());
                progressDialog.setIcon(appGroup.getIcon());
                progressDialog.setProgress(appIndex);
            }
        }.execute();
    }

    private void actionSetAppWatched(final AppUidGroup appGroup, final boolean watched) {
        // Nothing to do, if the desired watched-state is already present. This happens when the GUI refreshes (as it does on scrolling).
        if (watched == mainActivity.firewall.subsystem.watchedApps.isAppWatched(appGroup)) {
//            Log.v(LOG_TAG, "App already " + (watched?"":"not ") + "watched. No change in sate required. This call happens when creating the list.");
            return;
        }

        // Show toast as this operation takes a few seconds
        if (watched)
            Toast.makeText(mainActivity, "monitor app traffic: " + appGroup.getName(), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mainActivity, "ignore app traffic: " + appGroup.getName(), Toast.LENGTH_SHORT).show();

        // Since this operation takes around a second, it is anoying that the GUI freezes for this amount of time ==> parallel task
        new AsyncTask<Boolean, Boolean, Boolean>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    mainActivity.firewall.subsystem.watchedApps.setAppGroupWatched(appGroup, watched);
                } catch (FirewallExceptions.FirewallException e) {
                    errorMessage = "Error changing watched-state for app '" + appGroup.getPackageName() + "': " + e.getMessage();
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);

                if (errorMessage != null)
                    ErrorDialog.showError(mainActivity, "App-Watch", errorMessage);
            }
        }.execute();
    }


    public void actionWatchedAppsContextMenuStartApp(int listViewItemPosition) {
        Log.v(LOG_TAG, "run application at list-position: " + listViewItemPosition + ")");
        final PackageManager packageManager = mainActivity.getPackageManager();

        // Cannot be null, index has to be within bounds. Otherwise something has SERIOUSLY gone wrong within the GUI framework.
        AppUidGroup appGroup = watchedAppsListAdapter.getItem(listViewItemPosition);
        Log.i(LOG_TAG, "Application: Run " + appGroup.getIcon() + " (" + appGroup.getPackageName() + ")");

        Toast.makeText(mainActivity, "starting app: " + appGroup.getIcon() + "\n" + appGroup.getPackageName(), Toast.LENGTH_SHORT).show();
        Intent startIntent = packageManager.getLaunchIntentForPackage(appGroup.getPackageName());
        mainActivity.startActivity(startIntent);
    }

    public void actionWatchedAppsContextMenuShowRules(int listViewItemPosition) {
        Log.v(LOG_TAG, "show application rules at list-position: " + listViewItemPosition + ")");

        // Cannot be null, index has to be within bounds. Otherwise something has SERIOUSLY gone wrong within the GUI framework.
        AppUidGroup appGroup = watchedAppsListAdapter.getItem(listViewItemPosition);
        Log.i(LOG_TAG, "Application: Show Rules: " + appGroup.getPackageName());

        actionWatchedAppShowFirewallRules(appGroup);
    }

}
