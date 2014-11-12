package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-11-07.
 */
public class QueuedInputStream extends InputStream {


    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private DecodedTimedAudioChunk currentAudioChunk;
    private int currentAudioChunkIndex;

    public QueuedInputStream(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
    }

    @Override
    public int read() throws IOException {
        if (this.currentAudioChunk == null || currentAudioChunkIndex >= this.currentAudioChunk.getPcmData().length) {
            this.currentAudioChunk = decodedAudioQueue.poll();
            currentAudioChunkIndex = 0;
            if (currentAudioChunk == null) {
                return -1;
            }
        }

        final int tmpByte = currentAudioChunk.getPcmData()[currentAudioChunkIndex] & 0xFF;
        currentAudioChunkIndex++;
        return tmpByte;
    }

}
