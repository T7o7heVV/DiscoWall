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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.gui.adapters.AppRulesAdapter;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;


public class ShowAppRulesActivity extends AppCompatActivity {
    private final String LOG_TAG = ShowAppRulesActivity.class.getSimpleName();

    public FirewallService firewallService;
    public Firewall firewall;

    private AppRulesAdapter appsAdapter;
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

        firewall.DEBUG_TEST(appUidGroup);

        // Fill rules-list:
        {
            ListView rulesListView = (ListView) findViewById(R.id.activity_show_app_rules_listView_rules);
            appsAdapter = new AppRulesAdapter(this, firewall.subsystem.rulesManager.getRules(appUidGroup));
            rulesListView.setAdapter(appsAdapter);

            if (appsAdapter.getRules().size() > 0)
                findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);
        }
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
