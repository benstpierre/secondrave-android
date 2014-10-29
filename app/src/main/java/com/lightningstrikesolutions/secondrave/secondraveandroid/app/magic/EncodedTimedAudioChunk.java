package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import java.io.File;

/**
 * Created by benstpierre on 14-10-29.
 */
public class EncodedTimedAudioChunk {


    private File contentFile;
    private long playAy;
    private int lengthMS;

    public EncodedTimedAudioChunk(File contentFile, long playAy, int lengthMS) {
        this.contentFile = contentFile;
        this.playAy = playAy;
        this.lengthMS = lengthMS;
    }

    public File getContentFile() {
        return contentFile;
    }

    public void setContentFile(File contentFile) {
        this.contentFile = contentFile;
    }

    public long getPlayAy() {
        return playAy;
    }

    public void setPlayAy(long playAy) {
        this.playAy = playAy;
    }

    public int getLengthMS() {
        return lengthMS;
    }

    public void setLengthMS(int lengthMS) {
        this.lengthMS = lengthMS;
    }
}
