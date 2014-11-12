package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.resampler.Resampler;

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
    private final int driverDelayMs;
    private int modifiedSpeed;

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity, int driverDelayMs) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
        this.driverDelayMs = driverDelayMs;
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
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);
        keepPlaying.set(true);
        audioTrack.play();
        //Keep playing data until stopped
        while (keepPlaying.get()) {
            if (!decodedAudioQueue.isEmpty()) {
                final DecodedTimedAudioChunk decodedTimedAudioChunk = decodedAudioQueue.poll();
                if (decodedTimedAudioChunk.isFirstSampleInChunk()) {
                    long now = System.currentTimeMillis();
                    //now = now + driverDelayMs;

                    final long theoreticalEndTime = decodedTimedAudioChunk.getPlayAt() + decodedTimedAudioChunk.getLengthMS();
                    final long actualEndTimeAt1XSpeed = now + decodedTimedAudioChunk.getLengthMS();
                    final long deltaTime = actualEndTimeAt1XSpeed - theoreticalEndTime;
                    if (deltaTime > 5000) {
                        continue;
                    }
                    final int extraSamplesToPlay = (int) (deltaTime * 44100 / 1000);
                    final int timeLeft = (int) (theoreticalEndTime - now);
                    final int speedChange = extraSamplesToPlay * 1000 / timeLeft;
                    this.modifiedSpeed = 44100 - speedChange;
                    mainActivity.setDelay((int) deltaTime, modifiedSpeed);
                }
                final byte[] data = decodedTimedAudioChunk.getPcmData();
                if (data.length > 0) {
                    byte[] newSamples = new Resampler().reSample(data, 16, 44100, modifiedSpeed);

                    audioTrack.write(newSamples, 0, newSamples.length);
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
