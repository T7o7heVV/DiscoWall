package de.uni_kl.informatik.disco.discowall;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRulesExporter;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppRulesAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.gui.handlers.MainActivityGuiHandlerFirewallControl;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.AboutDialog;
import de.uni_kl.informatik.disco.discowall.gui.handlers.MainActivityGuiHandlerWatchedApps;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.FileUtils;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;
import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private final MainActivityGuiHandlerFirewallControl guiHandlerFirewallControl = new MainActivityGuiHandlerFirewallControl(this);
    private final MainActivityGuiHandlerWatchedApps guiHandlerWatchedApps = new MainActivityGuiHandlerWatchedApps(this);
    public final DiscoWallSettings discowallSettings = DiscoWallSettings.getInstance();

    public FirewallService firewallService;
    public Firewall firewall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.main_activity_title));

        // for creating the Floating-Menu on the Apps-List ==> Long-Press will now show the menu
        registerForContextMenu(findViewById(R.id.listViewFirewallMonitoredApps)); // see http://developer.android.com/guide/topics/ui/menus.html#FloatingContextMenu



        // NOTE: All initialization of GUI-Elements etc. is being done onFirewallServiceBound
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_main_menu_settings:
            {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            }
            case R.id.action_main_menu_about:
            {
                AboutDialog.show(this);
                return true;
            }
//            case R.id.action_main_menu_hide:
//            {
//                if (firewall.isFirewallRunning())
//                    Toast.makeText(this, R.string.message_application_runs_in_background, Toast.LENGTH_LONG).show();
//
//                finish();
//                return true;
//            }
            case R.id.action_main_menu_exit:
            {
                actionExitFirewall();
                return true;
            }
            case R.id.action_ifconfig:
            {
                actionShowIfconfigOutput();
                return true;
            }
            case R.id.action_iptables_show_all:
            {
                actionShowIptableRules(true);
                return true;
            }
            case R.id.action_iptables_show_discowall_only:
            {
                actionShowIptableRules(false);
                return true;
            }
            case R.id.action_monitored_apps__monitor_all:
            {
                guiHandlerWatchedApps.actionSetAllAppsWatched(true);
                return true;
            }
            case R.id.action_monitored_apps__monitor_none:
            {
                guiHandlerWatchedApps.actionSetAllAppsWatched(false);
                return true;
            }
            case R.id.action_monitored_apps__invert_selected:
            {
                guiHandlerWatchedApps.actionInvertAllAppsWatched();
                return true;
            }
            case R.id.action_rules_export:
            {
                actionExportRules();
                return true;
            }
            case R.id.action_rules_import:
            {
                actionImportRules();
                return true;
            }
            case R.id.action_rules_delete_all:
            {
                actionDeleteAllRules();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void actionDeleteAllRules() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.question_delete_all_rules)
                .setMessage(R.string.message_delete_all_rules_confirm)
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "Deleting all rules...");
                        firewall.subsystem.rulesManager.deleteAllRules();
                        Log.d(LOG_TAG, "rules deleted.");

                        // reload activity, so that the empty list is shown:
                        GuiUtils.restartActivity(MainActivity.this);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .create().show();
    }

    private void actionExportRules() {
        File discowallPublicDir = DiscoWallConstants.Directories.discowallPublicDirectory;

        String filePrefix = getString(R.string.app_name) + "_rules_export";
        File exportFile = FileUtils.createUniqueFilename(discowallPublicDir, filePrefix, ".xml");

        firewall.subsystem.rulesManager.saveAllRulesToFile(exportFile);

        Toast.makeText(this, exportFile.getAbsolutePath() + " written.", Toast.LENGTH_LONG).show();
    }

    private void actionImportRules() {
        // TODO!
    }

    private void actionShowIfconfigOutput() {
        String content = "";

        try {
            content = RootShellExecute.execute(true, "busybox ifconfig").processOutput;
        } catch (ShellExecuteExceptions.CallException e) {
            content = e.getMessage(); // showing error directly in text-view.
            e.printStackTrace();
        }

        TextViewActivity.showText(this, "ifconfig output", content);
    }

    private void actionExitFirewall() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_exit_application)
                .setIcon(R.drawable.firewall_launcher)
                .setMessage(firewall.isFirewallStopped() ? R.string.question_exit_application : R.string.question_disable_firewall_and_exit_application) // ask exit or disable+exit (if firewall running) otherwise
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        terminateApplication();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .create().show();
    }

    private void terminateApplication() {
        Runnable actionTerminateApplication = new Runnable() {
            @Override
            public void run() {
                // stop service:
                // The service is still bound to this activity, but will unbind on activity-stop
                // ==> since the service then is (1) not persistent anymore and (2) not bound, it will stop completely.
                firewallService.stopFirewallService(); // will also remove notification

                Toast.makeText(MainActivity.this, R.string.message_application_terminated, Toast.LENGTH_LONG).show();

                // Close MainActivity
                finish(); // will automatically unbind service
            }
        };

        if (!firewall.isFirewallStopped()) {
            // Disable Firewall and show Progress in Dialog, then do App-Exit
            guiHandlerFirewallControl.actionSetFirewallEnabled(false, actionTerminateApplication);
        } else {
            // directly exit application
            actionTerminateApplication.run();
        }
    }

    private void actionShowIptableRules(boolean all) {
        String content = "";

        try {
            content = firewall.subsystem.rulesManager.getIptableRules(all);
        } catch (FirewallExceptions.FirewallException e) {
            content = e.getMessage(); // showing error directly in text-view.
            e.printStackTrace();
        }

        TextViewActivity.showText(this, "Iptable Rules", content);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Creating floating-menu for Watched-Apps-List:
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_watched_apps_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // = Info: This method is automatically called by android-os, as the user long-clicks a view, which has been registered for a menu using 'registerForContextMenu()'

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.v(LOG_TAG, "Context/Floating-Menu opened on listViewItem: " + info.position);

        switch (item.getItemId()) {
            case R.id.action_menu_watched_apps_list_app_show_rules:
                Log.i(LOG_TAG, "Application: Show Rules");
                return true;
            case R.id.action_menu_watched_apps_list_app_start:
                guiHandlerWatchedApps.actionWatchedAppsContextMenuStartApp(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // assure that the firewall-service runs indefinitely - even if all bound activities unbind:
        FirewallService.startFirewallService(this, false);

        // Bind to LocalService
        Intent intent = new Intent(this, FirewallService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (firewallService != null) {
            unbindService(mConnection);
            firewallService = null;
        }
    }

    private void onFirewallServiceBound() {
        Log.d(LOG_TAG, "FirewallService bound");

        // Update list of installed apps when opening the firewall-gui, so that newly installed apps are shown and can be selected for filtering
        firewall.subsystem.watchedApps.updateInstalledAppsList();

        // Restoring firewall-state, if current state differs from supposed state
        // (happens when app is started and firewall was enabled last time)
        if (discowallSettings.isFirewallEnabled(this) != firewall.isFirewallRunning())
            guiHandlerFirewallControl.actionSetFirewallEnabled(true);

        guiHandlerFirewallControl.showFirewallEnabledState();

        Switch firewallEnabledSwitch = (Switch) findViewById(R.id.switchFirewallEnabled);
        firewallEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                guiHandlerFirewallControl.onFirewallSwitchCheckedChanged(buttonView, isChecked);
            }
        });

        guiHandlerFirewallControl.setupFirewallPolicyRadioButtons();

        // enable/disable buttons and select according to current policy
        guiHandlerFirewallControl.updateFirewallPolicyRadioButtonsWithCurrentPolicy();

        // show apps and watched-status
        guiHandlerWatchedApps.setupWatchedAppsList();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FirewallService.FirewallBinder binder = (FirewallService.FirewallBinder) service;
            firewallService = binder.getService();
            firewall = firewallService.getFirewall();

            onFirewallServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            firewallService = null;
        }
    };

}
