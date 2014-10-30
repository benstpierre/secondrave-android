package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import java.io.File;

/**
 * Created by benstpierre on 14-10-29.
 */
public class EncodedTimedAudioChunk {


    private File contentFile;
    private long playAt;
    private int lengthMS;

    public EncodedTimedAudioChunk(File contentFile, long playAt, int lengthMS) {
        this.contentFile = contentFile;
        this.playAt = playAt;
        this.lengthMS = lengthMS;
    }

    public File getContentFile() {
        return contentFile;
    }

    public void setContentFile(File contentFile) {
        this.contentFile = contentFile;
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
