package ch.zhaw.bait17.audio_signal_processing_toolbox.util;

/**
 * @author georgrem, stockan1
 */

public class Constants {

    // Decoder and audio player
    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_CHANNELS = 2;
    public static final int DEFAULT_FFT_RESOLUTION = 4096;

    // FIR Filter
    public static final String FREQUENCY_PASS_1 = "fpass1";
    public static final String FREQUENCY_PASS_2 = "fpass2";
    public static final String FREQUENCY_STOP_1 = "fstop1";
    public static final String FREQUENCY_STOP_2 = "fstop2";
    public static final String AMOUNT_RIPPLE_PASS_1 = "Apass1";
    public static final String AMOUNT_RIPPLE_PASS_2 = "Apass2";
    public static final String ATTENUATION_STOP_1 = "Astop1";
    public static final String ATTENUATION_STOP_2 = "Astop2";

    // FIR comb filter
    public static final float FIR_COMB_FILTER_MAX_DELAY = 0.1f;
    public static final float FIR_COMB_FILTER_DEFAULT_DELAY = 0.005f;

    // Bitcrusher
    public static final float BITCRUSHER_MIN_NORM_FREQ = 0;
    public static final float BITCRUSHER_MAX_NORM_FREQ = 1;
    public static final int BITCRUSHER_MIN_BIT_DEPTH = 1;
    public static final int BITCRUSHER_MAX_BIT_DEPTH = 16;
    public static final float BITCRUSHER_DEFAULT_NORM_FREQUENCY = 0.1f;
    public static final int BITCRUSHER_DEFAULT_BITS = 8;

    //  Waveshaper
    public static final int WAVESHAPER_DEFAULT_THRESHOLD = 5;
    public static final int WAVESHAPER_MIN_THRESHOLD = 1;
    public static final int WAVESHAPER_MAX_THRESHOLD = 25;

    // Soft clipper
    public static final int SOFT_CLIPPER_MIN_CLIPPING_FACTOR = 1;
    public static final int SOFT_CLIPPER_MAX_CLIPPING_FACTOR = 100;
    public static final float SOFT_CLIPPER_DEFAULT_CLIPPING_FACTOR = 20f;

    // Tube distortion
    public static final float TUBE_DISTORTION_MIN_GAIN = 0;
    public static final float TUBE_DISTORTION_MAX_GAIN = 10;
    public static final float TUBE_DISTORTION_DEFAULT_GAIN = 1.5f;
    public static final float TUBE_DISTORTION_MAX_MIX = 1;
    public static final float TUBE_DISTORTION_DEFAULT_MIX = 0.5f;

    // Ring modulator
    public static final int RING_MODULATOR_MAX_MOD_FREQUENCY = 800;
    public static final int RING_MODULATOR_DEFAULT_FREQUENCY = 50;

    // Tremolo
    public static final float TREMOLO_MAX_MOD_FREQUENCY = 20.0f;
    public static final float TREMOLO_DEFAULT_MOD_FREQUENCY = 5.0f;
    public static final float TREMOLO_MAX_AMPLITUDE = 1.0f;
    public static final float TREMOLO_DEFAULT_AMPLITUDE = 0.5f;

    // Flanger
    public static final float FLANGER_MAX_RATE = 1.0f;
    public static final float FLANGER_DEFAULT_RATE = 0.5f;
    public static final float FLANGER_MAX_AMPLITUDE = 1;
    public static final float FLANGER_DEFAULT_AMPLITUDE = 0.7f;
    public static final float FLANGER_MAX_DELAY = 0.015f;
    public static final float FLANGER_DEFAULT_DELAY = 0.003f;

    // Linear gain
    public static final float GAIN_DEFAULT = 1.0f;
    public static final float GAIN_MAX = 2.0f;
}
