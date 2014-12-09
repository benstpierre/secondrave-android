/*
 * Copyright (C) 2011 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Resample signal data (base on bytes)
 *
 * @author jacquet
 */
public class Resampler {


    /**
     * Do resampling. Currently the amplitude is stored by short such that maximum bitsPerSample is 16 (bytePerSample is 2)
     *
     * @param sourceData    The source data in bytes
     * @param bitsPerSample How many bits represents one sample (currently supports max. bitsPerSample=16)
     * @param sourceRate    Sample rate of the source data
     * @param targetRate    Sample rate of the target data
     * @return re-sampled data
     */
    public static ByteBuffer reSample(ByteBuffer sourceData, int channels, int bitsPerSample, int sourceRate, int targetRate) {
        if (sourceRate == targetRate) {
            sourceData.position(0);
            return sourceData;
        }
        //Handy calc, pretty much everything we do will be 16 bit audio (2 bytes)
        final int bytePerSample = bitsPerSample / 8;

        final int originalSampleCount = sourceData.capacity() / bytePerSample / channels;

        final int newSampleCount = Math.round((float) originalSampleCount / sourceRate * targetRate);
        //Allocate output buffer given known sample count and bytesperchannel
        final ByteBuffer interpolatedSamples = ByteBuffer.allocate(newSampleCount * bytePerSample * channels);
        interpolatedSamples.order(ByteOrder.LITTLE_ENDIAN);
        //Needs to be float
        final float lengthMultiplier = (float) newSampleCount / originalSampleCount;

        final int frameSize = channels * bytePerSample;

        // interpolate the value by the linear equation y=mx+c
        for (int i = 0; i < newSampleCount; i++) {
            final int frameStart = frameSize * i;
            for (int channel = 0; channel < channels; channel++) {

                final float currentSamplePosition = i / lengthMultiplier;
                // get the nearest positions for the interpolated point
                final int nearestLeftSamplePosition = (int) currentSamplePosition;

                int nearestRightSamplePosition = nearestLeftSamplePosition + 1;
                if (nearestRightSamplePosition >= originalSampleCount) {
                    nearestRightSamplePosition = originalSampleCount - 1;
                }

                final short nearestRightSample = sourceData.getShort(findIndex(nearestRightSamplePosition, channel, frameSize, bytePerSample));
                final short nearestLeftSample = sourceData.getShort(findIndex(nearestLeftSamplePosition, channel, frameSize, bytePerSample));

                final float slope = nearestRightSample - nearestLeftSample;     // delta x is 1
                float positionFromLeft = currentSamplePosition - nearestLeftSamplePosition;

                final short shortAtLocation = (short) (slope * positionFromLeft + nearestLeftSample);      // y=mx+c

                interpolatedSamples.putShort(shortAtLocation);
            }
        }
        interpolatedSamples.position(0);
        return interpolatedSamples;
    }


    private static int findIndex(int sampleNumber, int channel, int frameSize, int bytesPerSample) {
        final int frameStart = frameSize * sampleNumber;
        return frameStart + (channel * bytesPerSample);
    }

}