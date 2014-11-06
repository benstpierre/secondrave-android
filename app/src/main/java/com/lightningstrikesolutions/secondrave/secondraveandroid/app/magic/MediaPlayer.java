package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaPlayer implements Runnable {


    private static final String TAG = "MediaPlayer";
    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private final AtomicBoolean keepPlaying = new AtomicBoolean();
    private final MainActivity mainActivity;

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
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
        Speed speed = Speed.GoodEnough;
        while (keepPlaying.get()) {
            if (!decodedAudioQueue.isEmpty()) {
                final DecodedTimedAudioChunk decodedTimedAudioChunk = decodedAudioQueue.poll();
                if (decodedTimedAudioChunk.isFirstSampleInChunk()) {
                    final long now = System.currentTimeMillis();
                    final long theoreticalEndTime = decodedTimedAudioChunk.getPlayAt() + decodedTimedAudioChunk.getLengthMS();
                    final long actualEndTimeAt1XSpeed = now + decodedTimedAudioChunk.getLengthMS();
                    final long deltaTime = actualEndTimeAt1XSpeed - theoreticalEndTime;
                    if (deltaTime > 5000) {
                        continue;
                    }
                    final int extraSamplesToPlay = (int) (deltaTime * 44100 / 1000);
                    final int timeLeft = (int) (theoreticalEndTime - now);
                    final int speedChange = extraSamplesToPlay * 1000 / timeLeft;

                    if (speedChange > 5000) {
                        if (speed != Speed.Fast) {
                            speed = Speed.Fast;
                            audioTrack.setPlaybackRate(44100 + 5000);
                        }
                    } else if (speedChange < -5000) {
                        if (speed != Speed.Slow) {
                            speed = Speed.Slow;
                            audioTrack.setPlaybackRate(44100 - 5000);
                        }
                    } else {
                        if (Math.abs(speedChange) > 500) {
                            speed = Speed.Custom;
                            audioTrack.setPlaybackRate(44100 + speedChange);
                        } else {
                            if (speed != Speed.GoodEnough) {
                                speed = Speed.GoodEnough;
                                audioTrack.setPlaybackRate(44100);
                            }
                        }
                    }

                    mainActivity.setDelay((int) deltaTime, speedChange, speed);
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

    public enum Speed {
        Fast,
        Slow,
        Custom,
        GoodEnough
    }


    public void stop() {
        keepPlaying.set(false);
    }

}
