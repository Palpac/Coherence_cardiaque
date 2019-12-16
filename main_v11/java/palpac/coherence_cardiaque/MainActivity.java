package palpac.coherence_cardiaque;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Button buttonplay;
    TextView TextV1, TextV2, TextV3, TextV4, TextV5;
    LinearLayout linear_fond;
    Toolbar toolbar;
    Menu menu;
    Boolean autostart, timer_started = false, saved_playing_state, dark_theme;
    int INTENT_RETURN_ID; // Used by intent to start Settings activity
    int timer_time;
    String sound, saved_sound;
    String FORMAT = "%02d:%02d"; // Timer display format
    CountDownTimer CountDownTimer = null;

    LocalBroadcastManager localBroadcastManager; // Secured receiver by been local (app only) ...
    BroadcastReceiver broadcastReceiver;
    IntentFilter broadcast_intentfilter; // ... receive only filtered broadcasts

    AudioPlayerService mService;    // Service
    boolean mBound = false;         // Used to know if Service is bound



    //////////////////////////////////////////////////////////////////////////////////////////////// LIFE CYCLE & SERVICE
    @Override
    protected void onStart() {
        //Toast.makeText(getApplicationContext(),  "onStart MainActivity", Toast.LENGTH_SHORT).show();
        super.onStart();
        Intent start_intent = new Intent(this, AudioPlayerService.class); // Create intent to Service
        ContextCompat.startForegroundService(this, start_intent);
        bindService(start_intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onStop() {
        super.onStop();
        //Toast.makeText(getApplicationContext(),  "onStop MainActivity", Toast.LENGTH_SHORT).show();
        unbindService(mConnection);                                   // UnBind Service when app stop
        mBound = false;
    }
    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(getApplicationContext(), "onResume MainActivity", Toast.LENGTH_SHORT).show();
        localBroadcastManager.registerReceiver(broadcastReceiver, broadcast_intentfilter); // To receive playback changes again (update button text)
    }
    @Override
    protected void onPause() {
        //Toast.makeText(getApplicationContext(), "onPause MainActivity", Toast.LENGTH_SHORT).show();
        localBroadcastManager.unregisterReceiver(broadcastReceiver); // Don't need (button invisible)
        super.onPause();
    }
    @Override
    protected void onDestroy() { // when user end app or system destroy for memory use.
        //Toast.makeText(getApplicationContext(), "onDestroy MainActivity", Toast.LENGTH_SHORT).show();
        localBroadcastManager.unregisterReceiver(broadcastReceiver); // Unregister playback change
        super.onDestroy();
    }
    private ServiceConnection mConnection = new ServiceConnection() { // Needed Callbacks for service binding, passed to bindService()
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            //Toast.makeText(getApplicationContext(),  "onServiceConnected MainActivity", Toast.LENGTH_SHORT).show();
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //Toast.makeText(getApplicationContext(),  "onServiceDisconnected MainActivity", Toast.LENGTH_SHORT).show();
            mBound = false;
        }
    };



    //////////////////////////////////////////////////////////////////////////////////////////////// ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Locale locale = new Locale("fr");// Used for localization (translated strings)            Localization
        //Configuration config = getBaseContext().getResources().getConfiguration();
        //config.locale = locale;
        //getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linear_fond = findViewById(R.id.linear_fond);
        TextV1 = findViewById(R.id.text1);
        TextV2 = findViewById(R.id.text2);
        TextV3 = findViewById(R.id.text3);
        TextV4 = findViewById(R.id.text4);
        TextV5 = findViewById(R.id.text5);

        toolbar = findViewById(R.id.toolbar); // handle toolbar from id
        setSupportActionBar(toolbar); // Set toolbar as an actionbar
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide app name in toolbar

        buttonplay = findViewById(R.id.buttonplay);
        buttonplay.setOnClickListener(buttonplayListener);
        buttonplay.setOnTouchListener(buttonplayTouchListener);


        //////////////////////////////////////////////////////////////////////////////////////////// LOCAL BROADCAST RECEIVER
        broadcast_intentfilter = new IntentFilter();                                    // To receive player playback changes
        broadcast_intentfilter.addAction("Coherence_cardiaque.broadcast.playingstate"); // Name to filter intent with
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                if (intent != null){
                    //Toast.makeText(context, intent.getStringExtra("playbackState").toString(), Toast.LENGTH_LONG).show();
                    if (intent.getStringExtra("playbackState").equals("True")) {              // PLAY
                        //Toast.makeText(context, "Play", Toast.LENGTH_LONG).show();
                        if (!timer_started) {           // If button does not indicate time
                            buttonplay.setText(R.string.stop); // Update button text
                        }
                    }
                    if (intent.getStringExtra("playbackState").equals("False")) {             // STOP
                        //Toast.makeText(context, "Pause", Toast.LENGTH_LONG).show();
                        buttonplay.setText(R.string.play);
                        if(timer_started) {
                            CountDownTimer.cancel();
                            timer_started = false;
                        }
                    }
                    if (intent.getStringExtra("playbackState").equals("Cancel")) {            // STOP
                        finish(); // Quit app
                    }
                }
            }
        };

        maj_preferences(); // Update user preferences

    } // onCreate end


    //////////////////////////////////////////////////////////////////////////////////////////////// PLAY / STOP Listeners
    View.OnTouchListener buttonplayTouchListener = new View.OnTouchListener() {   // PLAY Touched
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) { // Button released
                //Toast.makeText(getApplicationContext(),  "Up", Toast.LENGTH_SHORT).show();
                if (dark_theme) {
                    buttonplay.setBackground(getResources().getDrawable(R.drawable.button_black2));
                }
                else {
                    buttonplay.setShadowLayer(0,0,0, R.color.transparent);
                    buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2));
                }
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) { // Button pressed
                //Toast.makeText(getApplicationContext(),  "Down", Toast.LENGTH_SHORT).show();
                if (dark_theme) {
                    buttonplay.setBackground(getResources().getDrawable(R.drawable.button_black2_pressed));
                }
                else {
                    buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2_pressed));
                    buttonplay.setShadowLayer(10,0,0, getResources().getColor(R.color.white));
                }
            }
            return false;
        }
    };
    View.OnClickListener buttonplayListener = new View.OnClickListener() {       // PLAY pressed
        @Override
        public void onClick(View view) {
            // play pressed
            //Toast.makeText(getApplicationContext(), "Timer IsPlaying Save_volume", Toast.LENGTH_SHORT).show();
            if (mBound) {
                boolean Player_isPlaying_State = mService.getPlayer_isPlaying_State();
                //Toast.makeText(getApplicationContext(), "Player_PlaybackState : " + Player_isPlaying_State, Toast.LENGTH_SHORT).show();
                if(Player_isPlaying_State) { // STOP action
                    mService.PausePlayer();// Pause player...
                    if (timer_started) {
                        CountDownTimer.cancel(); // Cancel an existing timer
                        timer_started = false;
                    }
                }
                else { // PLAY action
                    mService.PlayPlayer(); // Start player
                }
            }
        }
    };


    //////////////////////////////////////////////////////////////////////////////////////////////// ACTION BAR
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        this.menu = menu;
        if (dark_theme) {
            menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.settings_black));
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.timer_black));
            menu.getItem(2).setIcon(getResources().getDrawable(R.drawable.exit_dark));
        }
        else {
            menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.settings));
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.timer));
            menu.getItem(2).setIcon(getResources().getDrawable(R.drawable.exit));
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int res_id = item.getItemId();
        if (res_id == R.id.action_1)
        {
            lance_settings();
        }
        if (res_id == R.id.action_2)
        {
            timer_choose();
        }
        if (res_id == R.id.action_3)
        {
            mService.stopSelf();
            finish();
        }
        return true;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// SETTINGS
    protected void lance_settings() {
        save_playingState_and_current_sound(); // Save current playing state
        Intent i = new Intent(MainActivity.this, Settings.class);
        startActivityForResult(i, INTENT_RETURN_ID);                                                // Lance Settings
    }
    private void save_playingState_and_current_sound() {
        if (mBound) {
            boolean Player_isPlaying_State = mService.getPlayer_isPlaying_State();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  In preferences
            SharedPreferences.Editor editor = preferences.edit(); // Mode edition
            editor.putBoolean("Save_playing_state", Player_isPlaying_State); // Save value
            editor.putString("Save_current_sound", sound); // Save current sound
            editor.apply(); // Save
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {                 // Return from Settings
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        saved_sound = preferences.getString("Save_current_sound", "Bass_5mn"); // Read previous sound
        sound = preferences.getString("Sound", "Bass_5mn"); // Read new sound
        saved_playing_state = preferences.getBoolean("Save_playing_state", false); // Read previous playing state
        if (!saved_sound.equals(sound)) { // If sound changed
            mService.StopPlayer();
            mService.get_RawRessource_for_player(); // Prepare new sound
            mService.prepare_Player();
        }
        if (saved_playing_state) {
            mService.PlayPlayer(); // Play if player was playing
        }
        maj_preferences();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// PREFERENCES Update
    private void maj_preferences() { // Read user saved values
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        autostart = preferences.getBoolean("Autostart", false);
        dark_theme = preferences.getBoolean("Dark_theme", false);
        sound = preferences.getString("Sound", "Original");
        if (dark_theme) {
            linear_fond.setBackgroundColor(getResources().getColor(R.color.black));
            TextV1.setTextColor(getResources().getColor(R.color.white));
            TextV2.setTextColor(getResources().getColor(R.color.white));
            TextV3.setTextColor(getResources().getColor(R.color.white));
            TextV4.setTextColor(getResources().getColor(R.color.white));
            TextV5.setTextColor(getResources().getColor(R.color.white));
            TextV1.setShadowLayer(0,0,0, R.color.transparent);
            TextV2.setShadowLayer(0,0,0, R.color.transparent);
            TextV3.setShadowLayer(0,0,0, R.color.transparent);
            TextV4.setShadowLayer(0,0,0, R.color.transparent);
            TextV5.setShadowLayer(0,0,0, R.color.transparent);
            buttonplay.setBackground(getResources().getDrawable(R.drawable.button_black2));
            buttonplay.setTextColor(getResources().getColor(R.color.white));
        }
        else {
            linear_fond.setBackground(getResources().getDrawable(R.drawable.fond));
            TextV1.setTextColor(getResources().getColor(R.color.text_color));
            TextV2.setTextColor(getResources().getColor(R.color.text_color));
            TextV3.setTextColor(getResources().getColor(R.color.text_color));
            TextV4.setTextColor(getResources().getColor(R.color.text_color));
            TextV5.setTextColor(getResources().getColor(R.color.text_color));
            TextV1.setShadowLayer(6,0,0, getResources().getColor(R.color.white));
            TextV2.setShadowLayer(6,0,0, getResources().getColor(R.color.white));
            TextV3.setShadowLayer(6,0,0, getResources().getColor(R.color.white));
            TextV4.setShadowLayer(6,0,0, getResources().getColor(R.color.white));
            TextV5.setShadowLayer(6,0,0, getResources().getColor(R.color.white));
            buttonplay.setTextColor(getResources().getColor(R.color.text_color));
            buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2));
        }
        invalidateOptionsMenu(); // Update menu icon
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// TIMER
    protected void timer_choose() {
        String[] dialog_items = {"01 mn", "02 mn", "03 mn", "04 mn", "05 mn", "06 mn", "07 mn", "08 mn", "09 mn", "10 mn", "15 mn", "20 mn", "25 mn", "30 mn"};
        AlertDialog.Builder timer_dialog_builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
        timer_dialog_builder
                .setTitle(R.string.timer_title)
                .setItems(dialog_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch(which){
                            case 0:
                                timer_time = 60000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 1:
                                timer_time = 120000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 2:
                                timer_time = 180000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 3:
                                timer_time = 240000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 4:
                                timer_time = 300000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 5:
                                timer_time = 360000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 6:
                                timer_time = 420000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 7:
                                timer_time = 480000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 8:
                                timer_time = 540000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 9:
                                timer_time = 600000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 10:
                                timer_time = 900000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 11:
                                timer_time = 1200000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 12:
                                timer_time = 1500000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                            case 13:
                                timer_time = 1800000;
                                buttonplay.setText(String.format(FORMAT,
                                        TimeUnit.MILLISECONDS.toMinutes(timer_time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timer_time)),
                                        TimeUnit.MILLISECONDS.toSeconds(timer_time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timer_time))));
                                timer_start();
                                break;
                        }
                    }
                });
        AlertDialog timer_alert = timer_dialog_builder.create();
        timer_alert.show();
    }
    public void timer_start() {
        if (timer_started) {
            CountDownTimer.cancel(); // Stop existing timer
            timer_started = false;
        }
        if (mBound) {
            countdown(); // Start timer
            //update_volume();
            boolean Player_isPlaying_State = mService.getPlayer_isPlaying_State();
            if(!Player_isPlaying_State) {
                mService.PlayPlayer(); // Start player if needed
            }
        }
    }
    public void countdown() {
        timer_started = true;
        CountDownTimer = new CountDownTimer(timer_time, 1000) {
            public void onTick(long millisUntilFinished) {
                buttonplay.setText(String.format(FORMAT,
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }
            public void onFinish() {
                mService.PausePlayer();// Pause player
                timer_started = false;
            }
        }.start();
    }

} //Activity end


