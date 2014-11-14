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


    private View btnStartTheParty;
    private View btnStopTheParty;
    private TextView txtDelay;
    private int audioLatency;
    private int currentChunk;
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
    }

    private void bindUi() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.btnStartTheParty.setEnabled(!application.getPartyStarted().get() && !application.getPartyChanging().get());
                MainActivity.this.btnStopTheParty.setEnabled(application.getPartyStarted().get() && !application.getPartyChanging().get());
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


    public void setDelay(final int delay, final int speedChange, final long clockOffset) {
        this.currentChunk++;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.txtDelay.setText("Chunk=" + currentChunk + "\n"
                                + "Behind=" + delay + " ms\n"
                                + "Change=" + speedChange + "hz\n"
                                + "NtpOffset= " + clockOffset
                );
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
