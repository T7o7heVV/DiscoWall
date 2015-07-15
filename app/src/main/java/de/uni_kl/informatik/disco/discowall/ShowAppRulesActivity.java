package de.uni_kl.informatik.disco.discowall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppRulesAdapter;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;


public class ShowAppRulesActivity extends AppCompatActivity {
    private final String LOG_TAG = ShowAppRulesActivity.class.getSimpleName();

    public FirewallService firewallService;
    public Firewall firewall;

    private int groupUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_app_rules);

        // Either this activity has been resumed, or just recently started. Either way, fetch args:
        Bundle args;
        if (savedInstanceState != null)
            args = savedInstanceState;
        else
            args = getIntent().getExtras();

        groupUid = args.getInt("app.uid");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(getIntent().getExtras()); // preserve arguments
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_app_rules, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

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

    private void onFirewallServiceBound() {
        Log.d(LOG_TAG, "Firewall-Service connected. Loading rules...");

        AppUidGroup appUidGroup = firewall.subsystem.watchedApps.getInstalledAppGroupByUid(groupUid);

        // Activity Title:
        setTitle("Rules: " + appUidGroup.getName());

        // App Information
        ((TextView) findViewById(R.id.activity_show_app_rules_app_name)).setText(appUidGroup.getName());
        ((TextView) findViewById(R.id.activity_show_app_rules_app_package)).setText(appUidGroup.getPackageName());
        ((ImageView) findViewById(R.id.activity_show_app_rules_app_icon)).setImageDrawable(appUidGroup.getIcon());

        firewall.DEBUG_TEST(appUidGroup); // DEBUG!

        showAppRulesInGui(appUidGroup);
    }

//    /**
//     * Since the extremely incomplete listView-framework uses internal buffers for buffering events and constructing rows,
//     * the checkbox-checked-listeners are being fired as the gui is being constructed.
//     * ==> I have to sort out those cases.
//     * But since it is not possible to determine whether the listview is "done" with constructing the list,
//     * I simply enable the checkbox-listeners when the user tries to click it the first time.
//     */
//    private boolean checkBoxesEnabled = false;

    private void showAppRulesInGui(final AppUidGroup appUidGroup) {
        ListView rulesListView = (ListView) findViewById(R.id.activity_show_app_rules_listView_rules);
        final AppRulesAdapter appRulesAdapter = new AppRulesAdapter(this, firewall.subsystem.rulesManager.getRules(appUidGroup));
        rulesListView.setAdapter(appRulesAdapter);

        // Listeners for opening the rule-edit-dialog
        appRulesAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (view instanceof CheckBox) {
//                    if (!checkBoxesEnabled) {
//                        Log.v(LOG_TAG, "checkbox clicked --> checkbox checked-events are enabled from now on. This is a android-listview bug-workaround.");
//                        checkBoxesEnabled = true;
//
//                        // performing the toggle-click, which would be lost otherwise (as it would have been executed before this call):
//                        appRulesAdapter.getOnCheckedChangeListener().onCheckedChanged(appRulesAdapter, appRulesAdapter.getItem(position), position, ((CheckBox)view), ((CheckBox)view).isChecked());
//                    }
//
//                    return;
//                }

                FirewallRules.IFirewallRule appRule = appRulesAdapter.getItem(position);
                EditConnectionRuleDialog.show(ShowAppRulesActivity.this, "", appUidGroup, appRule);
            }
        });

//        // Listeners for changing protocol-/device-filter
//        appRulesAdapter.setOnCheckedChangeListener(new AppRulesAdapter.CheckedChangedListener() {
//            @Override
//            public void onCheckedChanged(AppRulesAdapter adapter, FirewallRules.IFirewallRule rule, int position, CheckBox checkBox, boolean isChecked) {
//                // IMPORTANT NOTE:
//                // No idea how to stop android form calling this method before the GUI is finished building (the list view is NOT filled linear, but randomly!).
//                // Even though the listeners is set AFTER the assignment of the adapter, a few calls still end in this listener. -.-
//
//                if (!checkBoxesEnabled)
//                    return;
//
//                Log.v(LOG_TAG, "original rule: " + rule);
//
//                try {
//                    switch (checkBox.getId()) {
//                        case AppRulesAdapter.widgetId_checkbox_rule_interface_umts:
//                            rule.setDeviceFilter(FirewallRules.DeviceFilter.construct(rule.getDeviceFilter().allowsWifi(), isChecked));
//                            break;
//                        case AppRulesAdapter.widgetId_checkbox_rule_interface_wifi:
//                            rule.setDeviceFilter(FirewallRules.DeviceFilter.construct(isChecked, rule.getDeviceFilter().allowsUmts()));
//                            break;
//                        case AppRulesAdapter.widgetId_checkbox_rule_protocol_tcp:
//                            rule.setProtocolFilter(FirewallRules.ProtocolFilter.construct(isChecked, rule.getProtocolFilter().isUdp()));
//                            break;
//                        case AppRulesAdapter.widgetId_checkbox_rule_protocol_udp:
//                            rule.setProtocolFilter(FirewallRules.ProtocolFilter.construct(rule.getProtocolFilter().isTcp(), isChecked));
//                            break;
//                    }
//
//                    Toast.makeText(ShowAppRulesActivity.this, "rule updated", Toast.LENGTH_SHORT).show();
//                } catch (FirewallRules.DeviceFilter.DeviceFilterException e) {
//                    // happens if the resulting filter is NONE ==> user unchecked WiFi+UMTS
//                    // ==> reset last checkbox.
//                    checkBox.setChecked(true);
//
//                    Toast.makeText(ShowAppRulesActivity.this, "at least one interface required.", Toast.LENGTH_SHORT).show();
//                } catch (FirewallRules.ProtocolFilter.ProtocolFilterException e) {
//                    // happens if the resulting filter is NONE ==> user unchecked TCP+UDP
//                    // ==> reset last checkbox.
//                    checkBox.setChecked(true);
//
//                    Toast.makeText(ShowAppRulesActivity.this, "at least one protocol required", Toast.LENGTH_SHORT).show();
//                }
//
//                Log.v(LOG_TAG, "updated rule: " + rule);
//            }
//        });

        // Hide the "list empty" text, if the list is not empty
        if (appRulesAdapter.getRules().size() > 0)
            findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);
    }

    public static void showAppRules(Context context, AppUidGroup appUidGroup) {
        Bundle args = new Bundle();

        // Dialog-Infos:
        args.putInt("app.uid", appUidGroup.getUid());

        Intent showAppRulesIntent = new Intent(context, ShowAppRulesActivity.class);
        showAppRulesIntent.putExtras(args);
        context.startActivity(showAppRulesIntent);
    }
}
