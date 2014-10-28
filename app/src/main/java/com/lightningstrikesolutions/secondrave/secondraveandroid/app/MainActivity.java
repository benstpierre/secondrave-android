package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.common.collect.Queues;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MainActivity extends Activity {


    private ConcurrentLinkedQueue<short[]> decodedAudioQueue = Queues.newConcurrentLinkedQueue();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void doRofl(View view) throws IOException {
        final ConcurrentLinkedQueue<File> downloadedAudioQueue = Queues.newConcurrentLinkedQueue();
        final MediaDownloader mediaDownloader = new MediaDownloader(downloadedAudioQueue, getApplicationContext().getCacheDir());
        new Thread(mediaDownloader).start();

        final MediaDecoder mediaDecoder = new MediaDecoder(decodedAudioQueue, downloadedAudioQueue);
        new Thread(mediaDecoder).start();

        final MediaPlayer mediaPlayer = new MediaPlayer(decodedAudioQueue);
        new Thread(mediaPlayer).start();
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
