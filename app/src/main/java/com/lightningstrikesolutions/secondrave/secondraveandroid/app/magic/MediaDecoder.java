package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import com.secondrave.protos.SecondRaveProtos;

import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaDecoder implements Runnable {

    private static final String TAG = "MediaDecoder";


    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private final ConcurrentLinkedQueue<SecondRaveProtos.AudioPiece> downloadedAudioQueue;
    private AtomicBoolean keepGoing = new AtomicBoolean();

    public MediaDecoder(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue,
                        ConcurrentLinkedQueue<SecondRaveProtos.AudioPiece> downloadedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.downloadedAudioQueue = downloadedAudioQueue;
    }

    @Override
    public void run() {
        keepGoing.set(true);
        while (keepGoing.get()) {
            try {
                if (decodedAudioQueue.size() > 200) {
                    Thread.sleep(1000);
                    continue;
                }

                if (downloadedAudioQueue.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }

                final SecondRaveProtos.AudioPiece encodedTimedAudioChunk = downloadedAudioQueue.poll();
                if (encodedTimedAudioChunk == null) {
                    throw new RuntimeException("Null Encoded Audio piece");
                }

                final InputStream inputStream = encodedTimedAudioChunk.getAudioData().newInput();

                boolean isFirstSampleInChunk = true;

                int bytesRead = 0;
                while (bytesRead != -1) {
                    final byte[] tmpData = new byte[4000];
                    bytesRead = inputStream.read(tmpData);
                    if (bytesRead == -1) {
                        continue;
                    }
                    byte[] tmpData2;
                    if (bytesRead < 4000) {
                        tmpData2 = new byte[bytesRead];
                        System.arraycopy(tmpData, 0, tmpData, 0, bytesRead);
                    } else {
                        tmpData2 = tmpData;
                    }
                    decodedAudioQueue.offer(new DecodedTimedAudioChunk(tmpData2, encodedTimedAudioChunk.getPlayAt(), encodedTimedAudioChunk.getDuration(), isFirstSampleInChunk));
                    isFirstSampleInChunk = false;
                }

                inputStream.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        this.keepGoing.set(false);
    }
}
