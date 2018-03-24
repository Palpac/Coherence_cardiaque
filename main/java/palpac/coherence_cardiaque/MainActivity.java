package palpac.coherence_cardiaque;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button buttonplay;
    Button buttonstop;
    TextView TextV1, TextV2, TextV3, TextV4, TextV5;
    LinearLayout linear_fond;
    MediaPlayer mediaPlayer;
    boolean BooleanMedia1IsPlaying; // Flag lecture = true, stopped = false
    public android.support.v4.app.NotificationCompat.Builder notificationBuilder;
    Toolbar toolbar;
    Menu menu;
    Boolean autostart, dark_theme, SDcard = false;
    int INTENT_RETURN_ID;
    String sound, sdcard_sounds_path, sdcard_res_path, sound1; // Chemin SDcard sounds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar); // handle toolbar from id
        setSupportActionBar(toolbar); // Set toolbar as an actionbar
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide app name in toolbar

        buttonplay = findViewById(R.id.buttonplay);
        buttonstop = findViewById(R.id.buttonstop);
        buttonstop.setTextColor(getResources().getColor(R.color.text_disable));
        buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
        buttonplay.setOnClickListener(buttonplayListener);
        buttonplay.setOnTouchListener(buttonplayTouchListener);
        buttonstop.setOnClickListener(buttonstopListener);
        buttonstop.setOnTouchListener(buttonstopTouchListener);
        linear_fond = findViewById(R.id.linear_fond);
        TextV1 = findViewById(R.id.text1);
        TextV2 = findViewById(R.id.text2);
        TextV3 = findViewById(R.id.text3);
        TextV4 = findViewById(R.id.text4);
        TextV5 = findViewById(R.id.text5);

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        Boolean isSDSupportedDevice = Environment.isExternalStorageRemovable();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Dans preferences
        SharedPreferences.Editor editor = preferences.edit(); // Mode edition
        if(isSDSupportedDevice && isSDPresent) { // SDcard is present
            SDcard = true;
            editor.putBoolean("SDcard", true);
            sdcard_res_path = Environment.getExternalStorageDirectory().toString() + "/Android/data";
            sdcard_sounds_path = sdcard_res_path + "/CoherenceCardiaque";
            copy_res_to_sd(); // Copy sounds to SDcard for user
        }
        else { // No SDcard found
            SDcard = false;
            editor.putBoolean("SDcard", false);
        }
        editor.apply(); // Save in preference file

        maj_preferences(); // Lecture preferences utilisateur
        createNotification(); // Notification

        //////////////////////////////////////////////////////////////////////////////////////////// AUTOSTART
        if (autostart) {
            VerifMedia1Paying ();
            if (SDcard) {
                if (sound.equals("Sound1")) {
                    mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load default raw/mp3
                    mediaPlayer.setLooping(true);
                    LanceMedia1 ();
                }
                else {
                    try {
                        mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(sound)); // Try load from custom sound path
                        mediaPlayer.setLooping(true);
                        LanceMedia1 ();
                    }
                    catch (Exception e) {
                        editor.putString("Sound", "Sound1"); // Reset sound path
                        editor.apply();
                        Toast.makeText(getApplicationContext(),  R.string.sound_error, Toast.LENGTH_SHORT).show();
                        mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1); // Load default sound
                        mediaPlayer.setLooping(true);
                        LanceMedia1 ();
                    }
                }
            }
            else {
                mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load audio raw/mp3
                mediaPlayer.setLooping(true);
                LanceMedia1 ();
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// MEDIAPlAYER
    protected void VerifMedia1Paying (){  // Vérif si mediaplayer en lecture
        if (BooleanMedia1IsPlaying) {
            mediaPlayer.stop();
            mediaPlayer.release();
            BooleanMedia1IsPlaying = false;
            if (dark_theme) {
                buttonplay.setTextColor(getResources().getColor(R.color.white));
                buttonstop.setTextColor(getResources().getColor(R.color.white_grey));
            }
            else {
                buttonstop.setTextColor(getResources().getColor(R.color.text_disable));
                buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
                buttonplay.setTextColor(getResources().getColor(R.color.text_color));
                buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2));
            }
        }
    }
    protected void LanceMedia1 (){ // Lance mediaplayer
        mediaPlayer.start();
        BooleanMedia1IsPlaying = mediaPlayer.isPlaying(); // A utiliser après start
        if (dark_theme) {
            buttonplay.setTextColor(getResources().getColor(R.color.white_grey));
            buttonstop.setTextColor(getResources().getColor(R.color.white));
        }
        else {
            buttonplay.setTextColor(getResources().getColor(R.color.text_disable));
            buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
            buttonstop.setTextColor(getResources().getColor(R.color.text_color));
            buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2));
        }
    }






    //////////////////////////////////////////////////////////////////////////////////////////////// Listeners
    View.OnTouchListener buttonplayTouchListener = new View.OnTouchListener() { // PLAY PRESSED
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) { // Button released
                //Toast.makeText(getApplicationContext(),  "Up", Toast.LENGTH_SHORT).show();
                if (dark_theme) {
                    buttonplay.setBackground(getResources().getDrawable(R.drawable.button_black2));
                }
                else {
                    buttonplay.setShadowLayer(0,0,0, R.color.transparent);
                    if (BooleanMedia1IsPlaying) {
                        buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
                    }
                    else {
                        buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2));
                    }
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
    View.OnTouchListener buttonstopTouchListener = new View.OnTouchListener() { // STOP PRESSED
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (dark_theme) {
                    buttonstop.setBackground(getResources().getDrawable(R.drawable.button_black2));
                }
                else {
                    buttonstop.setShadowLayer(0,0,0, R.color.transparent);
                    if (BooleanMedia1IsPlaying) {
                        buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
                    }
                    else {
                        buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2));
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (dark_theme) {
                    buttonstop.setBackground(getResources().getDrawable(R.drawable.button_black2_pressed));
                }
                else {
                    buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2_pressed));
                    buttonstop.setShadowLayer(10,0,0, getResources().getColor(R.color.white));
                }

            }
            return false;
        }
    };
    View.OnClickListener buttonplayListener = new View.OnClickListener() { //PLAY
        @Override
        public void onClick(View view) {
            VerifMedia1Paying ();
            if (SDcard) {
                if (sound.equals("Sound1")) {
                    mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load default raw/mp3
                    mediaPlayer.setLooping(true);
                    LanceMedia1 ();
                }
                else {
                    try {
                        mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(sound)); // Try load from custom sound path
                        mediaPlayer.setLooping(true);
                        LanceMedia1 ();
                    }
                    catch (Exception e) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("Sound", "Sound1"); // Reset sound path
                        editor.apply();
                        Toast.makeText(getApplicationContext(),  R.string.sound_error, Toast.LENGTH_SHORT).show();
                        mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1); // Load default sound
                        mediaPlayer.setLooping(true);
                        LanceMedia1 ();
                    }
                }
            }
            else {
                mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load audio raw/mp3
                mediaPlayer.setLooping(true);
                LanceMedia1 ();
            }
        }
    };
    View.OnClickListener buttonstopListener = new View.OnClickListener() { //STOP
        @Override
        public void onClick(View view) {
            VerifMedia1Paying ();
        }
    };

    //////////////////////////////////////////////////////////////////////////////////////////////// Notification
    private void createNotification() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("NotClick", true);
        PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (dark_theme) {
            notificationBuilder = new android.support.v4.app.NotificationCompat.Builder(this, "Title")
                    .setContentIntent(intent)
                    .setSmallIcon(R.drawable.h_src) // android:icon="@drawable/h" in manifest
                    .setTicker(getResources().getString(R.string.notif_ticker))
                    .setContentTitle(getResources().getString(R.string.notif_title))
                    .setContentText(getResources().getString(R.string.notif_text));
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1,notificationBuilder.build());
        }
        else {
            notificationBuilder = new android.support.v4.app.NotificationCompat.Builder(this, "Title")
                    .setContentIntent(intent)
                    .setSmallIcon(R.mipmap.cc_f_mid) // android:icon="@drawable/heart_icon" in manifest
                    .setTicker(getResources().getString(R.string.notif_ticker))
                    .setContentTitle(getResources().getString(R.string.notif_title))
                    .setContentText(getResources().getString(R.string.notif_text));
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1,notificationBuilder.build());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) { // Retour depuis notification
        try {
            Bundle extras = intent.getExtras();
            if (extras.getBoolean("NotClick")) {
                //Toast.makeText(getApplicationContext(), "return", Toast.LENGTH_SHORT).show();
                boolean flag = false;
                if (BooleanMedia1IsPlaying) {
                    VerifMedia1Paying();
                    //Toast.makeText(getApplicationContext(), "true", Toast.LENGTH_SHORT).show();
                    flag = true; // Arret lecture = action effectué
                }
                if (!flag) { // Si pas action effectuée
                    if (!BooleanMedia1IsPlaying) {
                        VerifMedia1Paying ();
                        if (SDcard) {
                            if (sound.equals("Sound1")) {
                                mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load default raw/mp3
                                mediaPlayer.setLooping(true);
                                LanceMedia1 ();
                            }
                            else {
                                try {
                                    mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(sound)); // Try load from custom sound path
                                    mediaPlayer.setLooping(true);
                                    LanceMedia1 ();
                                }
                                catch (Exception e) {
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("Sound", "Sound1"); // Reset sound path
                                    editor.apply();
                                    Toast.makeText(getApplicationContext(),  R.string.sound_error, Toast.LENGTH_SHORT).show();
                                    mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1); // Load default sound
                                    mediaPlayer.setLooping(true);
                                    LanceMedia1 ();
                                }
                            }
                        }
                        else {
                            mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.cc1);   // Load audio raw/mp3
                            mediaPlayer.setLooping(true);
                            LanceMedia1 ();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("onclick", "Exception onclick" + e);
        }
    }

    @Override
    public void onBackPressed() {
        if (BooleanMedia1IsPlaying) {  // Si lecture en cours
            AlertDialog.Builder my_builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
            my_builder
                    .setCancelable(true)  // Demande validation pour quitter
                    .setTitle(R.string.alert_title)
                    .setMessage(R.string.alert_message)
                    .setPositiveButton(R.string.alert_postive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Toast.makeText(getApplicationContext(), "Rester", Toast.LENGTH_SHORT).show();
                            dialogInterface.cancel();
                        }
                    })
                    .setNegativeButton(R.string.alert_negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Toast.makeText(getApplicationContext(), "Quitter", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
            AlertDialog alert = my_builder.create();
            alert.show();
        }
        else finish();
    }

    @Override
    protected void onDestroy() { // when user end app or system destroy for memory use.
        //Toast.makeText(getApplicationContext(), "Destroy", Toast.LENGTH_SHORT).show();
        VerifMedia1Paying (); // Arrêt Mediaplayer si lancé
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1); // Cancel notification


        super.onDestroy();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        this.menu = menu;
        if (dark_theme) {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.settings_black));
        }
        else {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.settings));
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int res_id = item.getItemId();
        if (res_id == R.id.action_1)
        {
            //Toast.makeText(getApplicationContext(), "Help text here", Toast.LENGTH_SHORT).show();
            lance_settings();
        }
        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// Settings
    protected void lance_settings() {
        Intent i = new Intent(MainActivity.this, Settings.class);
        startActivityForResult(i, INTENT_RETURN_ID);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// Update Preferences
    private void maj_preferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        autostart = preferences.getBoolean("Autostart", false); // Lecture autostart
        dark_theme = preferences.getBoolean("Dark_theme", false); // Lecture dark theme
        sound = preferences.getString("Sound", "Sound1"); // Lecture sound path
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
            buttonstop.setBackground(getResources().getDrawable(R.drawable.button_black2));
            if (BooleanMedia1IsPlaying) {
                buttonplay.setTextColor(getResources().getColor(R.color.white_grey));
                buttonstop.setTextColor(getResources().getColor(R.color.white));
            }
            else {
                buttonplay.setTextColor(getResources().getColor(R.color.white));
                buttonstop.setTextColor(getResources().getColor(R.color.white_grey));

            }
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
            if (BooleanMedia1IsPlaying) {
                buttonplay.setTextColor(getResources().getColor(R.color.text_disable));
                buttonstop.setTextColor(getResources().getColor(R.color.text_color));
                buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
                buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2));
            }
            else {
                buttonplay.setTextColor(getResources().getColor(R.color.text_color));
                buttonstop.setTextColor(getResources().getColor(R.color.text_disable));
                buttonplay.setBackground(getResources().getDrawable(R.drawable.heart_button2));
                buttonstop.setBackground(getResources().getDrawable(R.drawable.heart_button2_transparent));
            }
        }
        createNotification(); // Update notification icon
        invalidateOptionsMenu(); // Update menu icon
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// Return from Settings
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        maj_preferences();
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// SDcard
    private void copy_res_to_sd () {
        // Repertory
        File rep = new File(sdcard_res_path, "CoherenceCardiaque"); // Create directory in SDcard if needed
        if (!rep.exists()) {
            rep.mkdirs();
        }
        // 1_cc1
        File sound1_audiofile = new File(sdcard_sounds_path, "1_default.mp3"); // Declaration fichier audio destination
        if (!sound1_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream sound1_audio = getResources().openRawResource(R.raw.cc1); // Creation stream audio depuis raw
            try {
                FileOutputStream sound1_audio_out = new FileOutputStream(sdcard_sounds_path + "/1_default.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = sound1_audio.read(buff)) > 0) {
                        sound1_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    sound1_audio.close(); // Fermeture flux
                    sound1_audio_out.close(); // Et fichier son
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 2_bass
        File bass_audiofile = new File(sdcard_sounds_path, "2_bass.mp3"); // Declaration fichier audio destination
        if (!bass_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream bass_audio = getResources().openRawResource(R.raw.bass); // Creation stream audio depuis raw
            try {
                FileOutputStream bass_audio_out = new FileOutputStream(sdcard_sounds_path + "/2_bass.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = bass_audio.read(buff)) > 0) {
                        bass_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    bass_audio.close(); // Fermeture flux
                    bass_audio_out.close(); // Et fichier son
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 3_clap
        File clap_audiofile = new File(sdcard_sounds_path, "3_clap.mp3"); // Declaration fichier audio destination
        if (!clap_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream clap_audio = getResources().openRawResource(R.raw.clap); // Creation stream audio depuis raw
            try {
                FileOutputStream clap_audio_out = new FileOutputStream(sdcard_sounds_path + "/3_clap.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = clap_audio.read(buff)) > 0) {
                        clap_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    clap_audio.close(); // Fermeture flux
                    clap_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 4_clave
        File clave_audiofile = new File(sdcard_sounds_path, "4_clave.mp3"); // Declaration fichier audio destination
        if (!clave_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream clave_audio = getResources().openRawResource(R.raw.clave); // Creation stream audio depuis raw
            try {
                FileOutputStream clave_audio_out = new FileOutputStream(sdcard_sounds_path + "/4_clave.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = clave_audio.read(buff)) > 0) {
                        clave_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    clave_audio.close(); // Fermeture flux
                    clave_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 5_flute
        File flute_audiofile = new File(sdcard_sounds_path, "5_flute.mp3"); // Declaration fichier audio destination
        if (!flute_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream flute_audio = getResources().openRawResource(R.raw.flute); // Creation stream audio depuis raw
            try {
                FileOutputStream flute_audio_out = new FileOutputStream(sdcard_sounds_path + "/5_flute.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = flute_audio.read(buff)) > 0) {
                        flute_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    flute_audio.close(); // Fermeture flux
                    flute_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 6_guitar
        File guitar_audiofile = new File(sdcard_sounds_path, "6_guitar.mp3"); // Declaration fichier audio destination
        if (!guitar_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream guitar_audio = getResources().openRawResource(R.raw.guitar); // Creation stream audio depuis raw
            try {
                FileOutputStream guitar_audio_out = new FileOutputStream(sdcard_sounds_path + "/6_guitar.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = guitar_audio.read(buff)) > 0) {
                        guitar_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    guitar_audio.close(); // Fermeture flux
                    guitar_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 7_reggae
        File reggae_audiofile = new File(sdcard_sounds_path, "7_reggae.mp3"); // Declaration fichier audio destination
        if (!reggae_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream reggae_audio = getResources().openRawResource(R.raw.reggae); // Creation stream audio depuis raw
            try {
                FileOutputStream reggae_audio_out = new FileOutputStream(sdcard_sounds_path + "/7_reggae.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = reggae_audio.read(buff)) > 0) {
                        reggae_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    reggae_audio.close(); // Fermeture flux
                    reggae_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
        // 8_shake
        File shake_audiofile = new File(sdcard_sounds_path, "8_shake.mp3"); // Declaration fichier audio destination
        if (!shake_audiofile.exists()) { // Si fichier existe pas déjà
            InputStream shake_audio = getResources().openRawResource(R.raw.shake); // Creation stream audio depuis raw
            try {
                FileOutputStream shake_audio_out = new FileOutputStream(sdcard_sounds_path + "/8_shake.mp3"); // Declaration flux audio destination
                byte[] buff = new byte[1024]; // taille buffer
                int read = 0;
                try {
                    while ((read = shake_audio.read(buff)) > 0) {
                        shake_audio_out.write(buff, 0, read); // Ecriture fichier
                    }
                } finally {
                    shake_audio.close(); // Fermeture flux
                    shake_audio_out.close(); // Et fichier son
                }
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.file_copy_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
