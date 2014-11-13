package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;
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
    private final ClockService clockService;
    private int modifiedSpeed;

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity, int driverDelayMs, ClockService clockService) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
        this.driverDelayMs = driverDelayMs;
        this.clockService = clockService;
    }


    private long now() {
        return System.currentTimeMillis() + clockService.getClockOffset();
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
                final DecodedTimedAudioChunk decodedTimedAudioChunk = decodedAudioQueue.peek(); //do not poll the queue until we know we have useful sample
                if (decodedTimedAudioChunk.isFirstSampleInChunk()) {
                    final long now = now();

                    final long theoreticalEndTime = decodedTimedAudioChunk.getPlayAt() + decodedTimedAudioChunk.getLengthMS();
                    final long actualEndTimeAt1XSpeed = now + decodedTimedAudioChunk.getLengthMS();
                    final long deltaTime = actualEndTimeAt1XSpeed - theoreticalEndTime;
                    //determine if you are too far ahead or behind
                    if (deltaTime > 3000) {
                        decodedAudioQueue.poll();//toss this sample, you are too far behind
                        continue;
                    } else if (deltaTime < -500) {
                        try {
                            Thread.sleep(100);  //sleep and try again
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    } else {
                        decodedAudioQueue.poll(); // poll this as its going to actually get used
                    }
                    final int extraSamplesToPlay = (int) (deltaTime * 44100 / 1000);
                    final int timeLeft = (int) (theoreticalEndTime - now);
                    final int speedChange = extraSamplesToPlay * 1000 / timeLeft;
                    this.modifiedSpeed = 44100 - speedChange;
                    mainActivity.setDelay((int) deltaTime, modifiedSpeed);
                } else {
                    decodedAudioQueue.poll();
                }
                final byte[] data = decodedTimedAudioChunk.getPcmData();
                if (data.length > 0) {
                    if (modifiedSpeed < 4410) {
                        continue;
                    } else {
                        byte[] newSamples = new Resampler().reSample(data, 16, 44100, modifiedSpeed);
                        audioTrack.write(newSamples, 0, newSamples.length);
                    }
                } else {
                    Log.w(TAG, "No audio samples to play");
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
