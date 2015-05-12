package de.uni_kl.informatik.disco.discowall;

import android.content.res.AssetManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.uni_kl.informatik.disco.discowall.utils.RootUtils;
import de.uni_kl.informatik.disco.discowall.utils.ShellExecute;


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

        assetsTest();

        try {
//            ShellExecute.ShellExecuteResult result = RootUtils.shellExecuteAsRoot("echo start", "id", "echo yes", "echo done.");
//            Log.v("Root test", "execution result: " + result.processOutput);

//            Log.v("Root test", "isDeviceRooted: " + RootUtils.isDeviceRooted());
        } catch(Exception e)
        {

        }
    }

    private void assetsTest() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("ASSETS ERROR", e.getMessage());
        }

        Log.v("ASSETS-Test", "files count: " + files.length);

        for (String file : files)
            Log.v("ASSETS-Test", "> file: " + file);

        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("test.txt");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        if (inputStream == null) {
            Log.e("ASSETS-Test", "file not foundt: " + "test.txt");
            return;
        }

        String text = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        EditText editText = (EditText) findViewById(R.id.editText);

        try {
            editText.setText(reader.readLine());
            Thread.sleep(1000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
