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

import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

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

        // make the bytes to amplitudes first
        final int bytePerSample = bitsPerSample / 8;
        //Determine size per channel in bytes
        final int byteSizePerChannel = sourceData.capacity() / channels;

        //Split each channel into its own byte array
        final List<ByteBuffer> sourceDataByChannel = Lists.newArrayList();
        for (int channel = 0; channel < channels; channel++) {
            final ByteBuffer currentChannelData = ByteBuffer.allocate(byteSizePerChannel);
            currentChannelData.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = channel * bytePerSample; i + 1 < sourceData.capacity(); i += channels * bytePerSample) {
                currentChannelData.putShort(sourceData.getShort(i));
            }
            sourceDataByChannel.add(currentChannelData);
        }

        final List<ByteBuffer> resampledChannels = Lists.newArrayList();
        for (int channel = 0; channel < channels; channel++) {
            resampledChannels.add(interpolate(sourceRate, targetRate, sourceDataByChannel.get(channel)));
        }

        final int sizePerChannel = resampledChannels.get(0).capacity();

        final ByteBuffer result = ByteBuffer.allocate(sizePerChannel * channels);
        result.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i + 1 < sizePerChannel; i += 2) {
            for (int c = 0; c < channels; c++) {
                final short sample = resampledChannels.get(c).getShort(i);
                result.putShort(sample);
            }
        }
        return result;
    }


    /**
     * Do interpolation on the samples according to the original and destinated sample rates
     *
     * @param oldSampleRate sample rate of the original samples
     * @param newSampleRate sample rate of the interpolated samples
     * @param samples       original samples
     * @return interpolated samples
     */
    public static ByteBuffer interpolate(int oldSampleRate, int newSampleRate, ByteBuffer samples) {

        if (oldSampleRate == newSampleRate) {
            return samples;
        }
        final int originalSampleCount = samples.capacity() / 2;

        final int newSampleCount = Math.round((float) originalSampleCount / oldSampleRate * newSampleRate);
        //Allocate output buffer as we know 1 sample = 2 bytes
        final ByteBuffer interpolatedSamples = ByteBuffer.allocate(newSampleCount * 2);
        interpolatedSamples.order(ByteOrder.LITTLE_ENDIAN);
        //Needs to be float
        final float lengthMultiplier = (float) newSampleCount / originalSampleCount;

        // interpolate the value by the linear equation y=mx+c
        for (int i = 0; i < newSampleCount; i++) {

            final float currentSamplePosition = i / lengthMultiplier;
            // get the nearest positions for the interpolated point
            final int nearestLeftSamplePosition = (int) currentSamplePosition;

            int nearestRightSamplePosition = nearestLeftSamplePosition + 1;
            if (nearestRightSamplePosition >= originalSampleCount) {
                nearestRightSamplePosition = originalSampleCount - 1;
            }

            final short nearestRightSample = samples.getShort(nearestRightSamplePosition * 2);
            final short nearestLeftSample = samples.getShort(nearestLeftSamplePosition * 2);
            final float slope = nearestRightSample - nearestLeftSample;     // delta x is 1
            float positionFromLeft = currentSamplePosition - nearestLeftSamplePosition;

            final short shortAtLocation = (short) (slope * positionFromLeft + nearestLeftSample);      // y=mx+c
            interpolatedSamples.putShort(i * 2, shortAtLocation);
        }
        return interpolatedSamples;
    }
}