package com.lightningstrikesolutions.secondrave.secondraveandroid.app;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class MainActivity extends Activity {

    private static final String TAG = "DecoderTest";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void doRofl(View view) {
        try {
            TextView rofl = (TextView) findViewById(R.id.helloWorldBanner);
            rofl.setText("ROFLBERRY PWN CAKES");


            int decodedIdx = 0;
            short[] decoded = new short[1542144];

            final AssetFileDescriptor testFd = getAssets().openFd("example1.aac");

            final MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(), testFd.getLength());

            final MediaFormat format = extractor.getTrackFormat(0);
            final String mime = format.getString(MediaFormat.KEY_MIME);
            final MediaCodec codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();


            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();

            extractor.selectTrack(0);
            // start decoding
            final long kTimeOutUs = 5000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
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
                    if (decodedIdx + (info.size / 2) >= decoded.length) {
                        decoded = Arrays.copyOf(decoded, decodedIdx + (info.size / 2));
                    }
                    for (int i = 0; i < info.size; i += 2) {
                        decoded[decodedIdx++] = buf.getShort(i);
                    }
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

            final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(decoded, 0, decoded.length);
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
