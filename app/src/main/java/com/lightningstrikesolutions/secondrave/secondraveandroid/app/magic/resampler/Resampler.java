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

package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic.resampler;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Resample signal data (base on bytes)
 *
 * @author jacquet
 */
public class Resampler {

    public Resampler() {
    }

    /**
     * Do resampling. Currently the amplitude is stored by short such that maximum bitsPerSample is 16 (bytePerSample is 2)
     *
     * @param sourceData    The source data in bytes
     * @param bitsPerSample How many bits represents one sample (currently supports max. bitsPerSample=16)
     * @param sourceRate    Sample rate of the source data
     * @param targetRate    Sample rate of the target data
     * @return re-sampled data
     */
    public byte[] reSample(byte[] sourceData, int channels, int bitsPerSample, int sourceRate, int targetRate) {

        // make the bytes to amplitudes first
        final int bytePerSample = bitsPerSample / 8;

        //Determine number of samples
        final int numSamples = sourceData.length / bytePerSample / channels;

        //Split each channel into an amplitude short array
        final List<short[]> sourceDataByChannel = Lists.newArrayList();
        {
            for (int channel = 0; channel < channels; channel++) {
                final short[] currentChannelAmplitudes = new short[numSamples];
                int index = 0;
                for (int j = (channel * bytePerSample); j < sourceData.length - 1; j += bytePerSample) {
                    short amplitude = 0;
                    for (int byteNumber = 0; byteNumber < bytePerSample; byteNumber++) {
                        // little endian
                        amplitude |= (short) ((sourceData[j++] & 0xFF) << (byteNumber * 8));
                    }
                    currentChannelAmplitudes[index++] = amplitude;
                }
                sourceDataByChannel.add(currentChannelAmplitudes);
            }
        }


        final List<byte[]> resampledChannels = Lists.newArrayList();
        for (int c = 0; c < channels; c++) {

            final short[] amplitudes = sourceDataByChannel.get(c);

            // do interpolation
            final short[] targetSample = new LinearInterpolation().interpolate(sourceRate, targetRate, amplitudes);
            final int targetLength = targetSample.length;

            // convert the amplitude to bytes
            final byte[] bytes;
            if (bytePerSample == 1) {
                bytes = new byte[targetLength];
                for (int i = 0; i < targetLength; i++) {
                    bytes[i] = (byte) targetSample[i];
                }
            } else {
                // suppose bytePerSample==2
                bytes = new byte[targetLength * 2];
                for (int i = 0; i < targetSample.length; i++) {
                    // little endian
                    bytes[i * 2] = (byte) (targetSample[i] & 0xff);
                    bytes[i * 2 + 1] = (byte) ((targetSample[i] >> 8) & 0xff);
                }
            }
            resampledChannels.add(bytes);
        }

        final int sizePerChannel = resampledChannels.get(0).length;
        final byte[] result = new byte[sizePerChannel * channels];

        int resultIndex = 0;
        for (int i = 0; i < sizePerChannel - 2; i += 2) {
            for (int c = 0; c < channels; c++) {
                result[resultIndex++] = resampledChannels.get(c)[i];
                result[resultIndex++] = resampledChannels.get(c)[i + 1];
            }
        }
        return result;
    }
}