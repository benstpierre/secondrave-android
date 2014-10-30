package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.util.Log;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-27.
 */
public class MediaDownloader implements Runnable {


    private final ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue;
    private final File cacheDir;
    private AtomicBoolean keepGoing = new AtomicBoolean();

    public MediaDownloader(ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue, File cacheDir) {
        this.downloadedAudioQueue = downloadedAudioQueue;
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() {
        this.keepGoing.set(true);
        while (this.keepGoing.get()) {
            try {
                if (downloadedAudioQueue.size() > 3) {
                    Thread.sleep(5000);
                    continue;
                }

                final File outputFile = File.createTempFile("audiobuffer", "tmp", cacheDir);

                final String url = "http://192.168.1.48:8080/unodish-web-1.0/RaveService";
                final HttpClient httpclient = new DefaultHttpClient();
                final HttpRequest request = new HttpGet(url);
                request.addHeader("NEWEST_SAMPLE_AFTER_INSTANT", String.valueOf(System.currentTimeMillis()));
                final HttpResponse response = httpclient.execute(new HttpGet(url));
                final StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                    final String strPlayAt = response.getFirstHeader("PLAYAT").getValue();
                    final String strPlayLength = response.getFirstHeader("PLAYLENGTH").getValue();
                    final InputStream in = response.getEntity().getContent();
                    final OutputStream out = Files.asByteSink(outputFile).openBufferedStream();
                    ByteStreams.copy(in, out);
                    in.close();
                    out.close();
                    downloadedAudioQueue.offer(new EncodedTimedAudioChunk(outputFile, Long.valueOf(strPlayAt), Integer.valueOf(strPlayLength)));
                } else {
                    outputFile.delete();
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
        }
        System.out.println("Media Downloader is off");
    }


    public void stop() {
        this.keepGoing.set(false);
    }
}
