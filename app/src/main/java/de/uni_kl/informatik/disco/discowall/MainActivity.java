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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

import de.uni_kl.informatik.disco.discowall.firewallService.FirewallService;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterUtils;
import de.uni_kl.informatik.disco.discowall.netfilter.NfqueueControl;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;


public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private FirewallService firewallService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void sendButtonClicked(View sendButtonView) {
        Button sendButton = (Button)sendButtonView;
        Log.v("Main", "button clicked");

        EditText editText = (EditText) findViewById(R.id.editText);
        Log.v("Main", "edit text: " + editText.getText());

        try {
//            Log.v("ENGINE TEST", "isIptablesModuleInstalled: " + NetfilterUtils.isIptablesModuleInstalled());

            if (editText.getText().equals("stop"))
                firewallService.stopFirewallService();
            else
                firewallService.enableFirewall(1337);

//            Log.i("SERVICE TEST", "is connected: " + (firewallService != null));
//            Log.i("SERVICE TEST", "service.isFirewallRunning(): " + firewallService.isFirewallRunning());
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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.v(LOG_TAG, "conntected to service");

            FirewallService.FirewallBinder binder = (FirewallService.FirewallBinder) service;
            firewallService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            firewallService = null;
        }
    };
}
