package palpac.coherence_cardiaque;


import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {

    CheckBox auto_start, save_volume, dark;
    Spinner spinner_sounds;
    Button button_cancel, button_save, button_file_choose, button_clear;
    TextView textView, textView_file_choose;
    Boolean auto_start_value, save_volume_value, dark_value, isSDPresent;
    LinearLayout fond, save;
    String sound;
    public int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        auto_start = findViewById(R.id.checkBox_autostart);
        save_volume = findViewById(R.id.checkBox_save_volume);
        dark = findViewById(R.id.checkBox_dark);
        spinner_sounds = findViewById(R.id.spinner);
        button_clear=findViewById(R.id.button_clear);
        button_file_choose = findViewById(R.id.button_file_choose);
        button_cancel = findViewById(R.id.button_cancel);
        button_save = findViewById(R.id.button_save);
        fond = findViewById(R.id.linear_fond);
        save =findViewById(R.id.linear_save);
        textView = findViewById(R.id.textV1);
        textView_file_choose = findViewById(R.id.textV_choose);
        textView_file_choose.setMovementMethod(new ScrollingMovementMethod());

        String[] spinner_items = new String[]{"CHOISIR UN SON", "Original", "Bass_5mn", "Bass", "Clap", "Clave", "Flute", "Guitar", "Reggae", "Shake"}; //List of items for the spinner.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, spinner_items); // Adapter to describe how the items are displayed
        spinner_sounds.setAdapter(adapter);

        maj_preferences();

        if (dark_value) {
            setContentView(R.layout.settings_dark);
            auto_start = findViewById(R.id.checkBox_autostart);
            save_volume = findViewById(R.id.checkBox_save_volume);
            dark = findViewById(R.id.checkBox_dark);
            spinner_sounds = findViewById(R.id.spinner);
            button_clear = findViewById(R.id.button_clear);
            button_file_choose = findViewById(R.id.button_file_choose);
            button_cancel = findViewById(R.id.button_cancel);
            button_save = findViewById(R.id.button_save);
            fond = findViewById(R.id.linear_fond);
            save =findViewById(R.id.linear_save);
            textView = findViewById(R.id.textV1);
            textView_file_choose = findViewById(R.id.textV_choose);
            textView_file_choose.setMovementMethod(new ScrollingMovementMethod());

            ArrayAdapter<String> adapter_black = new ArrayAdapter<>(this, R.layout.spinner_item_dark, spinner_items);
            spinner_sounds.setAdapter(adapter_black);
            spinner_sounds.setBackground(getResources().getDrawable(R.drawable.button_black2));

            maj_preferences();
        }

        spinner_sounds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {         // Spinner on select listener
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        textView_file_choose.setText("Original");
                        break;
                    case 2:
                        textView_file_choose.setText("Bass_5mn");
                        break;
                    case 3:
                        textView_file_choose.setText("Bass");
                        break;
                    case 4:
                        textView_file_choose.setText("Clap");
                        break;
                    case 5:
                        textView_file_choose.setText("Clave");
                        break;
                    case 6:
                        textView_file_choose.setText("Flute");
                        break;
                    case 7:
                        textView_file_choose.setText("Guitar");
                        break;
                    case 8:
                        textView_file_choose.setText("Reggae");
                        break;
                    case 9:
                        textView_file_choose.setText("Shake");
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // sometimes you need or nothing here
            }
        });

        isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // Detect SDcard
        if (!isSDPresent) { // If no SDcard
            button_file_choose.setEnabled(false); // Disable file choose
            button_file_choose.setText(getString(R.string.file_choose_disable));
            button_file_choose.setTextColor(getResources().getColor(R.color.grey));
            if (dark_value) {
                button_file_choose.setBackground(getResources().getDrawable(R.drawable.button_settings_disable_black));
            }
            else {
                button_file_choose.setBackground(getResources().getDrawable(R.drawable.button_settings_disable));
            }
        }

    } // onCreate end

    //////////////////////////////////////////////////////////////////////////////////////////////// UPDATE FROM PREFERENCES
    private void maj_preferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        auto_start_value = preferences.getBoolean("Autostart", false); // Lecture autostart
        if (auto_start_value)
            auto_start.setChecked(true);
        save_volume_value = preferences.getBoolean("Save_volume", false); // Lecture save_volume
        if (save_volume_value)
            save_volume.setChecked(true);
        dark_value = preferences.getBoolean("Dark_theme", false); // Lecture dark theme
        if (dark_value)
            dark.setChecked(true);
        sound = preferences.getString("Sound", "Original"); // Lecture sound path
        textView_file_choose.setText(sound);
    }

    public void FILE_CHOOSE(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //this means permission is granted and you can do read and write
            //Toast.makeText(getApplicationContext(), "SDcard : permisssion granded", Toast.LENGTH_LONG).show();
            SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(this, "FileOpen",
                    new SimpleFileDialog.SimpleFileDialogListener() {
                        @Override
                        public void onChosenDir(String chosenDir) {
                            sound = chosenDir; // Update sound path choosen
                            textView_file_choose.setText(sound); // Update texview with soundpath choosen
                        }
                    });
            FileOpenDialog.Default_File_Name = "";
            FileOpenDialog.chooseFile_or_Dir();
        }
        else {
            //Toast.makeText(getApplicationContext(),  "Asking for read/write SDcard permission", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions( Settings.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // Handle user permission response
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Request for Write SDcard permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(this, "FileOpen",
                        new SimpleFileDialog.SimpleFileDialogListener() {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                sound = chosenDir; // Update sound path choosen
                                textView_file_choose.setText(sound); // Update texview with soundpath choosen
                            }
                        });
                FileOpenDialog.Default_File_Name = "";
                FileOpenDialog.chooseFile_or_Dir();
            } else {
                // Permission request was denied.
                Toast.makeText(getApplicationContext(),  "Application need permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// CLEAR
    public void CLEAR(View view) {
        AlertDialog.Builder my_builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        my_builder
                .setCancelable(true)  // Demande validation pour quitter
                .setTitle(R.string.title_clear)
                .setMessage(R.string.message_clear)
                .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        textView_file_choose.setText("Original");
                        spinner_sounds.setSelection(0);
                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog alert = my_builder.create();
        alert.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// On click CANCEL/SAVE
    public void SAVE(View view) {
        switch (view.getId())
        {
            case R.id.button_cancel:
                onBackPressed();
                break;
            case R.id.button_save:
                save_values();
                onBackPressed();
                break;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// On click RATE
    public void RATE(View view) {
        switch (view.getId())
        {
            case R.id.button_rate:
                try
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                }
                catch (ActivityNotFoundException e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=palpac.coherence_cardiaque")));
                }
                break;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// On click RATE
    public void DONATE(View view) {
        switch (view.getId())
        {
            case R.id.button_donate:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/davidpalpacuer")));
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Save preferences
    private void save_values() {
        auto_start_value = auto_start.isChecked();  // Read values
        save_volume_value = save_volume.isChecked();
        dark_value = dark.isChecked();
        sound = textView_file_choose.getText().toString();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Open preferences
        SharedPreferences.Editor editor = preferences.edit(); // Edit mode
        editor.putBoolean("Autostart", auto_start_value); // Write values
        editor.putBoolean("Save_volume", save_volume_value);
        editor.putBoolean("Dark_theme", dark_value);
        editor.putString("Sound", sound);
        editor.apply(); // Save
    }

} // Activity end
