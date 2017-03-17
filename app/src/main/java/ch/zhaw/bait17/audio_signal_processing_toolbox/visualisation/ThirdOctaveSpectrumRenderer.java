/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author georgrem, stockan1
 */
public class ThirdOctaveSpectrumRenderer  implements SpectrumRenderer {

    private final int HISTORY_LENGTH = 3;
    private static final String TAG = ThirdOctaveSpectrumRenderer.class.getSimpleName();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.0K");
    private static final int OCTAVE_BANDS = 31;
    private static final int REFERENCE_CENTER_FREQUENCY = 1000;
    private static final int OCTAVE_BAND_REFERENCE_FREQUENCY = 18;
    private static final int dB_RANGE = 96;

    private double[] centreFrequencies;
    private double[] thirdOctaveFrequencyBoundaries;
    private int width, heigth;
    private TextPaint textPaint;
    private Paint paint;
    List<float[]> spectrumHistory;

    /**
     * Creates an instances of ThirdOctaveSpectrumRenderer.
     * @param paint
     */
    public ThirdOctaveSpectrumRenderer(Paint paint, TextPaint textPaint) {
        this.paint = paint;
        this.textPaint = textPaint;
        spectrumHistory = new ArrayList<>(HISTORY_LENGTH);

        calculateCentreFrequencies();
        calculateThirdOctaveBands();
    }

    /**
     * Go through all centre frequencies and calculate lower and upper frequency bounds.
     */
    private void calculateThirdOctaveBands() {
        if (centreFrequencies != null) {
            thirdOctaveFrequencyBoundaries = new double[(centreFrequencies.length * 2) + 1];
            int centreFrequencyIndex = 0;
            double factor = Math.pow(2.0, 1d / 6);
            double firstLowerBound = centreFrequencies[centreFrequencyIndex] / factor;
            thirdOctaveFrequencyBoundaries[0] = firstLowerBound;

            for (int i = 1; i < thirdOctaveFrequencyBoundaries.length; ) {
                double upperBound = centreFrequencies[centreFrequencyIndex] * factor;
                // Centre frequency
                thirdOctaveFrequencyBoundaries[i++] = centreFrequencies[centreFrequencyIndex];
                thirdOctaveFrequencyBoundaries[i++] = upperBound;
                centreFrequencyIndex++;
            }
        }
    }

    private void calculateCentreFrequencies() {
        centreFrequencies = new double[OCTAVE_BANDS + 1];
        centreFrequencies[OCTAVE_BAND_REFERENCE_FREQUENCY] = REFERENCE_CENTER_FREQUENCY;
        final double factor = Math.pow(2, 1d/3);
        // Fill lower centre
        for (int i = OCTAVE_BAND_REFERENCE_FREQUENCY - 1; i >= 0; i--) {
            centreFrequencies[i] = centreFrequencies[i + 1] / factor;
        }
        // Fill higher centre
        for (int i = OCTAVE_BAND_REFERENCE_FREQUENCY; i < centreFrequencies.length-1; i++) {
            centreFrequencies[i + 1] = centreFrequencies[i] * factor;
        }
    }

    public void render(Canvas canvas, @NonNull short[] samples, int sampleRate) {
        if (sampleRate > 0) {
            float[] spectrum = getPowerSpectrum(samples);

            if (spectrum != null) {
                int nFFT = spectrum.length;
                double deltaFrequency = sampleRate / (double) nFFT;

                float barWidth = width / (float) OCTAVE_BANDS;
                float dcMagnitude = (float) (10 * Math.log10(Math.abs(spectrum[0])));

                //Log.i(TAG, String.format("bar width: %f", barWidth));
                //Log.i(TAG, String.format("canvas width: %d  canvas height: %d", canvas.getWidth(), canvas.getHeight()));
                //Log.i(TAG, String.format("measured width: %d  measured height: %d", width, heigth));

                Map<Double, RectF> magnitudeBars = new LinkedHashMap<>();
                // DC -> bin m[0]
                magnitudeBars.put(0d, new RectF(0, heigth - (heigth / dB_RANGE * dcMagnitude), barWidth, heigth - 40));

                double frequency = 0;
                int bin = 0;
                int countRect = 1;
                for (int i = 1; i < thirdOctaveFrequencyBoundaries.length - 1; i += 3) {
                    double upperBound = thirdOctaveFrequencyBoundaries[i + 1];
                    float meanMagnitude = 0;
                    int k = 0;
                    while ((frequency = bin * deltaFrequency) <= upperBound) {
                        meanMagnitude += Math.abs(spectrum[bin]);
                        bin++;
                        k++;
                    }
                    meanMagnitude /= k;
                    magnitudeBars.put(frequency, new RectF((countRect * barWidth) + 5,
                            heigth - (heigth / dB_RANGE * (float) (10 * Math.log10(meanMagnitude))),
                            (countRect * barWidth) + barWidth,
                            heigth - 40));
                    countRect++;
                }

                //Log.i(TAG, String.format("magnitude bars: %d", magnitudeBars.size()));

                int count = 0;
                for (Map.Entry<Double, RectF> entry : magnitudeBars.entrySet()) {
                    // Render frequency band
                    canvas.drawRect(entry.getValue(), paint);

                    // Render frequency label text
                    double freq = entry.getKey().doubleValue();
                    String frequencyLabel = null;
                    if (freq < REFERENCE_CENTER_FREQUENCY && count % 2 == 0) {
                        frequencyLabel = Long.toString((long) freq);
                    } else {
                        if (count % 2 == 0) {
                            frequencyLabel = getFormattedValue(freq / 1000);
                        }
                    }
                    count++;

                    if (frequencyLabel != null) {
                        canvas.drawText(frequencyLabel, entry.getValue().centerX(),
                                canvas.getHeight(), textPaint);
                    }
                }
            }
        }
    }

    private float[] getPowerSpectrum(@NonNull short[] samples) {
        return new PowerSpectrum(samples).getPowerSpectrum();
    }

    public double[] getCentreFrequencies() {
        double[] retVal = new double[centreFrequencies.length];
        System.arraycopy(centreFrequencies, 0, retVal, 0, centreFrequencies.length);
        return retVal;
    }

    public double[] getThirdOctaveFrequencyBands() {
        double[] retVal = new double[thirdOctaveFrequencyBoundaries.length];
        System.arraycopy(thirdOctaveFrequencyBoundaries, 0, retVal, 0, thirdOctaveFrequencyBoundaries.length);
        return retVal;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeigth(int heigth) {
        this.heigth = heigth;
    }

    private static String getFormattedValue(double value) {
        return DECIMAL_FORMAT.format(value);
    }

}
