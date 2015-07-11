package de.uni_kl.informatik.disco.discowall.gui.handlers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.EditConnectionRuleDialog;
import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.ShowAppRulesActivity;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.DiscoWallAppAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.gui.AppAdapter;

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
        final DiscoWallAppAdapter appsAdapter = new DiscoWallAppAdapter(mainActivity);
        appsListView.setAdapter(appsAdapter);

        appsAdapter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        // Storing reference, so that the list can be updated when enabling/disabling the firewall
        watchedAppsListAdapter = appsAdapter;

        // Adapter-Handler for manipulating list-view while it is being created etc.
        appsAdapter.setAdapterHandler(new AppAdapter.AdapterHandler() {
            @Override
            public void onRowCreate(AppAdapter adapter, ApplicationInfo appInfo, TextView appNameWidget, TextView appPackageNameWidget, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget) {
                /* This method is being called when...
                 * - the individual rows are being written when creating the list
                 * - the list is being scrolled and therefore updated
                 */

                // IMPORTANT: If I would buffer the "watched apps" at any point, scrolling the list will reset the value to the buffered state!
                appWatchedCheckboxWidget.setChecked(mainActivity.firewall.isAppWatched(appInfo));
            }

            @Override
            public void onAppNameClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public void onAppPackageClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public void onAppIconClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public boolean onAppNameLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview) {
                return false;
            }

            @Override
            public boolean onAppPackageLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget) {
                return false;
            }

            @Override
            public boolean onAppIconLongClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget) {
                return false;
            }

            @Override
            public void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, ApplicationInfo appInfo, CheckBox appWatchedCheckboxWidget, boolean isChecked) {
                actionSetAppWatched(appInfo, appWatchedCheckboxWidget.isChecked());
            }
        });
    }

    private void actionWatchedAppShowFirewallRules(ApplicationInfo appInfo) {
//        EditConnectionRuleDialog.show(mainActivity, "example tag", appInfo, new Packages.IpPortPair("192.168.178.100", 1337), new Packages.IpPortPair("192.168.178.200", 4200), FirewallRules.RulePolicy.ACCEPT);

        ShowAppRulesActivity.showAppRules(mainActivity, appInfo);
    }

    public void actionSetAllAppsWatched(boolean watched) {
        Log.i(LOG_TAG, "set " + (watched ? "all" : "no") + " apps to be watched by firewall...");

        HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(ApplicationInfo appInfo : mainActivity.firewall.getWatchableApps())
            appsToWatchedStateMap.put(appInfo, watched);

        setAppsWatched(appsToWatchedStateMap, watched ? R.string.action_main_menu_monitor_all_apps : R.string.action_main_menu_monitor_no_apps);
    }

    public void actionInvertAllAppsWatched() {
        Log.i(LOG_TAG, "invert apps to be watched by firewall...");

        List<ApplicationInfo> watchedApps = mainActivity.firewall.getWatchedApps();

        HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(ApplicationInfo appInfo : mainActivity.firewall.getWatchableApps())
            appsToWatchedStateMap.put(appInfo, !mainActivity.firewall.isAppWatched(appInfo));

        setAppsWatched(appsToWatchedStateMap, R.string.action_main_menu_monitor_invert_monitored);
    }

    private void setAppsWatched(final HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap, final int updateDialogTitleStringRessourceId) {
        // Since this operation might take up to a minute on slow devides ==> run with progress-bar etc..
        new AsyncTask<List<ApplicationInfo>, Integer, Boolean>() {
            private String errorMessage;
            private ProgressDialog progressDialog;
            private LinkedList<ApplicationInfo> apps = new LinkedList<ApplicationInfo>(appsToWatchedStateMap.keySet()); // a list, so that I can iterate over it and use integers to show progress

            @Override
            protected Boolean doInBackground(List<ApplicationInfo>... params) {
                int i=0;

                for(ApplicationInfo appInfo : apps) {
                    publishProgress(i++);
                    boolean watchApp = appsToWatchedStateMap.get(appInfo);

                    try {
                        if (mainActivity.firewall.isAppWatched(appInfo) != watchApp)
                            mainActivity.firewall.setAppWatched(appInfo, watchApp);
                    } catch(FirewallExceptions.FirewallException e) {
                        if (!errorMessage.isEmpty())
                            errorMessage += "\n";
                        errorMessage += "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage();

                        Log.e(LOG_TAG, "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage(), e);
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
                progressDialog.setIcon(apps.get(0).loadIcon(mainActivity.getPackageManager()));
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

                ApplicationInfo appInfo = apps.get(appIndex);
                String appName = appInfo.loadLabel(packageManager) + "";

                progressDialog.setMessage(appName + "\n" + appInfo.packageName);
                progressDialog.setIcon(appInfo.loadIcon(packageManager));
                progressDialog.setProgress(appIndex);
            }
        }.execute();
    }

    private void actionSetAppWatched(final ApplicationInfo appInfo, final boolean watched) {
        // Nothing to do, if the desired watched-state is already present. This happens when the GUI refreshes (as it does on scrolling).
        if (watched == mainActivity.firewall.isAppWatched(appInfo)) {
//            Log.v(LOG_TAG, "App already " + (watched?"":"not ") + "watched. No change in sate required. This call happens when creating the list.");
            return;
        }

        // Show toast as this operation takes a few seconds
        if (watched)
            Toast.makeText(mainActivity, "monitor app traffic: " + appInfo.loadLabel(mainActivity.getPackageManager()), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mainActivity, "ignore app traffic: " + appInfo.loadLabel(mainActivity.getPackageManager()), Toast.LENGTH_SHORT).show();

        // Since this operation takes around a second, it is anoying that the GUI freezes for this amount of time ==> parallel task
        new AsyncTask<Boolean, Boolean, Boolean>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    mainActivity.firewall.setAppWatched(appInfo, watched);
                } catch (FirewallExceptions.FirewallException e) {
                    errorMessage = "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage();
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
        ApplicationInfo appInfo = watchedAppsListAdapter.getItem(listViewItemPosition);
        Log.i(LOG_TAG, "Application: Run " + appInfo.loadLabel(packageManager) + " (" + appInfo.packageName + ")");

        Toast.makeText(mainActivity, "starting app: " + appInfo.loadLabel(packageManager) + "\n" + appInfo.packageName, Toast.LENGTH_SHORT).show();
        Intent startIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName);
        mainActivity.startActivity(startIntent);
    }

    public void actionWatchedAppsContextMenuShowRules(int listViewItemPosition) {
        Log.v(LOG_TAG, "show application rules at list-position: " + listViewItemPosition + ")");

        // Cannot be null, index has to be within bounds. Otherwise something has SERIOUSLY gone wrong within the GUI framework.
        ApplicationInfo appInfo = watchedAppsListAdapter.getItem(listViewItemPosition);
        Log.i(LOG_TAG, "Application: Show Rules: " + appInfo.packageName);

        actionWatchedAppShowFirewallRules(appInfo);
    }

}
