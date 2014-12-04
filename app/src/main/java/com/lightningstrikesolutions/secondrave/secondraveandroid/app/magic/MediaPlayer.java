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
    private MainActivity mainActivity;
    private final int driverDelayMs;
    private final ClockService clockService;
    private int modifiedSpeed;
    private int currentChunk;

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity, int driverDelayMs, ClockService clockService) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
        this.driverDelayMs = driverDelayMs;
        this.clockService = clockService;
    }


    private long now() {
        return System.currentTimeMillis() + clockService.getClockOffset();
    }

    private static final int MAX_CORRECTION = 44100 / 2;
    private static final int MIN_CORRECTION = -1 * 44100 / 2;
    private static final double P_GAIN = 0.3;
    private static final double I_GAIN = 0.0;
    private static final double D_GAIN = 0.0;

    private long cumulativeError;
    private long lastError;

    private int doPid(long error) {
        //Calculate p correction
        final double pCorrection = P_GAIN * error;
        //Calculate i correction
        this.cumulativeError = cumulativeError + error;
        final double iCorrection = I_GAIN * cumulativeError;
        //Calculate p correction
        final long slope = error - lastError;
        final double dCorrection = slope * D_GAIN;
        this.lastError = error; // save error for next loop
        final double pidCorrection = pCorrection + iCorrection + dCorrection;

        if (pidCorrection > MAX_CORRECTION) {
            return MAX_CORRECTION;
        } else if (pidCorrection < MIN_CORRECTION) {
            return MIN_CORRECTION;
        } else {
            return (int) pidCorrection;
        }
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
                    this.currentChunk++;
                    //Get current corrected time
                    final long now = now();
                    //Calculate error
                    final long error = 0 - (now - decodedTimedAudioChunk.getPlayAt());
                    //Calculations for skipping clips (when too far behind), sleeping (when too far ahead), or re-sampling (for small differences)


//                    if (error < -1500) { //Skip clip if too far behind
//                        decodedAudioQueue.poll();
//                        continue;
//                    } else

                    if (error > 1500) { //Sleep and try again if you are too far off
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        //Calculations to do re-sampling in order to fix small errors
                        final int correction = doPid(error);
                        this.modifiedSpeed = (int) (44100 + (correction * 44.1));
                        if (mainActivity != null) {
                            if (this.currentChunk % 2 == 0) {
                                mainActivity.setDelay((int) lastError, correction, clockService.getClockOffset(), currentChunk);
                            }
                        }
                    }
                }
                decodedAudioQueue.poll();//Remove the head of the queue as we are about to play the audio chunk
                final byte[] resampledAudio = new Resampler().reSample(decodedTimedAudioChunk.getPcmData(), 2, 16, 44100, modifiedSpeed);
                audioTrack.write(resampledAudio, 0, resampledAudio.length);
            }
        }
        //Stop music
        audioTrack.stop();
    }


    public void stop() {
        keepPlaying.set(false);
    }

    public synchronized void setActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }
}
