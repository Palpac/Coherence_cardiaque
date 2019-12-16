package palpac.coherence_cardiaque;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.util.Util;


public class AudioPlayerService extends Service {

    //int i=0;

    private SimpleExoPlayer player;
    private RawResourceDataSource rawResourceDataSource;    // For player
    private PlayerNotificationManager playerNotificationManager;
    private int PLAYBACK_NOTIFICATION_ID = 1520;    // For notification
    private String PLAYBACK_CHANNEL_ID = "Coherence Cardiaque Channel"; // For notification
    private boolean is_playing; // To detect true player state changes
    DataSpec dataSpec;
    private AudioManager s_audioManager; // For volume control
    private int save_volume_value, restore_volume, restore_volume_value;
    boolean save_volume;

    private final IBinder mBinder = new LocalBinder(); // Binder given to clients onBind Service
    final Context context = this;
    String sound;
    Boolean sound_path_mode = false;

    private PhoneStateListener phoneStateListener; // Used to handle incoming PHONE CALL
    private TelephonyManager telephonyManager;
    int phoneStateListener_count = 0; // Used to prevent starting state change
    boolean to_restart = false; // Used to restart player after a phone call

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() { // Init headphones unpglugged receiver
        @Override
        public void onReceive(Context context, Intent intent) { // Headphone unppluged receiver
            //Toast.makeText(getApplicationContext(),  "mNoisyReceiver", Toast.LENGTH_SHORT).show();
            PausePlayer(); // Pause player if headphones unplugged
        }
    };


    SharedPreferences preferences;

    public class LocalBinder extends Binder { // To dialog with MainActivity
        AudioPlayerService getService() { // Return this instance of LocalService so clients can...
            return AudioPlayerService.this; // ...call public methods (getPlayer_isPlaying_State)
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// SERVICE LIFE CYCLE
    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(getApplicationContext(), "onBind Service", Toast.LENGTH_SHORT).show();
        return mBinder;
    }
    @Override
    public int onStartCommand(
            Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        //Toast.makeText(getApplicationContext(),  "onDestroy Service", Toast.LENGTH_SHORT).show();
        if (phoneStateListener != null) { // Unregister phone calls listener
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        playerNotificationManager.setPlayer(null);
        player.release();
        player = null;
        stopSelf();
        super.onDestroy();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// ON CREATE
    @Override
    public void onCreate() {
        super.onCreate();

        s_audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); // For volume control
        restore_volume_value = s_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // Read current

        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());           // Create PLAYER
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(                  // Player data source
                context, Util.getUserAgent(context, "David_AudioService"));
        rawResourceDataSource = new RawResourceDataSource(this);
        get_RawRessource_for_player();
        prepare_Player();


        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context, PLAYBACK_CHANNEL_ID, R.string.app_name, PLAYBACK_NOTIFICATION_ID,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(context, MainActivity.class);
                        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }
                    @Override
                    public String getCurrentContentTitle(Player player) { return getString(R.string.notif_title); }
                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) { return getString(R.string.notif_text); }
                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.heart_icon_trim);
                        return icon;
                    }
                }
        );

        preferences = PreferenceManager.getDefaultSharedPreferences(this); // Open shared preferences
        boolean autostart = preferences.getBoolean("Autostart", false); // Read autostart value
        //Toast.makeText(getApplicationContext(),  "Autostart" + autostart, Toast.LENGTH_SHORT).show();
        if(autostart){ // If user want autostart
            player.setPlayWhenReady(true); // Autostart Player
            Intent broadcast_playing = new Intent("Coherence_cardiaque.broadcast.playingstate");
            broadcast_playing.putExtra("playbackState", "True"); // Send Play broadcast
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast_playing);
        }
        else { // Send Stop broadcast
            Intent broadcast_playing = new Intent("Coherence_cardiaque.broadcast.playingstate");
            broadcast_playing.putExtra("playbackState", "False");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast_playing);
        }

        //////////////////////////////////////////////////////////////////////////////////////// Player LISTENER
        player.addListener(new Player.EventListener() {
            @Override public void onTimelineChanged(Timeline timeline, Object manifest, int reason) { }
            @Override public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) { }
            @Override public void onLoadingChanged(boolean isLoading) { }
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {        // PLAY PAUSE CALLBACK
                //Toast.makeText(getApplicationContext(),  "onPlayerStateChanged", Toast.LENGTH_SHORT).show();
                if (playWhenReady) {
                    try {
                        //i++;
                        //Toast.makeText(getApplicationContext(),  "Play "+i, Toast.LENGTH_SHORT).show();
                        update_volume();
                        Intent broadcast_playing = new Intent("Coherence_cardiaque.broadcast.playingstate"); // Create new intent to be sent as a broadcast
                        broadcast_playing.putExtra("playbackState", "True"); // Add playing state  to the intent as an extra
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast_playing); // Send intent with extra in local broadcast
                        is_playing = true;
                        IntentFilter noisy_filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY); // Start listening headphone unplugged
                        registerReceiver(mNoisyReceiver, noisy_filter); // by filtering becoming noisy message from system
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (!playWhenReady) {
                    if (is_playing) { // Avoid multiple onPlayerStateChanged when Stop, to save volume only one time
                        try {
                            //i++;
                            //Toast.makeText(getApplicationContext(),  "Stop "+i, Toast.LENGTH_SHORT).show();
                            save_volume();
                            Intent broadcast_playing = new Intent("Coherence_cardiaque.broadcast.playingstate");
                            broadcast_playing.putExtra("playbackState", "False");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast_playing);
                            is_playing = false;
                            player.seekTo(0);         // Return to audio starting point
                            unregisterReceiver(mNoisyReceiver); // Stop listening headphone to be unplugged
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            @Override public void onRepeatModeChanged(int repeatMode) { }
            @Override public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) { }
            @Override public void onPlayerError(ExoPlaybackException error) { }
            @Override public void onPositionDiscontinuity(int reason) { }
            @Override public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) { }
            @Override public void onSeekProcessed() { }
        });

        //////////////////////////////////////////////////////////////////////////////////////// Player NOTIFICATION manager also create lock screen media notification
        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationStarted(int notificationId, Notification notification) {
                startForeground(notificationId, notification);
            }
            @Override
            public void onNotificationCancelled(int notificationId) {
                stopSelf();
                try {
                    Intent notification_cancelled = new Intent("Coherence_cardiaque.broadcast.playingstate");
                    notification_cancelled.putExtra("playbackState", "Cancel");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notification_cancelled);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setUseNavigationActions(false); // omit skip previous and next actions
        playerNotificationManager.setFastForwardIncrementMs(0); // omit fast forward action by setting the increment to zero
        playerNotificationManager.setRewindIncrementMs(0); // omit rewind action by setting the increment to zero
        //playerNotificationManager.setStopAction(null); // omit the stop action
        playerNotificationManager.setUseChronometer(false);
        playerNotificationManager.setSmallIcon(R.mipmap.cc_f_mid);

        callStateListener(); // Handle phone calls to stop player

    } // OnCreate end

    //////////////////////////////////////////////////////////////////////////////////////////////// Public methods called by intent from MainActivity
    public void PausePlayer() {
        player.setPlayWhenReady(false);                                                             // Pause
    }
    public void PlayPlayer() {
        player.setPlayWhenReady(true);                                                              // Play
    }
    public boolean getPlayer_isPlaying_State() {
        return player.getPlayWhenReady();                                                           // Return isPlaying to MainActivity
    }
    public void StopPlayer() {
        player.stop();
    }
    public void get_RawRessource_for_player() {
        sound_path_mode = false; // Reset mode detection flag
        preferences = PreferenceManager.getDefaultSharedPreferences(this); // Open shared preferences
        sound = preferences.getString("Sound", "Bass_5mn"); // Read user saved sound with Bass_5mn as app default sound
        //Toast.makeText(getApplicationContext(), "get_RawRessource_for_player : " + sound, Toast.LENGTH_SHORT).show();
        if(sound.equals("Original")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.cc1));
        }
        else if(sound.equals("Bass")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.bass));
        }
        else if(sound.equals("Bass_5mn")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.bass2));
        }
        else if(sound.equals("Clap")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.clap));
        }
        else if(sound.equals("Clave")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.clave));
        }
        else if(sound.equals("Flute")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.flute));
        }
        else if(sound.equals("Guitar")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.guitar));
        }
        else if(sound.equals("Reggae")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.reggae));
        }
        else if(sound.equals("Shake")){
            dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.shake));
        }
        else { // Default sound
            sound_path_mode = true; // File path chosen flag
        }
    }
    public void prepare_Player() {
        if (sound_path_mode) { // If file path chosen by user
            DataSpec dataSpec = new DataSpec(Uri.parse(sound));
            final FileDataSource fileDataSource = new FileDataSource();
            try {
                fileDataSource.open(dataSpec);
                DataSource.Factory factory = new DataSource.Factory() {
                    @Override
                    public DataSource createDataSource() {
                        return fileDataSource;
                    }
                };
                MediaSource audioSource = new ExtractorMediaSource(fileDataSource.getUri(),
                        factory, new DefaultExtractorsFactory(), null, null);

                LoopingMediaSource loopingMediaSource = new LoopingMediaSource(audioSource);
                player.prepare(loopingMediaSource);
            } catch (FileDataSource.FileDataSourceException e) {
                e.printStackTrace();
            }
        }
        else { // If proposed sound chosen
            try {
                rawResourceDataSource.open(dataSpec);
                DataSource.Factory factory = new DataSource.Factory() {
                    @Override
                    public DataSource createDataSource() {
                        return rawResourceDataSource;
                    }
                };
                MediaSource audioSource = new ExtractorMediaSource.Factory(factory).createMediaSource(rawResourceDataSource.getUri());
                LoopingMediaSource loopingMediaSource = new LoopingMediaSource(audioSource);
                player.prepare(loopingMediaSource); // Loop mode
            } catch (RawResourceDataSource.RawResourceDataSourceException e) {
                e.printStackTrace();
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// Handle incoming PHONE CALL
    //////////////////////////////////////////////////////////////////////////////////////////////// Need manifest permission
    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); // Get the telephony manager
        phoneStateListener = new PhoneStateListener() { //Starting listening for PhoneState changes
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                phoneStateListener_count += 1;
                Boolean offhook = false; // To detect the IDLE state thar follow OFFHOOK
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        //Toast.makeText(getApplicationContext(), "CALL_STATE_RINGING : " + incomingNumber, Toast.LENGTH_SHORT).show();
                        boolean Player_isPlaying_State_ringing = getPlayer_isPlaying_State();
                        if(Player_isPlaying_State_ringing) {
                            PausePlayer();// Pause player
                            to_restart = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        offhook = true;
                        //Toast.makeText(getApplicationContext(), "CALL_STATE_OFFHOOK", Toast.LENGTH_SHORT).show();
                        boolean Player_isPlaying_State = getPlayer_isPlaying_State();
                        if(Player_isPlaying_State) {
                            PausePlayer();// Pause player
                            to_restart = true;
                        }
                    case TelephonyManager.CALL_STATE_IDLE: // Phone is inactive
                        //Toast.makeText(getApplicationContext(), "CALL_STATE_IDLE", Toast.LENGTH_SHORT).show();
                        if (!offhook & phoneStateListener_count != 1) {
                            if(to_restart) {
                                PlayPlayer(); // Restart player if was playing
                                to_restart = false; //
                            }
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); // Register the listener
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// VOLUME
    private void update_volume() {                                                                  // Update
        restore_volume_value = s_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // Read current
        //Toast.makeText(getApplicationContext(), "To restore " + restore_volume_value, Toast.LENGTH_SHORT).show();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        save_volume = preferences.getBoolean("Save_volume", false);
        if (save_volume){ // If user want it
            if (isHeadsetOn(context)) {
                save_volume_value = preferences.getInt("Save_volume_value_headphone", 6); // Read saved headphone volume
                //Toast.makeText(getApplicationContext(), " Updated headphone vol : " + save_volume_value, Toast.LENGTH_SHORT).show();
            }
            else {
                save_volume_value = preferences.getInt("Save_volume_value", 6); // Read saved volume
                //Toast.makeText(getApplicationContext(), " Updated vol : " + save_volume_value, Toast.LENGTH_SHORT).show();
            }
            s_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, save_volume_value, 0); // Set saved volume

        }
    }
    private void save_volume() {                                                                    //save
        save_volume_value = s_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // Get music volume value
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit(); // Mode edition
        if (isHeadsetOn(context)) {
            editor.putInt("Save_volume_value_headphone", save_volume_value); // Write value
            //Toast.makeText(getApplicationContext(), "Saved headphone vol : " + save_volume_value, Toast.LENGTH_SHORT).show();
        }
        else {
            editor.putInt("Save_volume_value", save_volume_value);
            //Toast.makeText(getApplicationContext(), "Saved vol : " + save_volume_value, Toast.LENGTH_SHORT).show();
        }
        editor.apply(); // Save
        save_volume = preferences.getBoolean("Save_volume", false);
        if (save_volume) { // If user want it
            s_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restore_volume_value, 0); // Restore volume
            //Toast.makeText(getApplicationContext(), " Restored : " + restore_volume_value + "", Toast.LENGTH_SHORT).show();
        }

    }
    private boolean isHeadsetOn(Context context) { // HeadPhone detection
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return s_audioManager.isWiredHeadsetOn();
        } else {
            AudioDeviceInfo[] devices = s_audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (int i_devices = 0; i_devices < devices.length; i_devices++) {
                AudioDeviceInfo device = devices[i_devices];
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    return true;
                }
            }
        }
        return false;
    }



} // Service class end

