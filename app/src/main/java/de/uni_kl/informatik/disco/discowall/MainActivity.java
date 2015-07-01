package de.uni_kl.informatik.disco.discowall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewallService.Firewall;
import de.uni_kl.informatik.disco.discowall.firewallService.FirewallService;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.AppUtils;
import de.uni_kl.informatik.disco.discowall.utils.NetworkInterfaceHelper;
import de.uni_kl.informatik.disco.discowall.utils.NetworkUtils;


public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private FirewallService firewallService;

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
            firewallService.getFirewall().enableFirewall(1337);
            firewallService.getFirewall().DEBUG_TEST();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

//        if (true) return; // DEBUGGING GUI

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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.v(LOG_TAG, "conntected to service");

            FirewallService.FirewallBinder binder = (FirewallService.FirewallBinder) service;
            firewallService = binder.getService();

            test();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            firewallService = null;
        }
    };
}
