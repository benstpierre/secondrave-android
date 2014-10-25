package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.common.collect.Queues;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;


public class MainActivity extends Activity implements Runnable {

    private static final String TAG = "DecoderTest";
    private boolean playbackStarted;

    private boolean keepPlaying = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    private final ConcurrentLinkedQueue<short[]> decodedAudioQueue = Queues.newConcurrentLinkedQueue();

    public void run() {
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

        audioTrack.play();
        //Keep playing data until stopped
        while (keepPlaying) {
            if (!decodedAudioQueue.isEmpty()) {
                short[] data = decodedAudioQueue.poll();
                audioTrack.write(data, 0, data.length);
            }
        }
        //Stop music
        audioTrack.stop();
    }

    public void doRofl(View view) {
        try {

            final TextView rofl = (TextView) findViewById(R.id.helloWorldBanner);
            rofl.setText("ROFLBERRY PWN CAKES");

            final AssetFileDescriptor testFd = getAssets().openFd("example1.aac");

            final MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(), testFd.getLength());

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
                    if (!playbackStarted) {
                        playbackStarted = true;
                        new Thread(this).start();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
