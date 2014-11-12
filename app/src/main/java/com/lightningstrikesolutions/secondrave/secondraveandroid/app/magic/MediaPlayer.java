package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;

import java.io.BufferedInputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaPlayer implements Runnable {


    private static final String TAG = "MediaPlayer";
    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private final MainActivity mainActivity;
    private final int driverDelayMs;
    private AudioDispatcher audioDispatcher;

    public MediaPlayer(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity, int driverDelayMs) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
        this.driverDelayMs = driverDelayMs;
    }

    @Override
    public void run() {
        //Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        //Time warping stuff

        final TarsosDSPAudioInputStream audioStream = new UniversalAudioInputStream(
                new BufferedInputStream(new QueuedInputStream(decodedAudioQueue), 1024 * 25),
                new TarsosDSPAudioFormat(44100, 16, 1, true, false)
        );
        this.audioDispatcher = new AudioDispatcher(audioStream, 1024, 128);

        //final WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(WaveformSimilarityBasedOverlapAdd.Parameters.slowdownDefaults(1.0, 44100.0));
        //dispatcher.addAudioProcessor(wsola);
        audioDispatcher.addAudioProcessor(new AndroidAudioPlayer(audioDispatcher.getFormat(), 2048));
        new Thread(audioDispatcher).run();


//                long now = System.currentTimeMillis();
//                now = now + driverDelayMs;
//                final long theoreticalEndTime = decodedTimedAudioChunk.getPlayAt() + decodedTimedAudioChunk.getLengthMS();
//                final long actualEndTimeAt1XSpeed = now + decodedTimedAudioChunk.getLengthMS();
//                final long deltaTime = actualEndTimeAt1XSpeed - theoreticalEndTime;
//                if (deltaTime > 5000) {
//                    //continue;
//                }
//                final int extraSamplesToPlay = (int) (deltaTime * 44100 / 1000);
//                final int timeLeft = (int) (theoreticalEndTime - now);
//                final int speedChange = extraSamplesToPlay * 1000 / timeLeft;
//
//                mainActivity.setDelay((int) deltaTime, speedChange, speed);

    }


    public void stop() {
        audioDispatcher.stop();
    }

}
