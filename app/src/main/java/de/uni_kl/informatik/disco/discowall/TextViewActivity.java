package de.uni_kl.informatik.disco.discowall;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class TextViewActivity extends ActionBarActivity {
    private static final String INTENT_EXTRA_TEXTVIEW_CONTENT = "textview-content";
    private static final String INTENT_EXTRA_ACTIVITY_TITLE = "title";

    private TextView getControlTextView() {
        return (TextView) findViewById(R.id.textViewActivity_textView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_view);

        Bundle extras;
        if (savedInstanceState != null)
            extras = savedInstanceState;
        else
            extras = getIntent().getExtras();

        getControlTextView().setText(extras.getString(INTENT_EXTRA_TEXTVIEW_CONTENT));
        setTitle(extras.getString(INTENT_EXTRA_ACTIVITY_TITLE));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // puts current content into intent, so that pause/resume (also screen-rotation) works.
        outState.putAll(getIntent().getExtras());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_text_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_main_menu_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void showText(Context context, String title, String text) {
        Intent i = new Intent(context, TextViewActivity.class);

        i.putExtra(INTENT_EXTRA_TEXTVIEW_CONTENT, text);
        i.putExtra(INTENT_EXTRA_ACTIVITY_TITLE, title);
        context.startActivity(i);
    }
}
