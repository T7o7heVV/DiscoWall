package de.uni_kl.informatik.disco.discowall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.gui.MainActivityGuiHandlers;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.AboutDialog;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;


public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private final MainActivityGuiHandlers guiHandlers = new MainActivityGuiHandlers(this);
    public final DiscoWallSettings discowallSettings = DiscoWallSettings.getInstance();

    public FirewallService firewallService;
    public Firewall firewall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.main_activity_title));
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
            case R.id.action_main_menu_exit:
            {
                if (firewall.isFirewallRunning())
                    Toast.makeText(this, "Firewall runs in background...", Toast.LENGTH_SHORT).show();

                finish();
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
                guiHandlers.actionSetAllAppsWatched(true);
                return true;
            }
            case R.id.action_monitored_apps__monitor_none:
            {
                guiHandlers.actionSetAllAppsWatched(false);
                return true;
            }
            case R.id.action_monitored_apps__invert_selected:
            {
                guiHandlers.actionInvertAllAppsWatched();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void actionShowIptableRules(boolean all) {
        String content = "";

        try {
            content = firewall.getIptableRules(all);
        } catch (FirewallExceptions.FirewallException e) {
            content = e.getMessage(); // showing error directly in text-view.
            e.printStackTrace();
        }

        TextViewActivity.showText(this, "Iptable Rules", content);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // assure that the firewall-service runs indefinitely - even if all bound activities unbind:
        FirewallService.startFirewallService(this, false);
//        startService(new Intent(this, FirewallService.class));

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

        if (discowallSettings.isFirewallEnabled(this))
            guiHandlers.actionSetFirewallEnabled(true, false);

        guiHandlers.showFirewallEnabledState();

        Switch firewallEnabledSwitch = (Switch) findViewById(R.id.switchFirewallEnabled);
        firewallEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                guiHandlers.onFirewallSwitchCheckedChanged(buttonView, isChecked);
            }
        });

        guiHandlers.setupFirewallPolicyRadioButtons();

        // enable/disable buttons and select according to current policy
        guiHandlers.updateFirewallPolicyRadioButtonsWithCurrentPolicy();

        // show apps and watched-status
        guiHandlers.setupWatchedAppsList();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.v(LOG_TAG, "conntected to service");

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
