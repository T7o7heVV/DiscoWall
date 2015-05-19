package de.uni_kl.informatik.disco.discowall;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterUtils;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;


public class MainActivity extends ActionBarActivity {
//    static {
//        System.loadLibrary("DiscoLib");
//    }
//
//    public native String getStringFromNative();

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

//        Log.v("Main", "JNI test execution: " + getStringFromNative());

        engineTest();

        try {
//            Log.v("Root test", "isDeviceRooted: " + SystemInfo.isDeviceRooted());

//            Log.v("Temp Files", "Path: " + File.createTempFile("__tmp__", ".txt").toString());
        } catch(Exception e)
        {
            Log.e("ERROR On TEST", e.toString());
        }
    }

    private void engineTest() {
//        File binDir = DroidWallFiles.DEPLOYED_BINARIES__DIR.getFile(this);
//        Log.v("ENGINE TEST", "Binary Dir: " + binDir.getAbsolutePath());
//
//        File netfilterBridge = DroidWallFiles.NETFILTER_BRIDGE_BINARY__FILE.getFile(this);
//        Log.v("ENGINE TEST", "NetfilterBridge: " + netfilterBridge.getAbsolutePath());


        try {
            Log.v("ENGINE TEST", "isIptablesModuleInstalled: " + NetfilterUtils.isIptablesModuleInstalled());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
