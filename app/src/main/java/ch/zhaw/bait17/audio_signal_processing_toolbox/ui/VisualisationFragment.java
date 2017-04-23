package ch.zhaw.bait17.audio_signal_processing_toolbox.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import ch.zhaw.bait17.audio_signal_processing_toolbox.Constants;
import ch.zhaw.bait17.audio_signal_processing_toolbox.FFT;
import ch.zhaw.bait17.audio_signal_processing_toolbox.R;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.PCMSampleBlock;
import ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation.AudioView;
import ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation.FrequencyView;
import ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation.SpectrogramView;
import ch.zhaw.bait17.audio_signal_processing_toolbox.visualisation.TimeView;

/**
 * @author georgrem, stockan1
 */
public class VisualisationFragment extends Fragment {

    private static final String TAG = VisualisationFragment.class.getSimpleName();
    private static final String BUNDLE_ARGUMENT_AUDIOVIEWS =
            VisualisationFragment.class.getSimpleName() + ".AUDIOVIEWS";

    private FFT fft;
    private int fftResolution = Constants.DEFAULT_FFT_RESOLUTION;
    private List<AudioView> views;

    // Creates a new fragment given a array
    // VisualisationFragment.newInstance(views);
    public static VisualisationFragment newInstance(List<AudioView> views) {
        VisualisationFragment fragment = new VisualisationFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable(BUNDLE_ARGUMENT_AUDIOVIEWS, (ArrayList<AudioView>) views);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get back arguments
        Bundle arguments = this.getArguments();
        if (arguments.getSerializable(BUNDLE_ARGUMENT_AUDIOVIEWS) != null)
            views = (List<AudioView>) arguments.getSerializable(BUNDLE_ARGUMENT_AUDIOVIEWS);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.content_visualisation, container, false);

        // we have to wait for the drawing phase for the actual measurements
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                if (views != null && views.size() > 0) {
                    LinearLayout linearLayout = (LinearLayout) rootView.findViewById(R.id.content_visualisation);
                    int viewWidth = linearLayout.getWidth();
                    int viewHeight = linearLayout.getHeight() / views.size();
                    int margin = (int) (0.015 * viewHeight);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            viewWidth - 2 * margin, viewHeight - 2 * margin);
                    layoutParams.setMargins(margin, margin, margin, margin);

                    for (AudioView view : views) {
                        // replace identical instances
                        if (view.getParent() != null)
                            ((ViewGroup) view.getParent()).removeView(view);
                        linearLayout.addView(view, layoutParams);
                    }
                }

            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //TextView frequency = (TextView) spectrogramView.findViewById(R.id.textview_spectrogram_header);
        //frequency.setText("FFT size: " + fftResolution);
    }


    @Override
    public void onStart() {
        super.onStart();
        // Register to EventBus
        EventBus.getDefault().register(this);

        fft = new FFT(fftResolution);

        if (views != null) {
            for (AudioView view : views) {
                if (view instanceof SpectrogramView) {
                    ((SpectrogramView) view).setFFTResolution(fftResolution);
                }
            }
        }
    }

    @Override
    public void onStop() {
        // Unregister from EventBus
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /**
     * EventBus subscriber - receives {@code PCMSampleBlock}s from the publisher
     * {@link ch.zhaw.bait17.audio_signal_processing_toolbox.player.AudioPlayer}
     *
     * @param sampleBlock   a {@code PCMSampleBlock}
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPCMSampleBlockReceived(PCMSampleBlock sampleBlock) {
        if (sampleBlock != null && views != null) {
            for (AudioView view : views) {
                if ((view.isPreFilterView() && sampleBlock.isPreFilterSamples())
                        || (!view.isPreFilterView() && !sampleBlock.isPreFilterSamples())){
                    setViewParameters(view, sampleBlock);
                }
            }
        }
    }

    /**
     * Sets the views to be displayed in the fragment.
     *
     * @param views     a list of views
     */
    public void setViews(List<AudioView> views) {
        this.views = views;
    }

    /**
     * Sets the view parameters: </br>
     * <ul>
     *     <li>sample rate</li>
     *     <li>channels</li>
     *     <li>PCM samples or magnitude data</li>
     * </ul>
     *
     * @param view          a view
     * @param sampleBlock   a {@code PCMSampleBlock}
     */
    private void setViewParameters(AudioView view, PCMSampleBlock sampleBlock) {
        view.setSampleRate(sampleBlock.getSampleRate());
        view.setChannels(sampleBlock.getSampleRate());

        if (view instanceof FrequencyView) {
            ((FrequencyView) view).setSpectralDensity(fft.getPowerSpectrum(sampleBlock.getSamples(),
                    sampleBlock.getChannels()));
        }

        if (view instanceof TimeView) {
            ((TimeView) view).setSamples(sampleBlock.getSamples());
        }
    }

}
