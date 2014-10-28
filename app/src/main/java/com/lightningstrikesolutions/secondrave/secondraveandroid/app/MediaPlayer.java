package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaPlayer implements Runnable {


    private final ConcurrentLinkedQueue<short[]> decodedAudioQueue;
    private boolean keepPlaying;

    public MediaPlayer(ConcurrentLinkedQueue<short[]> decodedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 88200, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

        keepPlaying = true;
        audioTrack.play();
        //Keep playing data until stopped
        while (keepPlaying) {
            if (!decodedAudioQueue.isEmpty()) {
                short[] data = decodedAudioQueue.poll();
                if (data.length > 0)
                    System.gc();
                    //audioTrack.write(data, 0, data.length);
            }
        }
        //Stop music
        audioTrack.stop();
    }

    public synchronized void stop() {
        keepPlaying = false;
    }

}