package ru.abch.carrtltuner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.kevalpatel2106.rulerpicker.RulerValuePicker;
import com.kevalpatel2106.rulerpicker.RulerValuePickerListener;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity {
    static int freq = 1000;
    final static int minFreq = 880, maxFreq = 1080;
    RulerValuePicker rulerValuePicker;
    String TAG = "MainActivity";
    TextView tv;
    static Resources mResources;
    static boolean run = false, mute = false;
    Button btnFreqDown, btnFreqUp, btnF1, btnF2, btnF3, btnF4, btnF5, btnF6, btnF7, btnF8;
    ImageButton btnOn, btnMute;
    View.OnClickListener freqClick;
    View.OnLongClickListener freqLongClick;
    SharedPreferences sp;
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
        rulerValuePicker.selectValue(freq);
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
            }

            @Override
            public void onIntermediateValueChange(final int selectedValue) {
                //Value changed but the user is still scrolling the ruler.
                //This value is not final value. Application can utilize this value to display the current selected value.
                freq = selectedValue;
                tv.setText(showFreq(freq) + " " + mResources.getString(R.string.freq_unit));
            }
        });
        tv.setText(showFreq(freq));
        btnFreqDown = findViewById(R.id.freq_down);
        btnFreqUp = findViewById(R.id.freq_up);
        btnFreqDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (freq > minFreq) {
                    rulerValuePicker.selectValue(--freq);
                }
            }
        });
        btnFreqUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (freq < maxFreq) {
                    rulerValuePicker.selectValue(++freq);
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
                } else {
                    btnOn.setImageDrawable(mResources.getDrawable(R.mipmap.radio_tower_104px));
                }
            }
        });
        btnMute = findViewById(R.id.mute);
        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mute = !mute;
                if (mute) {
                    btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.no_audio_104px));
                } else {
                    btnMute.setImageDrawable(mResources.getDrawable(R.mipmap.speaker_104px));
                }
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
        sp = getSharedPreferences();
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
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    static String showFreq(int f) {
        return format("%.1f", f/10f);
    }
}
