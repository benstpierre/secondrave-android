package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AndroidAudioPlayer implements AudioProcessor {
    private AudioTrack audioTrack;
    private boolean started;

    AndroidAudioPlayer(TarsosDSPAudioFormat audioFormat, int bufferSize) {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                (int) audioFormat.getSampleRate(),
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        if (!started) {
            audioTrack.play();
        }
        //final short[] shorts = new short[audioEvent.getBufferSize() / 2];
        //ByteBuffer.wrap(audioEvent.getByteBuffer()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        final byte[] rofls = audioEvent.getByteBuffer();
        audioTrack.write(rofls, 0, rofls.length);
        return true;
    }

    @Override
    public void processingFinished() {
    }

}
