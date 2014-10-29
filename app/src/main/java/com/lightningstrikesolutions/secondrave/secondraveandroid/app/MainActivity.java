package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.common.collect.Queues;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.EncodedTimedAudioChunk;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaDecoder;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaDownloader;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MainActivity extends Activity {


    private ConcurrentLinkedQueue<short[]> decodedAudioQueue = Queues.newConcurrentLinkedQueue();
    private android.media.MediaPlayer _shootMP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void doRofl(View view) throws IOException {
        final ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue = Queues.newConcurrentLinkedQueue();
        final MediaDownloader mediaDownloader = new MediaDownloader(downloadedAudioQueue, getApplicationContext().getCacheDir());
        new Thread(mediaDownloader).start();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        if (downloadedAudioQueue.size() > 3) {
//                            Thread.sleep(5000);
//                            continue;
//                        }
//                        final File outputFile = File.createTempFile("prefix", "extension", getApplicationContext().getCacheDir());
//
//                        Files.asByteSink(outputFile).writeFrom(getAssets().openFd("sample1.mp3").createInputStream());
//
//                        downloadedAudioQueue.offer(outputFile);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        }).start();

        final MediaDecoder mediaDecoder = new MediaDecoder(decodedAudioQueue, downloadedAudioQueue);
        new Thread(mediaDecoder).start();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final Random random = new Random();
//                while (true) {
//                    if (decodedAudioQueue.size() > 100) {
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                    final short[] bs = new short[5 * 2 * 44100];
//                    for (int i = 0; i < bs.length; i++) {
//                        bs[i] = (short) random.nextInt(Short.MAX_VALUE + 1);
//                    }
//                    decodedAudioQueue.offer(bs);
//                }
//            }
//        }).start();

        final MediaPlayer mediaPlayer = new MediaPlayer(decodedAudioQueue);
        new Thread(mediaPlayer).start();
    }

    public void playASound(View view) throws IOException {
        final AudioManager meng = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (_shootMP == null)
                _shootMP = android.media.MediaPlayer.create(getApplicationContext(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (_shootMP != null)
                _shootMP.start();
        }
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
