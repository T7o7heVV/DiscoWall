package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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

import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.subsystems.SubsystemRulesManager;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppRulesAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;
import de.uni_kl.informatik.disco.discowall.utils.IntentDataSerializer;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;


public class ShowAppRulesActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShowAppRulesActivity.class.getSimpleName();

    private static final String INTENT_ACTION = "action";

    private static final String INTENT_ACTION_SHOW = "show";

    private static final String INTENT_DATA__APP_UID = "app.uid";
    private static final String INTENT_DATA__TRANSPORT_LAYER_PROTOCOL = "connection.protocol";

    private static final String INTENT_ACTION__PENDING_CONNECTION__DECIDE_BY_USER = "action.pendingConnection.user-decide";
    private static final String INTENT_ACTION__PENDING_CONNECTION__ACCEPT = "action.pendingConnection.accept";
    private static final String INTENT_ACTION__PENDING_CONNECTION__BLOCK  = "action.pendingConnection.block";


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
                actionCreateRuleAbove(clickedRule);
                return true;
            case R.id.action_rules_delete_rule:
                actionDeleteRule(clickedRule);
                return true;
            case R.id.action_rules_edit_rule:
                actionEditRule(clickedRule);
                return true;
            case R.id.action_rules_move_rule_up:
                actionMoveRule(clickedRule, true);
                return true;
            case R.id.action_rules_move_rule_down:
                actionMoveRule(clickedRule, false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    private void actionMoveRule(FirewallRules.IFirewallRule rule, boolean up) {
        boolean moveResult;
        if (up)
            moveResult = firewall.subsystem.rulesManager.moveRuleUp(rule);
        else
            moveResult = firewall.subsystem.rulesManager.moveRuleDown(rule);

        // refresh gui only if changed
        if (moveResult)
            afterRulesChanged();
    }

    private void actionDeleteRule(final FirewallRules.IFirewallRule ruleToDelete) {
        new AlertDialog.Builder(ShowAppRulesActivity.this)
                .setTitle(R.string.question_delete_rule)
                .setIcon(AppRulesAdapter.getRuleIcon(ruleToDelete, ShowAppRulesActivity.this))
                .setMessage("Delete selected rule?" + "\n\n" + ruleToDelete.getLocalFilter() + " -> " + ruleToDelete.getRemoteFilter())
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        firewall.subsystem.rulesManager.deleteRule(ruleToDelete);

                        Log.d(LOG_TAG, "User deleted rule '"+ruleToDelete+"' from app-group '" + appUidGroup + "'.");
                        Toast.makeText(ShowAppRulesActivity.this, "rule deleted", Toast.LENGTH_SHORT).show();

                        // remove rule from iptables:
                        onRuleDeleted(ruleToDelete);

                        // reload activity, so that the empty list is shown:
                        afterRulesChanged();
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

        handleIntentCommand();
    }

    private void showPackageDecidedToast(Connections.IConnection connection, boolean accepted) {
        String connectionInfo = Connections.Connection.toUserString(connection);
        Toast.makeText(getApplicationContext(), (accepted ? "accepted" : "blocked:") + "\n" + connectionInfo, Toast.LENGTH_SHORT).show();
    }

    private void handleIntentCommand() {
        final Intent intent = getIntent();
        final Bundle args = getIntent().getExtras();
        final String action = args.getString("action");

        if (action.equals(INTENT_ACTION_SHOW))
            return;

        /* Functionality-Notes:
         *   - Finishing Activities after action done:
         *     It would be nice to close all activities once everything is done. But closing activities (i.e. removing them from the stack) causes the listeners on calling activities to fail.
         *     Example: Some toasts (like "accepted"/"blocked") could not be shown when doing this.
         */

        // Pending Connection Notification Clicks:
        if (INTENT_ACTION__PENDING_CONNECTION__DECIDE_BY_USER.equals(action)) {
            // Extract Intent Data:
            Connections.IConnection connection = IntentDataSerializer.readConnection(intent, "connection");
            int uid = args.getInt(INTENT_DATA__APP_UID);
            final Packages.TransportLayerProtocol protocol = Packages.TransportLayerProtocol.valueOf(intent.getStringExtra(INTENT_DATA__TRANSPORT_LAYER_PROTOCOL));

            // decide what to do with connection
            DecideConnectionDialog.DecideConnectionDialogListener dialogResultListener = new DecideConnectionDialog.DecideConnectionDialogListener() {
                @Override
                public void onConnectionDecided(AppUidGroup appUidGroup, Connections.IConnection connection, final DecideConnectionDialog.AppConnectionDecision decision) {
                    // The package has to be accepted/blocked - independent of the creation of a rule
                    if (decision.allowConnection)
                        firewall.subsystem.pendingActionsManager.acceptPendingPackage();
                    else
                        firewall.subsystem.pendingActionsManager.blockPendingPackage();

                    // show toast about decision:
                    showPackageDecidedToast(connection, decision.allowConnection);

                    // create permanent rule:
                    if (decision.createRule) {
                        FirewallRules.RulePolicy policy = decision.allowConnection ? FirewallRules.RulePolicy.ALLOW : FirewallRules.RulePolicy.BLOCK;

                        actionCreateRuleAbove(null, FirewallRules.RuleKind.Policy, connection, protocol.toFilter(), policy,
                                new EditRuleDialog.DialogListener() {
                                    @Override
                                    public void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                                        // close app-rules view
                                        ShowAppRulesActivity.this.finish();
//                                        MainActivity.closeAllActivities(ShowAppRulesActivity.this); // close all activities; has to be called AFTER the dialog terminates - otherwise the dialog cannot even start correctly.
                                    }

                                    @Override
                                    public void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                                        // close app-rules view
                                        ShowAppRulesActivity.this.finish();
//                                        MainActivity.closeAllActivities(ShowAppRulesActivity.this); // close all activities
                                    }

                                    @Override
                                    public void onBeforeRuleChangesSaved(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                                        // the rule has just been created - nothing to do here.
                                    }
                                }
                        );
                    } else {
                        // close app-rules view
                        Log.v(LOG_TAG, "closing rules activity...");
//                        MainActivity.closeAllActivities(ShowAppRulesActivity.this); // close all activities
                        ShowAppRulesActivity.this.finish();
                    }
                }

                @Override
                public void onDialogCanceled(AppUidGroup appUidGroup, Connections.IConnection connection) {
                    firewall.subsystem.pendingActionsManager.OnDecisionDialogDismissed(appUidGroup, connection);
//                    MainActivity.closeAllActivities(ShowAppRulesActivity.this); // close all activities
                    ShowAppRulesActivity.this.finish(); // close app-rules view
                }
            };

            // Inform the pendingActionsManager which decision is currently being made by the user
            firewall.subsystem.pendingActionsManager.OnDecisionDialogOpened(appUidGroup, connection);

            // Show decision-dialog
            DecideConnectionDialog.show(this, dialogResultListener, appUidGroup, connection, protocol);
        } else if (INTENT_ACTION__PENDING_CONNECTION__ACCEPT.equals(action) || INTENT_ACTION__PENDING_CONNECTION__BLOCK.equals(action)) { // ACCEPT/BLOCK actions within Connection-Notification
            final boolean accept = INTENT_ACTION__PENDING_CONNECTION__ACCEPT.equals(action);

            if (accept) {
                Log.d(LOG_TAG, "Action.PendingConnection: accept package");
                firewall.subsystem.pendingActionsManager.acceptPendingPackage();
            } else {
                Log.d(LOG_TAG, "Action.PendingConnection: block package");
                firewall.subsystem.pendingActionsManager.blockPendingPackage();
            }

            // show toast about decision:
            Connections.IConnection connection = IntentDataSerializer.readConnection(intent, "connection");
            showPackageDecidedToast(connection, accept);

            finish();
//            MainActivity.closeAllActivities(this); // close all activities
        }

        // remove the action from the intent, so that the same action will not be performed again after returning from a sub-dialog:
        intent.putExtra(INTENT_ACTION, "");
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
                                firewall.subsystem.rulesManager.deleteUserRules(appUidGroup);

                                Log.d(LOG_TAG, "User deleted all rules for app-group: " + appUidGroup);
                                Toast.makeText(ShowAppRulesActivity.this, "all rules deleted", Toast.LENGTH_SHORT).show();

                                // reload activity, so that the empty list is shown:
                                afterRulesChanged();
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
        actionCreateRuleAbove(null);
    }

    private void actionCreateRuleAbove(final FirewallRules.IFirewallRule existingRuleBelowNewOne) {
        new AlertDialog.Builder(ShowAppRulesActivity.this)
                .setTitle("Create Rule")
                .setIcon(appUidGroup.getIcon())
                .setMessage("Select Rule-Kind")
                .setCancelable(true)
                .setPositiveButton("Redirection", new DialogInterface.OnClickListener() { // positive button is on the right ==> redirection is right button
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionCreateRuleAbove(existingRuleBelowNewOne, FirewallRules.RuleKind.Redirect);
                    }
                })
                .setNegativeButton("Policy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionCreateRuleAbove(existingRuleBelowNewOne, FirewallRules.RuleKind.Policy);
                    }
                })
                .create().show();
    }

    private void actionCreateRuleAbove(final FirewallRules.IFirewallRule existingRuleBelowNewOne, FirewallRules.RuleKind ruleKind) {
        actionCreateRuleAbove(existingRuleBelowNewOne, ruleKind, null, null, null, null);
    }

    private void actionCreateRuleAbove(final FirewallRules.IFirewallRule existingRuleBelowNewOne, FirewallRules.RuleKind ruleKind, Connections.IConnection connectionFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy policy, final EditRuleDialog.DialogListener dialogResultListener) {
        FirewallRules.IFirewallRule rule;

        switch(ruleKind) {
            case Policy:
                rule = new FirewallRules.FirewallTransportRule(appUidGroup.getUid(), policy != null ? policy : FirewallRules.RulePolicy.ALLOW);
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

        if (connectionFilter != null) {
            rule.setLocalFilter(connectionFilter.getSource());
            rule.setRemoteFilter(connectionFilter.getDestination());
        }

        if (protocolFilter != null)
            rule.setProtocolFilter(protocolFilter);

        EditRuleDialog.show(ShowAppRulesActivity.this, new EditRuleDialog.DialogListener() {
                    @Override
                    public void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        try {
                            Log.v(LOG_TAG, EditRuleDialog.class.getSimpleName() + " closed with OK --> Adding rule for AppGroup" + appUidGroup + ": " + rule);

                            if (existingRuleBelowNewOne != null)
                                firewall.subsystem.rulesManager.addRule(rule, existingRuleBelowNewOne);
                            else
                                firewall.subsystem.rulesManager.addRule(rule);

                            afterRulesChanged();
                        } catch (FirewallRuleExceptions.DuplicateRuleException | FirewallRuleExceptions.RuleNotFoundException e) {
                            // RuleNotFoundException: can also not happen, as both rules are owned by the same user - as they are in this list
                            // DuplicateRuleException: will never be fired, as I will never try to add this rule agAIn
                            Log.e(LOG_TAG, e.getMessage(), e);
                        }

                        if (dialogResultListener != null)
                            dialogResultListener.onAcceptChanges(rule, appUidGroup);
                    }

                    @Override
                    public void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        Log.v(LOG_TAG, EditRuleDialog.class.getSimpleName() + " closed with CANCEL.");

                        if (dialogResultListener != null)
                            dialogResultListener.onDiscardChanges(rule, appUidGroup);
                    }

                    @Override
                    public void onBeforeRuleChangesSaved(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        // nothing to do as the rule has just been created
                    }
                }, appUidGroup, rule
        );
    }

    private void afterRulesChanged() {
        new GuiUtils.AsyncTaskSpinnerProgress<Object, Object, Object>(this, "Iptables Update", "writing rules to iptables...") {
            @Override
            protected Object doInBackground(Object... params) {
                // Write rules to storage:
                firewall.subsystem.rulesManager.saveRulesToAppStorage(appUidGroup);

                // Write rules to iptables:
                try {
                    writeRulesToIptables();
                } catch(Exception e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                // Restart activity for refreshing data. Reloading listViews almost never works anyway.
                GuiUtils.restartActivity(ShowAppRulesActivity.this);
            }
        }.execute();
    }

    private void writeRulesToIptables() throws Exception {
        boolean writeInteractiveRulesToIptables = DiscoWallSettings.getInstance().isWriteInteractiveRulesToIptables(this);

        for(FirewallRules.IFirewallRule rule : firewall.subsystem.rulesManager.getRules(appUidGroup)) {
            if (rule instanceof FirewallRules.IFirewallPolicyRule) {
                if (!writeInteractiveRulesToIptables)
                    continue;
            }

            // removing and adding rule to create the rules-order as specified within this rules-list
            rule.removeFromIptables();
            rule.addToIptables();
        }
    }

    private void onRuleDeleted(FirewallRules.IFirewallRule rule) {
        deleteRuleFromIptables(rule);
    }

    private void deleteRuleFromIptables(FirewallRules.IFirewallRule rule) {
        // handle iptable-entries when rules are being deleted:
        try {
            rule.removeFromIptables();
        } catch (ShellExecuteExceptions.ShellExecuteException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            ErrorDialog.showError(ShowAppRulesActivity.this, "Unable to remove rule from iptables: " + e.getMessage(), e);
        }
    }

    private void actionEditRule(FirewallRules.IFirewallRule rule) {
        EditRuleDialog.show(
                ShowAppRulesActivity.this, new EditRuleDialog.DialogListener() {
                    @Override
                    public void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        Log.d(LOG_TAG, "accepted rule-changes for rule: " + rule);
                        afterRulesChanged();
                    }

                    @Override
                    public void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        // do nothing per default
                        Log.d(LOG_TAG, "discarded rule-changes for rule: " + rule);
                    }

                    @Override
                    public void onBeforeRuleChangesSaved(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup) {
                        // delete rule from iptables before it is being changed, as it cannot be identified afterwards
                        deleteRuleFromIptables(rule);
                    }

                }, appUidGroup, rule
        );
    }

    private void showAppRulesInGui(final AppUidGroup appUidGroup) {
        final ListView rulesListView = (ListView) findViewById(R.id.activity_show_app_rules_listView_rules);

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

    public static Intent createActionIntent_showAppRules(Context context, AppUidGroup appUidGroup) {
        Bundle args = new Bundle();

        args.putInt(INTENT_DATA__APP_UID, appUidGroup.getUid());
        args.putString(INTENT_ACTION, INTENT_ACTION_SHOW);

        Intent intent = new Intent(context, ShowAppRulesActivity.class);
        intent.putExtras(args);

        return intent;
    }

    public static void showAppRules(Context context, AppUidGroup appUidGroup) {
        context.startActivity(createActionIntent_showAppRules(context, appUidGroup));
    }

    public static Intent createActionIntent_decideConnection(Context context, Connections.Connection connection) {
        Intent intent = new Intent(context, ShowAppRulesActivity.class);

        intent.putExtra(INTENT_DATA__APP_UID, connection.getUserId());
        intent.putExtra(INTENT_DATA__TRANSPORT_LAYER_PROTOCOL, connection.getTransportLayerProtocol().toString());
        intent.putExtra(INTENT_ACTION, INTENT_ACTION__PENDING_CONNECTION__DECIDE_BY_USER);

        IntentDataSerializer.writeConnection(connection, intent, "connection");

        return intent;
    }

    public static Intent createActionIntent_handleConnection(Context context, Connections.Connection connection, boolean accept) {
        Intent intent = new Intent(context, ShowAppRulesActivity.class);
        intent.putExtra(INTENT_ACTION, accept ? INTENT_ACTION__PENDING_CONNECTION__ACCEPT : INTENT_ACTION__PENDING_CONNECTION__BLOCK);
        intent.putExtra(INTENT_DATA__TRANSPORT_LAYER_PROTOCOL, connection.getTransportLayerProtocol().toString());

        IntentDataSerializer.writeConnection(connection, intent, "connection");

        return intent;
    }

}
