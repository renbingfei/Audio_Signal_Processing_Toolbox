package ch.zhaw.bait17.audio_signal_processing_toolbox.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import ch.zhaw.bait17.audio_signal_processing_toolbox.ApplicationContext;
import ch.zhaw.bait17.audio_signal_processing_toolbox.Constants;
import ch.zhaw.bait17.audio_signal_processing_toolbox.dsp.AudioEffect;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.PostFilterSampleBlock;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.PreFilterSampleBlock;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.Track;
import ch.zhaw.bait17.audio_signal_processing_toolbox.util.PCMUtil;
import ch.zhaw.bait17.audio_signal_processing_toolbox.util.Util;

/**
 * <p>
 * A versatile yet easy to use player facade.
 * It hides the complexity of decoding the audio source, filtering and feeding the PCM samples
 * to the audio sink and controlling the audio playback.
 * </p>
 *
 * @author georgrem, stockan1
 */
public final class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();
    private static final AudioPlayer INSTANCE = new AudioPlayer();
    private static final int BUFFER_LENGTH_PER_CHANNEL_IN_SECONDS = 3;

    private static short[] decodedSamples;
    private static AudioDecoder decoder;
    private static AudioTrack audioTrack;
    private static List<AudioEffect> audioEffects;
    private static EventBus eventBus;
    private PlaybackListener listener;
    private Track currentTrack;
    private volatile PlayState playState = PlayState.STOP;
    private volatile boolean keepPlaying = false;
    private volatile boolean paused = false;
    private int sampleRate;
    private int channels;
    private boolean sampleRateHasChanged = false;
    private boolean channelsHasChanged = false;

    private enum PlayState {
        PLAY, STOP, PAUSE;
    }

    private AudioPlayer() {
        sampleRate = Constants.DEFAULT_SAMPLE_RATE;
        channels = Constants.DEFAULT_CHANNELS;
        buildEventBus();
    }

    /**
     * Returns the singleton instance of the PlayerPresenter.
     *
     * @return
     */
    public static AudioPlayer getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the {@code PlaybackListener}.
     *
     * @param listener
     */
    public void setOnPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    /**
     * Selects the {@code Track} to be played.
     *
     * @param track
     */
    public void selectTrack(@NonNull Track track) {
        currentTrack = track;
    }

    /**
     * Plays back the currently selected {@code Track}.
     * A {@code Track} must be select first {@link #selectTrack(Track)}.
     * Changing the {@code AudioTrack} buffer size on the fly requires API Level 24.
     * Therefore if the sample rate or the channel count has changed, a new {@code AudioTrack}
     * must be created.
     *
     */
    public void play() {
        if (!isPaused() && !isPlaying() && currentTrack != null) {
            initialiseDecoder(currentTrack.getUri());
            if (isDecoderInitialised()
                    && (!isAudioTrackInitialised() || sampleRateHasChanged || channelsHasChanged)) {
                audioTrack = null;
                createAudioTrack();
            }
            startPlayback();
        }
    }

    /**
     * Pauses the audio playback.
     */
    public void pausePlayback() {
        if (isAudioTrackInitialised()) {
            if (isPlaying()) {
                Log.d(TAG, "Pause playback");
                audioTrack.pause();
                paused = true;
                keepPlaying = true;
            }
        }
    }

    /**
     * Resumes the audio playback.
     */
    public void resumePlayback() {
        if (isAudioTrackInitialised()) {
            if (isPaused()) {
                Log.d(TAG, "Resume playback");
                audioTrack.play();
                paused = false;
                keepPlaying = true;
            }
        }
    }

    /**
     * Stops the audio playback.
     */
    public void stopPlayback() {
        Log.d(TAG, "Stop playback.");
        keepPlaying = false;
    }

    /**
     * Positions the playback head to the new position.
     *
     * @param msec
     */
    public void seekToPosition(int msec) {

    }

    /**
     * Returns true if the player is running.
     * Do not use {@code audioTrack.getPlayState()}.
     *
     * @return
     */
    public boolean isPlaying() {
        if (!isAudioTrackInitialised()) {
            Log.d(TAG, String.format("isPlaying ? --> AudioTrack is null"));
        } else {
            Log.d(TAG, String.format("isPlaying ? --> %s",
                    audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING));
        }
        return playState == PlayState.PLAY;
    }

    /**
     * Returns true if the player is stopped.
     * Do not use {@code audioTrack.getPlayState()}.
     *
     * @return
     */
    public boolean isStopped() {
        if (!isAudioTrackInitialised()) {
            Log.d(TAG, String.format("isStopped ? --> AudioTrack is null"));
        } else {
            Log.d(TAG, String.format("isStopped ? --> %s",
                    audioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED));
        }
        return playState == PlayState.STOP;
    }

    /**
     * Returns true if the player is paused.
     * Do not use {@code audioTrack.getPlayState()}.
     *
     * @return
     */
    public boolean isPaused() {
        if (!isAudioTrackInitialised()) {
            Log.d(TAG, String.format("isPaused ? --> AudioTrack is null"));
        } else {
            Log.d(TAG, String.format("isPaused ? --> %s",
                    audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED));
        }
        return playState == PlayState.PAUSE;
    }

    /**
     * Returns the sample rate of the currently playing track.
     *
     * @return
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of channels of the currently playing track.
     *
     * @return
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Returns the current playback position expressed in frames.
     *
     * @return {@code AudioPlayer} playback position
     */
    public int getPlaybackPosition() {
        return isAudioTrackInitialised() ? audioTrack.getPlaybackHeadPosition() : 0;
    }

    /**
     * Sets the {@code AudioEffect}s
     *
     * @param audioEffects list of {@code AudioEffect}s
     */
    public void setAudioEffects(List<AudioEffect> audioEffects) {
        this.audioEffects = audioEffects;
    }

    /**
     * Initialises the decoder.
     *
     * @param uri
     */
    private void initialiseDecoder(@NonNull final String uri) {
        try {
            InputStream is = Util.getInputStreamFromURI(uri);
            if (uri.endsWith(".mp3")) {
                decoder = MP3Decoder.getInstance();
            } else if (uri.endsWith(".wav")) {
                decoder = WaveDecoder.getInstance();
            }
            if (decoder != null) {
                decoder.setSource(is);
                int newSampleRate = decoder.getSampleRate();
                if (newSampleRate != sampleRate) {
                    sampleRateHasChanged = true;
                    sampleRate = newSampleRate;
                } else {
                    sampleRateHasChanged = false;
                }
                int newChannels = decoder.getChannels();
                if (newChannels != channels) {
                    channelsHasChanged = true;
                    channels = newChannels;
                } else {
                    channelsHasChanged = false;
                }
            } else {
                throw new DecoderException("Unsupported audio format.");
            }
        } catch (FileNotFoundException | DecoderException e) {
            Toast.makeText(ApplicationContext.getAppContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts the audio playback.
     */
    private void startPlayback() {
        // Sometimes AudioTrack initialisation fails - we need to check if AudioTrack is ready.
        if (isAudioTrackInitialised() && isDecoderInitialised()) {
            keepPlaying = true;
            paused = false;
            audioTrack.play();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Playback thread '" + Thread.currentThread().getName() + "' start");
                    Log.d(TAG, "Playback start");
                    listener.onStartPlayback();
                    while (keepPlaying) {
                        if (paused) {
                            playState = PlayState.PAUSE;
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Interrupted while paused.");
                            }
                        } else {
                            playState = PlayState.PLAY;
                            decodedSamples = decoder.getNextSampleBlock();
                            if (decodedSamples != null) {
                                float[] filteredSamples = PCMUtil.short2FloatArray(decodedSamples);
                                if (audioEffects != null) {
                                    applyAudioEffect(PCMUtil.short2FloatArray(decodedSamples), filteredSamples);
                                }
                                if (audioTrack.write(PCMUtil.float2ShortArray(filteredSamples),
                                        0, filteredSamples.length) < filteredSamples.length) {
                                    Log.d(TAG, "Dropped samples.");
                                }
                                // Broadcast pre applyAudioEffect sample block using event bus
                                eventBus.post(new PreFilterSampleBlock(decodedSamples, sampleRate));
                                // Broadcast post applyAudioEffect sample block using event bus
                                eventBus.post(new PostFilterSampleBlock(
                                        PCMUtil.float2ShortArray(filteredSamples), sampleRate));
                            } else {
                                // No more frames to decode, we reached the end of the InputStream.
                                // --> quit
                                keepPlaying = false;
                            }
                        }
                    }
                    Log.d(TAG, "Finished decoding");
                    // Wait some time and let AudioTrack output the frames in its buffer.
                    long then = System.nanoTime();
                    while (System.nanoTime() < then + 1e9) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Interrupted while waiting that playback finishes.");
                        }
                    }
                    listener.onCompletion();
                    audioTrack.pause();
                    audioTrack.stop();
                    audioTrack.flush();
                    playState = PlayState.STOP;
                    Log.d(TAG, "AudioTrack pause/stop/flush.");
                    Log.d(TAG, "Playback stop");
                    Log.d(TAG, "Playback thread '" + Thread.currentThread().getName() + "' stop");
                }
            }).start();
        } else {
            // Some error occurred, AudioPlayer is unable to play source.
            Toast.makeText(ApplicationContext.getAppContext(), "Unsupported audio format.",
                    Toast.LENGTH_SHORT).show();
            playState = PlayState.STOP;
            listener.onCompletion();
        }
    }

    /**
     * Applies the {@code AudioEffect}(s) to the supplied audio samples block.
     * Input and output arrays must have the same length.
     *
     * @param input     an array of {@code float}
     * @param output    an array of {@code float}
     */
    private void applyAudioEffect(@NonNull float[] input, @NonNull float[] output) {
        if (audioEffects != null && input.length == output.length) {
            for (AudioEffect fx : audioEffects) {
                if (fx != null) {
                    fx.apply(input, output);
                    input = output;
                }
            }
        }
    }

    /**
     * Creates an instance of {@code AudioTrack}.
     */
    private void createAudioTrack() {
        int optimalBufferSize = getOptimalBufferSize();
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize < optimalBufferSize) {
            bufferSize = optimalBufferSize;
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "AudioTrack created and initialised.");
        }
    }

    /**
     * Computes and returns the optimal buffers size for the {@code AudioTrack} object.
     *
     * @return
     */
    private int getOptimalBufferSize() {
        return sampleRate * channels * BUFFER_LENGTH_PER_CHANNEL_IN_SECONDS;
    }

    /**
     * Returns true if {@code AudioTrack} object is ready.
     *
     * @return
     */
    private boolean isAudioTrackInitialised() {
        return audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
    }

    /**
     * Returns true if the {@code AudioDecoder} is ready.
     *
     * @return
     */
    private boolean isDecoderInitialised() {
        return decoder != null && decoder.isInitialised();
    }

    /**
     * Initialises the EventBus.
     * EventBus is used to send sample blocks to different views.
     */
    private void buildEventBus() {
        eventBus = EventBus.builder()
                .logNoSubscriberMessages(false)
                .installDefaultEventBus();
    }

}
