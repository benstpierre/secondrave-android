package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaDecoder implements Runnable {

    private static final String TAG = "MediaDecoder";


    private final ConcurrentLinkedQueue<short[]> decodedAudioQueue;
    private final ConcurrentLinkedQueue<File> downloadedAudioQueue;
    private boolean keepGoing = true;

    public MediaDecoder(ConcurrentLinkedQueue<short[]> decodedAudioQueue,
                        ConcurrentLinkedQueue<File> downloadedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.downloadedAudioQueue = downloadedAudioQueue;
    }

    @Override
    public void run() {
        while (keepGoing) {
            try {
                final File outputFile = downloadedAudioQueue.poll();

                if (outputFile == null || decodedAudioQueue.size() > 1000) {
                    Thread.sleep(1000);
                    continue;
                }

                final MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(outputFile.getPath());

                final MediaFormat format = extractor.getTrackFormat(0);
                final String mime = format.getString(MediaFormat.KEY_MIME);
                final MediaCodec codec = MediaCodec.createDecoderByType(mime);
                codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
                codec.start();

                ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
                final ByteBuffer[] codecInputBuffers = codec.getInputBuffers();

                extractor.selectTrack(0);
                // start decoding
                final long kTimeOutUs = 5000;
                final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;
                int noOutputCounter = 0;
                while (!sawOutputEOS && noOutputCounter < 50) {
                    noOutputCounter++;
                    if (!sawInputEOS) {
                        int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                        if (inputBufIndex >= 0) {
                            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                            int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);
                            long presentationTimeUs = 0;
                            if (sampleSize < 0) {
                                Log.d(TAG, "saw input EOS.");
                                sawInputEOS = true;
                                sampleSize = 0;
                            } else {
                                presentationTimeUs = extractor.getSampleTime();
                            }
                            codec.queueInputBuffer(
                                    inputBufIndex,
                                    0 /* offset */,
                                    sampleSize,
                                    presentationTimeUs,
                                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                            if (!sawInputEOS) {
                                extractor.advance();
                            }
                        }
                    }
                    int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                    if (res >= 0) {
                        //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                        if (info.size > 0) {
                            noOutputCounter = 0;
                        }
                        final ByteBuffer buf = codecOutputBuffers[res];

                        short[] tmpData = new short[info.size];
                        for (int i = 0; i < info.size; i += 2) {
                            tmpData[i] = buf.getShort(i);
                        }

                        decodedAudioQueue.offer(tmpData);

                        codec.releaseOutputBuffer(res, false /* render */);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "saw output EOS.");
                            sawOutputEOS = true;
                        }
                    } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        codecOutputBuffers = codec.getOutputBuffers();
                        Log.d(TAG, "output buffers have changed.");
                    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat oformat = codec.getOutputFormat();
                        Log.d(TAG, "output format has changed to " + oformat);
                    } else {
                        Log.d(TAG, "dequeueOutputBuffer returned " + res);
                    }
                }
                codec.stop();
                codec.release();
                outputFile.delete();
                System.gc();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    }
}
