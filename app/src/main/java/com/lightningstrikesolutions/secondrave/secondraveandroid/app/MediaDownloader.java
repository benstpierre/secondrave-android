package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.util.Log;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-27.
 */
public class MediaDownloader implements Runnable {


    private final ConcurrentLinkedQueue<File> downloadedAudioQueue;
    private final File cacheDir;
    private boolean keepGoing = true;
    private long previousTimestamp;

    public MediaDownloader(ConcurrentLinkedQueue<File> downloadedAudioQueue, File cacheDir) {
        this.downloadedAudioQueue = downloadedAudioQueue;
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() {
        previousTimestamp = System.currentTimeMillis();
        while (this.keepGoing) {
            try {

                if (downloadedAudioQueue.size() > 3) {
                    Thread.sleep(5000);
                    continue;
                }

                final File outputFile = File.createTempFile("prefix", "extension", cacheDir);
                int count;

                final URL url = new URL("http://192.168.1.48:8080/unodish-web-1.0/RaveService");

                final Map<String, String> headers = Maps.newHashMap();
                headers.put("NEWEST_SAMPLE_AFTER_INSTANT", String.valueOf(System.currentTimeMillis()));

                final URLConnection connection = url.openConnection();
                connection.connect();
                final String rolf = connection.getHeaderField("PLAYAT");
                final String berry = connection.getHeaderField("PLAYLENGTH");

                // download the file
                final InputStream input = new BufferedInputStream(url.openStream(), 8192);
                // Output stream
                final OutputStream output = new FileOutputStream(outputFile);
                //Copy file
                ByteStreams.copy(input, output);

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                downloadedAudioQueue.offer(outputFile);
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
        }
    }
}
