package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.util.Log;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by benstpierre on 14-11-13.
 */
public class ClockService implements Runnable {

    private static final String TAG = "ClockService";
    private final String host;
    private AtomicBoolean keepGoing = new AtomicBoolean(true);
    private AtomicLong clockOffset = new AtomicLong(0);
    private final long audioCardDelay;

    public ClockService(long audioCardDelay, String host) {
        this.audioCardDelay = audioCardDelay;
        this.host = host;
    }

    @Override
    public void run() {
        while (keepGoing.get()) {
            try {
                timeTCP(host);
                Thread.sleep(10 * 1000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Long getClockOffset() {
        return clockOffset.get() + audioCardDelay;
    }

    public void timeTCP(String host) throws IOException {
        final NTPUDPClient client = new NTPUDPClient();
        try {
            // We want to timeout if a response takes longer than 10 seconds
            client.setDefaultTimeout(10000);
            try {
                client.open();
                try {
                    final TimeInfo info = client.getTime(InetAddress.getByName(host));
                    info.computeDetails();
                    ClockService.this.clockOffset.set(info.getOffset());
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } catch (SocketException e) {
                Log.w(TAG, e);
            }
        } finally {
            client.close();
        }
    }

    public void stop() {
        keepGoing.set(false);
    }

    public long getTime() {
        final long offset = getClockOffset();
        return System.currentTimeMillis() + offset;
    }
}
