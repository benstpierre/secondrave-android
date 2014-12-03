package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import com.google.common.collect.Queues;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.*;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-11-13.
 */
public class SecondRaveApplication extends Application {

    private static final String TAG = "SecondRaveApplication";
    private MediaDownloader mediaDownloader;
    private MediaPlayer mediaPlayer;
    private ClockService clockService;
    private final AtomicBoolean partyStarted = new AtomicBoolean(false);
    private final AtomicBoolean partyChanging = new AtomicBoolean(false);


    public int getAudioLatency() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        try {
            Method m = am.getClass().getMethod("getOutputLatency", int.class);
            Integer result = (Integer) m.invoke(am, AudioManager.STREAM_MUSIC);
            Log.e(TAG, "AudioLatency is " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "AudioLatency is unknown defaulting to 0");
            return 0;
        }
    }

    public AtomicBoolean getPartyStarted() {
        return partyStarted;
    }

    public AtomicBoolean getPartyChanging() {
        return partyChanging;
    }

    public MediaDownloader getMediaDownloader() {
        return mediaDownloader;
    }

    public void setMediaDownloader(MediaDownloader mediaDownloader) {
        this.mediaDownloader = mediaDownloader;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public ClockService getClockService() {
        return clockService;
    }

    public void setClockService(ClockService clockService) {
        this.clockService = clockService;
    }

    public void stopTheParty() {
        this.partyChanging.set(true);
        {
            this.clockService.stop();
            this.clockService = null;
            this.mediaPlayer.stop();
            this.mediaPlayer = null;
            this.mediaDownloader.stop();
            this.mediaDownloader = null;
        }
        this.partyChanging.set(false);
        this.partyStarted.set(false);
    }

    public void startTheParty(MainActivity mainActivity) {
        this.partyChanging.set(true);
        this.partyStarted.set(true);
        //Setup new decoded audio queue
        final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue = Queues.newConcurrentLinkedQueue();

        this.clockService = new ClockService(this.getAudioLatency());
        new Thread(clockService).start();

        final ThreadGroup threadGroup = new ThreadGroup("Audio Threads");
        //Start Media Downloader/decoder
        this.mediaDownloader = new MediaDownloader(decodedAudioQueue);
        new Thread(threadGroup, mediaDownloader, "Media Downloader").start();
        //Start Media Player
        this.mediaPlayer = new MediaPlayer(decodedAudioQueue, mainActivity, getAudioLatency(), clockService);
        new Thread(threadGroup, mediaPlayer, "Media Player").start();
        this.partyChanging.set(false);
        this.partyStarted.set(true);
    }
}
