package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaPlayer implements Runnable {


    private static final String TAG = "MediaPlayer";
    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private final AtomicBoolean keepPlaying = new AtomicBoolean();

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);
        keepPlaying.set(true);
        audioTrack.setPlaybackRate(44100);
        audioTrack.play();
        //Keep playing data until stopped
        while (keepPlaying.get()) {
            if (!decodedAudioQueue.isEmpty()) {
                final DecodedTimedAudioChunk decodedTimedAudioChunk = decodedAudioQueue.poll();
                final long now = System.currentTimeMillis();

                final long theoreticalEndTime = decodedTimedAudioChunk.getPlayAt() + decodedTimedAudioChunk.getLengthMS();
                final long actualEndTimeAt1XSpeed = now + decodedTimedAudioChunk.getLengthMS();
                final long deltaTime = actualEndTimeAt1XSpeed - theoreticalEndTime;
                final int extraSamplesToPlay = (int) (deltaTime * 44100 / 1000);
                final int timeLeft = (int) (theoreticalEndTime - now);
                final int speedChange = extraSamplesToPlay * 1000 / timeLeft;

                Log.i(TAG, "Theo Speed change is " + speedChange);
                if (speedChange > 3000) {
                    audioTrack.setPlaybackRate(44100 + 3000);
                } else if (speedChange < -3000) {
                    audioTrack.setPlaybackRate(44100 - 3000);
                } else {
                    audioTrack.setPlaybackRate(44100);
                }

                final byte[] data = decodedTimedAudioChunk.getPcmData();
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
