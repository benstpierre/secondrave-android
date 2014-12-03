package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final String DIAGNOSTICS_STRING = "DIAGNOSTICS_STRING";
    public static final String HOST = "10.0.1.13";


    private View btnStartTheParty;
    private View btnStopTheParty;
    private TextView txtDelay;
    private SecondRaveApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.btnStartTheParty = findViewById(R.id.btnStartTheParty);
        this.btnStopTheParty = findViewById(R.id.btnStopTheParty);
        this.txtDelay = (TextView) findViewById(R.id.txtDelay);
        this.application = (SecondRaveApplication) getApplication();
        if (this.application.getMediaPlayer() != null) {
            this.application.getMediaPlayer().setActivity(this);
        }
        bindUi();
        if (savedInstanceState != null) {
            final String diagnosticsString = savedInstanceState.getString(DIAGNOSTICS_STRING);
            this.txtDelay.setText(diagnosticsString == null ? "" : diagnosticsString);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DIAGNOSTICS_STRING, String.valueOf(this.txtDelay.getText()));
    }

    private void bindUi() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.btnStartTheParty.setEnabled(!application.getPartyStarted().get() && !application.getPartyChanging().get());
                MainActivity.this.btnStopTheParty.setEnabled(application.getPartyStarted().get() && !application.getPartyChanging().get());
                if (!application.getPartyStarted().get()) {
                    MainActivity.this.txtDelay.setText("");
                }
            }
        });
    }

    public void stopTheParty(View view) throws IOException {
        this.application.getPartyChanging().set(true);
        this.application.getPartyStarted().set(false);
        bindUi();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.application.stopTheParty();
                bindUi();
            }
        }).start();
    }


    public void startTheParty(View view) throws IOException {
        this.application.getPartyChanging().set(true);
        this.application.getPartyStarted().set(true);
        bindUi();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.application.startTheParty(MainActivity.this);
                bindUi();
            }
        }).start();
    }


    public void setDelay(final int error, final int speedChange, final long clockOffset, final int currentChunk) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String diagnostics = "Chunk=#" + currentChunk + "\n"
                        + "Error=" + error + " ms\n"
                        + "Correction=" + speedChange + "hz\n"
                        + "NtpOffset= " + clockOffset;
                MainActivity.this.txtDelay.setText(diagnostics);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }
}
