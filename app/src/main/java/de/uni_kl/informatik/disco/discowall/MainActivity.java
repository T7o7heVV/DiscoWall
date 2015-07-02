package de.uni_kl.informatik.disco.discowall;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewallService.Firewall;
import de.uni_kl.informatik.disco.discowall.firewallService.FirewallService;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.AppUtils;
import de.uni_kl.informatik.disco.discowall.utils.NetworkInterfaceHelper;
import de.uni_kl.informatik.disco.discowall.utils.NetworkUtils;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;


public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private final GuiHandlers guiHandlers = new GuiHandlers(this);
    private final DiscoWallSettings discowallSettings = DiscoWallSettings.getInstance();

    private FirewallService firewallService;
    private Firewall firewall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fill Apps List

        ListView appsList = (ListView)findViewById(R.id.listViewFirewallMonitoredApps);

        LinkedList<String> installedAppNames = new LinkedList<>();
        for(Intent app : AppUtils.getInstalledAppsLaunchActivities(this))
            installedAppNames.add(app + "");

        appsList.setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                //new String[]{ "App One", "App Two", "App Three" }
                installedAppNames
        ));

        appsList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.i(LOG_TAG, "firewall port: " + discowallSettings.getFirewallPort(MainActivity.this)); // DEBUG!! Testint preferences
                    }
                }
        );
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    public void sendButtonClicked(View sendButtonView) {
//        Button sendButton = (Button)sendButtonView;
//        Log.v("Main", "button clicked");
//
//        EditText editText = (EditText) findViewById(R.id.editText);
//        Log.v("Main", "edit text: " + editText.getText());
//
//        try {
//            Firewall firewall = firewallService.getFirewall();
//            firewall.DEBUG_TEST();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void test() {
        try {
//            firewallService.stopFirewallService();
//            firewallService.getFirewall().enableFirewall(1337);

            firewallService.getFirewall().DEBUG_TEST();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // assure that the firewall-service runs indefinitely - even if all bound activities unbind:
        startService(new Intent(this, FirewallService.class));

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

    private class GuiHandlers {
        private final Context context;

        public GuiHandlers(Context context) {
            this.context = context;
        }

        public void onFirewallSwitchCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
            actionSetFirewallEnabled(isChecked, true);
        }

        public void actionSetFirewallEnabled(final boolean enabled, boolean showToastIfAlreadyEnabled) {
            try {
                if (enabled && firewall.isFirewallRunning()) {
                    if (showToastIfAlreadyEnabled)
                        Toast.makeText(MainActivity.this, "Firewall already enabled.", Toast.LENGTH_SHORT).show();

                    return;
                } else if (!enabled && !firewall.isFirewallRunning()) {
                    if (showToastIfAlreadyEnabled)
                        Toast.makeText(MainActivity.this, "Firewall already disabled.", Toast.LENGTH_SHORT).show();

                    return;
                }
            } catch(Exception e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Firewall ERROR")
                        .setMessage("Firewall could not fetch state due to error: " + e.getMessage())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                e.printStackTrace();

                return;
            }

            if (enabled)
                Toast.makeText(MainActivity.this, "Enabling Firewall...", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(MainActivity.this, "Disabling Firewall...", Toast.LENGTH_SHORT).show();


            class FirewallSetupTask extends AsyncTask<Boolean, Boolean, Boolean> {
                private AlertDialog.Builder errorAlert;

                @Override
                protected Boolean doInBackground(Boolean... params) {
                    if (enabled) {
                        try {
                            firewall.enableFirewall(discowallSettings.getFirewallPort(MainActivity.this));
                        } catch (Exception e) {
                            errorAlert = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Firewall ERROR")
                                    .setMessage("Firewall could not start due to error: " + e.getMessage())
                                    .setIcon(android.R.drawable.ic_dialog_alert);
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            firewall.disableFirewall();
                        } catch (Exception e) {
                            errorAlert = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Firewall ERROR")
                                    .setMessage("Firewall could not stop correctly due to error: " + e.getMessage())
                                    .setIcon(android.R.drawable.ic_dialog_alert);
                            e.printStackTrace();
                        }
                    }

                    return null;
                }

                protected void onPostExecute(Boolean result) {
                    if (errorAlert != null) {
                        errorAlert.show();
                        return;
                    }

                    if (enabled)
                        Toast.makeText(MainActivity.this, "Firewall Enabled.", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(MainActivity.this, "Firewall Disabled.", Toast.LENGTH_LONG).show();
                }
            }

            // Store enabled/disabled state in settings, so that it can be restored on app-start
            discowallSettings.setFirewallEnabled(MainActivity.this, enabled);

            new FirewallSetupTask().execute();
        }

        private void showFirewallEnabledState() {
            Switch firewallEnabledSwitch = (Switch) findViewById(R.id.switchFirewallEnabled);
            try {
                firewallEnabledSwitch.setChecked(firewall.isFirewallRunning());
            } catch (Exception e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Firewall ERROR")
                        .setMessage("Firewall determine firewall state: " + e.getMessage())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                e.printStackTrace();

                firewallEnabledSwitch.setChecked(false); // assuming firewall is NOT running
            }
        }

    }

}
