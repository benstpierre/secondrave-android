package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaPlayer implements Runnable {


    private final ConcurrentLinkedQueue<short[]> decodedAudioQueue;
    private final AtomicBoolean keepPlaying = new AtomicBoolean();

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
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
        keepPlaying.set(true);
        audioTrack.setPlaybackRate(88200);
        audioTrack.play();
        //Keep playing data until stopped
        while (keepPlaying.get()) {
            if (!decodedAudioQueue.isEmpty()) {
                short[] data = decodedAudioQueue.poll();
                if (data.length > 0) {
                    audioTrack.write(data, 0, data.length);
                } else {
                    System.out.println("NOTHING TO PLAY THIS IS VERY BAD");
                }
            }
        }
        //Stop music
        audioTrack.stop();
    }

    public void stop() {
        keepPlaying.set(false);
    }

}
