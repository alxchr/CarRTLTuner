package ru.abch.carrtltuner;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kevalpatel2106.rulerpicker.RulerValuePicker;
import com.kevalpatel2106.rulerpicker.RulerValuePickerListener;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity implements IQSourceInterface.Callback, RFControlInterface {
    private int freq;
    final static int minFreq = 880, maxFreq = 1080;
    RulerValuePicker rulerValuePicker;
    String TAG = "MainActivity";
    TextView tv;
    static Resources mResources;
    static boolean run = false, mute;
    Button btnFreqDown, btnFreqUp, btnF1, btnF2, btnF3, btnF4, btnF5, btnF6, btnF7, btnF8;
    ImageButton btnOn, btnMute;
    View.OnClickListener freqClick;
    View.OnLongClickListener freqLongClick;
    SharedPreferences sp;
    public static final int RTL2832U_RESULT_CODE = 1234;	// arbitrary value, used when sending intent to RTL2832U
    private IQSourceInterface source = null;
    private Scheduler scheduler = null;
    private Demodulator demodulator = null;
    private int demodulationMode = Demodulator.DEMODULATION_WFM;
    private long fMult = 100000;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResources = getResources();
        tv = findViewById(R.id.freq);
        rulerValuePicker  = findViewById(R.id.ruler_picker);
        sp = getSharedPreferences();
        freq = sp.getInt("F",1000);
        mute = sp.getBoolean("mute", false);
        rulerValuePicker.setValuePickerListener(new RulerValuePickerListener() {
            @Override
            public void onValueChange(final int selectedValue) {
                //Value changed and the user stopped scrolling the ruler.
                //Application can consider this value as final selected value.
                Log.d(TAG,"Value change to "+ selectedValue);
                freq = selectedValue;
                if (freq > maxFreq) {
                    freq = maxFreq;
                    rulerValuePicker.selectValue(freq);
                }
                if (freq < minFreq) {
                    freq = minFreq;
                    rulerValuePicker.selectValue(freq);
                }
                tv.setText(showFreq(freq) + " " + mResources.getString(R.string.freq_unit));
                sp.edit().putInt("F",freq).apply();
                Log.d(TAG, "New freq = " + freq*fMult);
                updateSourceFrequency(freq*fMult);
                updateChannelFrequency(freq*fMult);
            }

            @Override
            public void onIntermediateValueChange(final int selectedValue) {
                //Value changed but the user is still scrolling the ruler.
                //This value is not final value. Application can utilize this value to display the current selected value.
                freq = selectedValue;
                tv.setText(showFreq(freq) + " " + mResources.getString(R.string.freq_unit));
            }
        });
        rulerValuePicker.selectValue(freq);
        tv.setText(showFreq(freq));
        btnFreqDown = findViewById(R.id.freq_down);
        btnFreqUp = findViewById(R.id.freq_up);
        btnFreqDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (freq > minFreq) {
                    rulerValuePicker.selectValue(--freq);
                    sp.edit().putInt("F",freq).apply();
                }
            }
        });
        btnFreqUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (freq < maxFreq) {
                    rulerValuePicker.selectValue(++freq);
                    sp.edit().putInt("F",freq).apply();
                }
            }
        });
        btnOn = findViewById(R.id.on_off);
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                run = !run;
                if (run) {
                    btnOn.setImageDrawable(mResources.getDrawable(R.mipmap.shutdown_104px));
                    createSource();
                    openSource();

                } else {
                    btnOn.setImageDrawable(mResources.getDrawable(R.mipmap.radio_tower_104px));
                    stopTuner();
                    /*
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setClassName("marto.rtl_tcp_andro", "com.sdrtouch.rtlsdr.DeviceOpenActivity");
                        intent.setData(Uri.parse("iqsrc://-x"));	// -x is invalid. will cause the driver to shut down (if running)
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "onDestroy: RTL2832U is not installed");
                    }

                     */
                    finish();
                }
            }
        });
        btnMute = findViewById(R.id.mute);
        if (mute) {
            btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.no_audio_104px));
        } else {
            btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.speaker_104px));
        }
        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mute = !mute;
                if (mute) {
                    btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.no_audio_104px));
                } else {
                    btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.speaker_104px));
                }
                sp.edit().putBoolean("mute", mute).apply();
            }
        });
        btnF1 = findViewById(R.id.f1);
        btnF2 = findViewById(R.id.f2);
        btnF3 = findViewById(R.id.f3);
        btnF4 = findViewById(R.id.f4);
        btnF5 = findViewById(R.id.f5);
        btnF6 = findViewById(R.id.f6);
        btnF7 = findViewById(R.id.f7);
        btnF8 = findViewById(R.id.f8);
        freqClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    freq = (int) view.getTag();
                    rulerValuePicker.selectValue(freq);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.toString());
                }
            }
        };
        freqLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String key;
                view.setTag(freq);
                ((Button)view).setText(showFreq(freq));
                switch (view.getId()) {
                    case R.id.f1:
                        key = "F1";
                        break;
                    case R.id.f2:
                        key = "F2";
                        break;
                    case R.id.f3:
                        key = "F3";
                        break;
                    case R.id.f4:
                        key = "F4";
                        break;
                    case R.id.f5:
                        key = "F5";
                        break;
                    case R.id.f6:
                        key = "F6";
                        break;
                    case R.id.f7:
                        key = "F7";
                        break;
                    case R.id.f8:
                        key = "F8";
                        break;
                    default:
                        key = "F1";
                        break;
                }
                sp.edit().putInt(key,freq).apply();
                return false;
            }
        };
        btnF1.setOnClickListener(freqClick);
        btnF2.setOnClickListener(freqClick);
        btnF3.setOnClickListener(freqClick);
        btnF4.setOnClickListener(freqClick);
        btnF5.setOnClickListener(freqClick);
        btnF6.setOnClickListener(freqClick);
        btnF7.setOnClickListener(freqClick);
        btnF8.setOnClickListener(freqClick);
        btnF1.setOnLongClickListener(freqLongClick);
        btnF2.setOnLongClickListener(freqLongClick);
        btnF3.setOnLongClickListener(freqLongClick);
        btnF4.setOnLongClickListener(freqLongClick);
        btnF5.setOnLongClickListener(freqLongClick);
        btnF6.setOnLongClickListener(freqLongClick);
        btnF7.setOnLongClickListener(freqLongClick);
        btnF8.setOnLongClickListener(freqLongClick);

        int freqBtn;
        freqBtn = sp.getInt("F1",890);
        btnF1.setTag(freqBtn);
        btnF1.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F2",910);
        btnF2.setTag(freqBtn);
        btnF2.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F3",930);
        btnF3.setTag(freqBtn);
        btnF3.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F4",960);
        btnF4.setTag(freqBtn);
        btnF4.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F5",1000);
        btnF5.setTag(freqBtn);
        btnF5.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F6",1020);
        btnF6.setTag(freqBtn);
        btnF6.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F7",1050);
        btnF7.setTag(freqBtn);
        btnF7.setText(showFreq(freqBtn));
        freqBtn = sp.getInt("F8",1060);
        btnF8.setTag(freqBtn);
        btnF8.setText(showFreq(freqBtn));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onBackPressed(){
        if (!run) {
            Log.d(TAG,"Finish application");
            super.onBackPressed();
            finish();
        } else {
            Log.d(TAG,"Go to launcher");
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
        }

    }
    SharedPreferences getSharedPreferences() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // close source
        if(source != null && source.isOpen())
            source.close();
        // shut down RTL2832U driver if running:
        if(run) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName("marto.rtl_tcp_andro", "com.sdrtouch.rtlsdr.DeviceOpenActivity");
                intent.setData(Uri.parse("iqsrc://-x"));	// -x is invalid. will cause the driver to shut down (if running)
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "onDestroy: RTL2832U is not installed");
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // err_info from RTL2832U:
        String[] rtlsdrErrInfo = {
                "permission_denied",
                "root_required",
                "no_devices_found",
                "unknown_error",
                "replug",
                "already_running"};

        switch (requestCode) {
            case RTL2832U_RESULT_CODE:
                // This happens if the RTL2832U driver was started.
                // We check for errors and print them:
                if (resultCode == RESULT_OK)
                {
                    Log.i(TAG, "onActivityResult: RTL2832U driver was successfully started.");
                }
                else {
                    int errorId = -1;
                    int exceptionCode = 0;
                    String detailedDescription = null;
                    if(data != null) {
                        errorId = data.getIntExtra("marto.rtl_tcp_andro.RtlTcpExceptionId", -1);
                        exceptionCode = data.getIntExtra("detailed_exception_code", 0);
                        detailedDescription = data.getStringExtra("detailed_exception_message");
                    }
                    String errorMsg = "ERROR NOT SPECIFIED";
                    if(errorId >= 0 && errorId < rtlsdrErrInfo.length)
                        errorMsg = rtlsdrErrInfo[errorId];

                    Log.e(TAG, "onActivityResult: RTL2832U driver returned with error: " + errorMsg + " ("+errorId+")"
                            + (detailedDescription != null ? ": " + detailedDescription + " (" + exceptionCode + ")" : ""));

                    if (source != null && source instanceof RtlsdrSource) {
                        Toast.makeText(MainActivity.this, "Error with Source [" + source.getName() + "]: " + errorMsg + " (" + errorId + ")"
                                + (detailedDescription != null ? ": " + detailedDescription + " (" + exceptionCode + ")" : ""), Toast.LENGTH_LONG).show();
                        source.close();
                    }
                }
                break;
        }
    }
    @Override
    public void onIQSourceReady(IQSourceInterface source) {	// is called after source.open()
        if (run) {
            startTuner();    // will start the processing loop, scheduler and source
            setDemodulationMode(Demodulator.DEMODULATION_WFM);
        }
    }

    @Override
    public void onIQSourceError(final IQSourceInterface source, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Error with Source [" + source.getName() + "]: " + message, Toast.LENGTH_LONG).show();
            }
        });
        stopTuner();

        if(this.source != null && this.source.isOpen())
            this.source.close();
    }
    public boolean updateDemodulationMode(int newDemodulationMode) {
        if(scheduler == null || demodulator == null || source == null) {
            Log.e(TAG,"updateDemodulationMode: scheduler/demodulator/source is null (no demodulation running)");
            return false;
        }
        setDemodulationMode(newDemodulationMode);
        return true;
    }
    @Override
    public boolean updateChannelWidth(int newChannelWidth) {
        if(demodulator != null) {
            if(demodulator.setChannelWidth(newChannelWidth)) {
//                analyzerSurface.setChannelWidth(newChannelWidth);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean updateChannelFrequency(long newChannelFrequency) {
        if(scheduler != null) {
            scheduler.setChannelFrequency(newChannelFrequency);
//            analyzerSurface.setChannelFrequency(newChannelFrequency);
            return true;
        }
        return false;
    }
    /**
     * Will set the modulation mode to the given value. Takes care of adjusting the
     * scheduler and the demodulator respectively and updates the action bar menu item.
     *
     * @param mode	Demodulator.DEMODULATION_OFF, *_AM, *_NFM, *_WFM
     */
    public void setDemodulationMode(int mode) {
        if(scheduler == null || demodulator == null || source == null) {
            Log.e(TAG,"setDemodulationMode: scheduler/demodulator/source is null");
            return;
        }

        // (de-)activate demodulation in the scheduler and set the sample rate accordingly:
        if(mode == Demodulator.DEMODULATION_OFF) {
            scheduler.setDemodulationActivated(false);
        }
        else {
            /*
            if(recordingFile != null && source.getSampleRate() != Demodulator.INPUT_RATE) {
                // We are recording at an incompatible sample rate right now.
                Log.i(TAG, "setDemodulationMode: Recording is running at " + source.getSampleRate() + " Sps. Can't start demodulation.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Recording is running at incompatible sample rate for demodulation!", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }


             */
            // adjust sample rate of the source:
            source.setSampleRate(Demodulator.INPUT_RATE);

            // Verify that the source supports the sample rate:
            if(source.getSampleRate() != Demodulator.INPUT_RATE) {
                Log.e(TAG,"setDemodulationMode: cannot adjust source sample rate!");
                Toast.makeText(MainActivity.this, "Source does not support the sample rate necessary for demodulation (" +
                        Demodulator.INPUT_RATE/1000000 + " Msps)", Toast.LENGTH_LONG).show();
                scheduler.setDemodulationActivated(false);
                mode = Demodulator.DEMODULATION_OFF;	// deactivate demodulation...
            } else {
                scheduler.setDemodulationActivated(true);
            }
        }

        // set demodulation mode in demodulator:
        demodulator.setDemodulationMode(mode);
        this.demodulationMode = mode;	// save the setting

        // disable/enable demodulation view in surface:
        if(mode == Demodulator.DEMODULATION_OFF) {
//           analyzerSurface.setDemodulationEnabled(false);
        } else {
//            analyzerSurface.setDemodulationEnabled(true);	// will re-adjust channel freq, width and squelch,
            // if they are outside the current viewport and update the
            // demodulator via callbacks.
//            analyzerSurface.setShowLowerBand(mode != Demodulator.DEMODULATION_USB);		// show lower side band if not USB
//            analyzerSurface.setShowUpperBand(mode != Demodulator.DEMODULATION_LSB);		// show upper side band if not LSB
        }

        // update action bar:
//        updateActionBar();
    }
    public boolean updateSourceFrequency(long newSourceFrequency) {
        if(source != null 	&& newSourceFrequency <= source.getMaxFrequency()
                && newSourceFrequency >= source.getMinFrequency()) {
            source.setFrequency(newSourceFrequency);
//            analyzerSurface.setVirtualFrequency(newSourceFrequency);

            String freq = String.format("%.1f MHz", source.getFrequency()/1000000f);
//            txt_freq.setText(freq);

            return true;
        }
        return false;
    }

    public boolean updateSampleRate(int newSampleRate) {
        if(source != null) {
            if(scheduler == null || !scheduler.isRecording()) {
                source.setSampleRate(newSampleRate);
                return true;
            }
        }
        return false;
    }
    @Override
    public void updateSquelch(float newSquelch) {
//        analyzerSurface.setSquelch(newSquelch);
    }

    @Override
    public boolean updateSquelchSatisfied(boolean squelchSatisfied) {
        if(scheduler != null) {
            scheduler.setSquelchSatisfied(squelchSatisfied);
            return true;
        }
        return false;
    }

    @Override
    public int requestCurrentChannelWidth() {
        if(demodulator != null)
            return demodulator.getChannelWidth();
        else
            return -1;
    }

    public long requestCurrentChannelFrequency() {
        if(scheduler != null)
            return scheduler.getChannelFrequency();
        else
            return -1;
    }

    public int requestCurrentDemodulationMode() {
        return demodulationMode;
    }

    public float requestCurrentSquelch() {
//        if(analyzerSurface != null)
//            return analyzerSurface.getSquelch();
//        else
            return Float.NaN;
    }

    public long requestCurrentSourceFrequency() {
        if(source != null)
            return source.getFrequency();
        else
            return -1;
    }

    public int requestCurrentSampleRate() {
        if(source != null)
            return source.getSampleRate();
        else
            return -1;
    }

    public long requestMaxSourceFrequency() {
        if(source != null)
            return source.getMaxFrequency();
        else
            return -1;
    }

    public int[] requestSupportedSampleRates() {
        if(source != null)
            return source.getSupportedSampleRates();
        else
            return null;
    }
    /**
     * Will create a IQ Source instance according to the user settings.
     *
     * @return true on success; false on error
     */
    public boolean createSource() {
        long frequency;
        int sampleRate;
                // Create RtlsdrSource
        source = new RtlsdrSource("127.0.0.1", 1234);
        frequency = freq * fMult;
        sampleRate =  source.getMaxSampleRate();
        if(sampleRate > 2000000)	// might be the case after switching over from HackRF
                sampleRate = 2000000;
        source.setFrequency(frequency);
        source.setSampleRate(sampleRate);
        ((RtlsdrSource) source).setFrequencyCorrection(0);
        ((RtlsdrSource)source).setFrequencyOffset(0);
        ((RtlsdrSource)source).setManualGain( false);
        ((RtlsdrSource)source).setAutomaticGainControl(false);
        if(((RtlsdrSource)source).isManualGain()) {
            ((RtlsdrSource) source).setGain(0);
            ((RtlsdrSource) source).setIFGain(0);
        }
        // inform the analyzer surface about the new source
//        analyzerSurface.setSource(source);
        return true;
    }

    /**
     * Will open the IQ Source instance.
     * Note: some sources need special treatment on opening, like the rtl-sdr source.
     *
     * @return true on success; false on error
     */
    public boolean openSource() {

                if (source != null && source instanceof RtlsdrSource) {
                    // We might need to start the driver:

                        // start local rtl_tcp instance:
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setClassName("marto.rtl_tcp_andro", "com.sdrtouch.rtlsdr.DeviceOpenActivity");
                            intent.setData(Uri.parse("iqsrc://-a 127.0.0.1 -p 1234 -n 1"));
                            startActivityForResult(intent, RTL2832U_RESULT_CODE);
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "createSource: RTL2832U is not installed");

                            // Show a dialog that links to the play market:
                            new AlertDialog.Builder(this)
                                    .setTitle("RTL2832U driver not installed!")
                                    .setMessage("You need to install the (free) RTL2832U driver to use RTL-SDR dongles.")
                                    .setPositiveButton("Install from Google Play", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=marto.rtl_tcp_andro"));
                                            startActivity(marketIntent);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            // do nothing
                                        }
                                    })
                                    .show();
                            return false;
                        }


                    return source.open(this, this);
                } else {
                    Log.e(TAG, "openSource: sourceType is RTLSDR_SOURCE, but source is null or of other type.");
                    return false;
                }

    }

    /**
     * Will stop the RF Analyzer. This includes shutting down the scheduler (which turns of the
     * source), the processing loop and the demodulator if running.
     */
    public void stopTuner() {
        // Stop the Scheduler if running:
        if(scheduler != null) {
            // Stop recording in case it is running:
//            stopRecording();
            scheduler.stopScheduler();
        }

        // Stop the Processing Loop if running:
        /*
        if(analyzerProcessingLoop != null)
            analyzerProcessingLoop.stopLoop();


         */
        // Stop the Demodulator if running:
        if(demodulator != null)
            demodulator.stopDemodulator();

        // Wait for the scheduler to stop:
        if(scheduler != null && !scheduler.getName().equals(Thread.currentThread().getName())) {
            try {
                scheduler.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "startAnalyzer: Error while stopping Scheduler.");
            }
        }

        // Wait for the processing loop to stop
        /*
        if(analyzerProcessingLoop != null) {
            try {
                analyzerProcessingLoop.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "startAnalyzer: Error while stopping Processing Loop.");
            }
        }

         */

        // Wait for the demodulator to stop
        if(demodulator != null) {
            try {
                demodulator.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "startAnalyzer: Error while stopping Demodulator.");
            }
        }

        run = false;

        // update action bar icons and titles:
//        updateActionBar();

        // allow screen to turn off again:
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    /**
     * Will start the RF Analyzer. This includes creating a source (if null), open a source
     * (if not open), starting the scheduler (which starts the source) and starting the
     * processing loop.
     */
    public void startTuner() {
        this.stopTuner();	// Stop if running; This assures that we don't end up with multiple instances of the thread loops

        // Retrieve fft size and frame rate from the preferences
        int fftSize = 1024;
        int frameRate = 1;
        boolean dynamicFrameRate = true;

        run = true;

        if(source == null) {
            if(!this.createSource())
                return;
        }

        // check if the source is open. if not, open it!
        if(!source.isOpen()) {
            if (!openSource()) {
                Toast.makeText(MainActivity.this, "Source not available (" + source.getName() + ")", Toast.LENGTH_LONG).show();
                run = false;
                return;
            }
            return;	// we have to wait for the source to become ready... onIQSourceReady() will call startAnalyzer() again...
        }

        // Create a new instance of Scheduler and Processing Loop:
        scheduler = new Scheduler(fftSize, source);
        if (scheduler == null) {
            Log.e(TAG, "Scheduler is null");
        } else {
            Log.d(TAG,"Scheduler OK");
        }
        /*
        analyzerProcessingLoop = new AnalyzerProcessingLoop(
                analyzerSurface, 			// Reference to the Analyzer Surface
                fftSize,					// FFT size
                scheduler.getFftOutputQueue(), // Reference to the input queue for the processing loop
                scheduler.getFftInputQueue()); // Reference to the buffer-pool-return queue
        if(dynamicFrameRate)
            analyzerProcessingLoop.setDynamicFrameRate(true);
        else {
            analyzerProcessingLoop.setDynamicFrameRate(false);
            analyzerProcessingLoop.setFrameRate(frameRate);
        }

         */
        // Start both threads:
        scheduler.start();
//        analyzerProcessingLoop.start();

        scheduler.setChannelFrequency(freq*fMult);

        // Start the demodulator thread:
        demodulator = new Demodulator(scheduler.getDemodOutputQueue(), scheduler.getDemodInputQueue(), source.getPacketSize());
        demodulator.start();

        // Set the demodulation mode (will configure the demodulator correctly)
        this.setDemodulationMode(demodulationMode);

        // update the action bar icons and titles:
//        updateActionBar();

        // Prevent the screen from turning off:
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    static String showFreq(int f) {
        return format("%.1f", f/10f);
    }
}
