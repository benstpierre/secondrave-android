package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.common.collect.Queues;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.EncodedTimedAudioChunk;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaDecoder;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaDownloader;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaPlayer;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MainActivity extends Activity {


    private MediaDownloader mediaDownloader;
    private MediaDecoder mediaDecoder;
    private MediaPlayer mediaPlayer;
    private View btnStartTheParty;
    private View btnStopTheParty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.btnStartTheParty = findViewById(R.id.btnStartTheParty);
        this.btnStopTheParty = findViewById(R.id.btnStopTheParty);
    }

    public void stopTheParty(View view) throws IOException {
        this.btnStopTheParty.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.mediaPlayer.stop();
                MainActivity.this.mediaDecoder.stop();
                MainActivity.this.mediaDownloader.stop();
                //Clear does not work, we need to guaranteed these two ConcurrentLinkedQueue are clear before continuing
                //noinspection StatementWithEmptyBody
                //Only allow party to start when queues are purged
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.btnStartTheParty.setEnabled(true);
                    }
                });
            }
        }).start();

    }

    public void startTheParty(View view) throws IOException {
        //Setup new downloaded/decoded queues
        ConcurrentLinkedQueue<short[]> decodedAudioQueue = Queues.newConcurrentLinkedQueue();
        ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue = Queues.newConcurrentLinkedQueue();
        //Disable/enable buttons as needed
        btnStartTheParty.setEnabled(false);
        final ThreadGroup threadGroup = new ThreadGroup("Audio Threads");
        //Start Media Downloader
        this.mediaDownloader = new MediaDownloader(downloadedAudioQueue, getApplicationContext().getCacheDir());
        new Thread(threadGroup, mediaDownloader, "Media Downloader").start();
        //Start Media Decoder
        this.mediaDecoder = new MediaDecoder(decodedAudioQueue, downloadedAudioQueue);
        new Thread(threadGroup, mediaDecoder, "Media Decoder").start();
        //Start Media Player
        this.mediaPlayer = new MediaPlayer(decodedAudioQueue);
        new Thread(threadGroup, mediaPlayer, "Media Player").start();
        btnStopTheParty.setEnabled(true);
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
