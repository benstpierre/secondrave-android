package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by benstpierre on 14-10-24.
 */
public class MediaDecoder implements Runnable {

    private static final String TAG = "MediaDecoder";


    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private final ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue;
    private AtomicBoolean keepGoing = new AtomicBoolean();

    public MediaDecoder(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue,
                        ConcurrentLinkedQueue<EncodedTimedAudioChunk> downloadedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.downloadedAudioQueue = downloadedAudioQueue;
    }

    @Override
    public void run() {
        keepGoing.set(true);
        while (keepGoing.get()) {
            try {
                if (decodedAudioQueue.size() > 100) {
                    Thread.sleep(1000);
                    continue;
                }

                if (downloadedAudioQueue.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }

                final EncodedTimedAudioChunk encodedTimedAudioChunk = downloadedAudioQueue.poll();
                if (encodedTimedAudioChunk == null) {
                    throw new RuntimeException("Null Encoded Audio piece");
                }
                final File outputFile = encodedTimedAudioChunk.getContentFile();

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
                            final ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
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

                        byte[] tmpData = new byte[info.size];
                        for (int i = 0; i < info.size; i++) {
                            tmpData[i] = buf.get(i);
                        }

                        decodedAudioQueue.offer(new DecodedTimedAudioChunk(tmpData, encodedTimedAudioChunk.getPlayAt(), encodedTimedAudioChunk.getLengthMS()));

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
                extractor.release();
                //outputFile.delete();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        this.keepGoing.set(false);
    }
}
