package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

/**
 * Created by benstpierre on 14-10-29.
 */
public class DecodedTimedAudioChunk {


    private byte[] pcmData;
    private long playAt;
    private int lengthMS;

    public DecodedTimedAudioChunk() {
    }

    public DecodedTimedAudioChunk(byte[] pcmData, long playAt, int lengthMS) {
        this.pcmData = pcmData;
        this.playAt = playAt;
        this.lengthMS = lengthMS;
    }

    public byte[] getPcmData() {
        return pcmData;
    }

    public void setPcmData(byte[] pcmData) {
        this.pcmData = pcmData;
    }

    public long getPlayAt() {
        return playAt;
    }

    public void setPlayAt(long playAt) {
        this.playAt = playAt;
    }

    public int getLengthMS() {
        return lengthMS;
    }

    public void setLengthMS(int lengthMS) {
        this.lengthMS = lengthMS;
    }
}
