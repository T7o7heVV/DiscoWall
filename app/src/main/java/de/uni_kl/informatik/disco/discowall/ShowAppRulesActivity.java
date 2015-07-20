package de.uni_kl.informatik.disco.discowall;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppRulesAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;


public class ShowAppRulesActivity extends AppCompatActivity {
    private final String LOG_TAG = ShowAppRulesActivity.class.getSimpleName();

    public FirewallService firewallService;
    public Firewall firewall;

    private AppRulesAdapter appRulesAdapter;
    private int groupUid;
    private AppUidGroup appUidGroup;

    private Button buttonAddRule;
    private Button buttonClearRules;

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

        // for creating the Floating-Menu on the Rules-List ==> Long-Press will now show the menu
        registerForContextMenu(findViewById(R.id.activity_show_app_rules_listView_rules)); // see http://developer.android.com/guide/topics/ui/menus.html#FloatingContextMenu

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
//        getMenuInflater().inflate(R.menu.menu_show_app_rules, menu);
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Creating floating-menu for Watched-Apps-List:
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_show_app_rules, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // = Info: This method is automatically called by android-os, as the user long-clicks a view, which has been registered for a menu using 'registerForContextMenu()'

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.v(LOG_TAG, "Context/Floating-Menu opened on listViewItem: " + info.position);

        FirewallRules.IFirewallRule clickedRule = appRulesAdapter.getItem(info.position);

        switch(item.getItemId()) {
            case R.id.action_rules_create_rule:
                actionCreateRule();
                return true;
            case R.id.action_rules_delete_rule:
                actionDeleteRule(clickedRule);
                return true;
            case R.id.action_rules_edit_rule:
                actionEditRule(clickedRule);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void actionDeleteRule(final FirewallRules.IFirewallRule ruleToDelete) {
        new AlertDialog.Builder(ShowAppRulesActivity.this)
                .setTitle("Delete Rule")
                .setIcon(AppRulesAdapter.getRuleIcon(ruleToDelete, ShowAppRulesActivity.this))
                .setMessage("Delete selected rule?" + "\n\n" + ruleToDelete.getLocalFilter() + " -> " + ruleToDelete.getRemoteFilter())
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        firewall.subsystem.rulesManager.deleteRule(ruleToDelete);

                        Log.d(LOG_TAG, "User deleted rule '"+ruleToDelete+"' from app-group '" + appUidGroup + "'.");
                        Toast.makeText(ShowAppRulesActivity.this, "rule deleted", Toast.LENGTH_SHORT).show();

                        // reload activity, so that the empty list is shown:
                        refreshActivityAfterRulesChanged();
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

        appUidGroup = firewall.subsystem.watchedApps.getInstalledAppGroupByUid(groupUid);

        // Activity Title:
        setTitle("Rules: " + appUidGroup.getName());

        // App Information
        ((TextView) findViewById(R.id.activity_show_app_rules_app_name)).setText(appUidGroup.getName());
        ((TextView) findViewById(R.id.activity_show_app_rules_app_package)).setText(appUidGroup.getPackageName());
        ((ImageView) findViewById(R.id.activity_show_app_rules_app_icon)).setImageDrawable(appUidGroup.getIcon());

        // Setup Buttons:
        buttonAddRule = (Button) findViewById(R.id.activity_show_app_rules_button_createRule);
        buttonClearRules = (Button) findViewById(R.id.activity_show_app_rules_button_clearRules);
        setupButtons();

        showAppRulesInGui(appUidGroup);
    }

    private void setupButtons() {
        buttonAddRule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionCreateRule();
            }
        });

        buttonClearRules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ShowAppRulesActivity.this)
                        .setTitle("Clear All Rules")
                        .setIcon(appUidGroup.getIcon())
                        .setMessage("Delete all rules for app '" + appUidGroup.getName() + "'?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                firewall.subsystem.rulesManager.deleteAllRules(appUidGroup);

                                Log.d(LOG_TAG, "User deleted all rules for app-group: " + appUidGroup);
                                Toast.makeText(ShowAppRulesActivity.this, "all rules deleted", Toast.LENGTH_SHORT).show();

                                // reload activity, so that the empty list is shown:
                                refreshActivityAfterRulesChanged();
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
        });
    }

    private void actionCreateRule() {
        new AlertDialog.Builder(ShowAppRulesActivity.this)
                .setTitle("Create Rule")
                .setIcon(appUidGroup.getIcon())
                .setMessage("Select Rule-Kind")
                .setCancelable(true)
                .setPositiveButton("Redirection", new DialogInterface.OnClickListener() { // positive button is on the right ==> redirection is right button
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionCreateRule(FirewallRules.RuleKind.Redirect);
                    }
                })
                .setNegativeButton("Policy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionCreateRule(FirewallRules.RuleKind.Policy);
                    }
                })
                .create().show();
    }

    private void actionCreateRule(FirewallRules.RuleKind ruleKind) {
        FirewallRules.IFirewallRule rule;

        switch(ruleKind) {
            case Policy:
                rule = new FirewallRules.FirewallTransportRule(appUidGroup.getUid(), FirewallRules.RulePolicy.ALLOW);
                break;
            case Redirect:
                try {
                    rule = new FirewallRules.FirewallTransportRedirectRule(appUidGroup.getUid(), new Packages.IpPortPair("localhost", 80));
                } catch (FirewallRuleExceptions.InvalidRuleDefinitionException e) {
                    // this rule-definition is fine - this cannot occur

                    Log.e(LOG_TAG, e.getMessage(), e);
                    ErrorDialog.showError(ShowAppRulesActivity.this, "Unable to create Redirection-Rule: " + e.getMessage(), e);
                    return;
                }
                break;
            default:
                Log.e(LOG_TAG, "Missing implementation for rule-kind: " + ruleKind);
                return;
        }

        EditRuleDialog.show(ShowAppRulesActivity.this, new EditRuleDialog.DialogListener() {
            @Override
            public void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                try {
                    Log.v(LOG_TAG, "Adding rule for AppGroup" + appUidGroup + ": " + rule);
                    firewall.subsystem.rulesManager.addRule(rule);
                    refreshActivityAfterRulesChanged();
                } catch (FirewallRuleExceptions.DuplicateRuleException e) {
                    // This exception cannot be fired, as I will never try to add this rule again
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
            }

            @Override
            public void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
            }
        }, appUidGroup, rule);
    }

    private void refreshActivityAfterRulesChanged() {
        // Restart activity for refreshing data. Reloading listViews almost never works anyway.
        GuiUtils.restartActivity(ShowAppRulesActivity.this);
    }

    private void actionEditRule(FirewallRules.IFirewallRule rule) {
        EditRuleDialog.show(
                ShowAppRulesActivity.this, new EditRuleDialog.DialogListener() {
                    @Override
                    public void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        refreshActivityAfterRulesChanged();
                    }

                    @Override
                    public void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        // do nothing
                    }
                }, appUidGroup, rule
        );
    }

    private void showAppRulesInGui(final AppUidGroup appUidGroup) {
        ListView rulesListView = (ListView) findViewById(R.id.activity_show_app_rules_listView_rules);
        appRulesAdapter = new AppRulesAdapter(this, firewall.subsystem.rulesManager.getRules(appUidGroup));
        rulesListView.setAdapter(appRulesAdapter);

        // Listeners for opening the rule-edit-dialog
        appRulesAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FirewallRules.IFirewallRule appRule = appRulesAdapter.getItem(position);
                actionEditRule(appRule);
            }
        });

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
