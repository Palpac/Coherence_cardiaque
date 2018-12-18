package palpac.coherence_cardiaque;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    CheckBox auto_start, dark;
    Button button_cancel, button_save, button_file_choose, button_clear;
    Boolean auto_start_value, dark_value, SDcard;
    LinearLayout fond, save;
    TextView textView, textView_file_choose;
    String sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        auto_start = findViewById(R.id.checkBox_autostart);
        dark = findViewById(R.id.checkBox_dark);
        button_clear=findViewById(R.id.button_clear);
        button_file_choose = findViewById(R.id.button_file_choose);
        button_cancel = findViewById(R.id.button_cancel);
        button_save = findViewById(R.id.button_save);
        fond = findViewById(R.id.linear_fond);
        save =findViewById(R.id.linear_save);
        textView = findViewById(R.id.textV1);
        textView_file_choose = findViewById(R.id.textV_choose);
        textView_file_choose.setMovementMethod(new ScrollingMovementMethod());

        maj_preferences();

        if (dark_value) {
            setContentView(R.layout.settings_dark);
            auto_start = findViewById(R.id.checkBox_autostart);
            dark = findViewById(R.id.checkBox_dark);
            button_clear=findViewById(R.id.button_clear);
            button_file_choose = findViewById(R.id.button_file_choose);
            button_cancel = findViewById(R.id.button_cancel);
            button_save = findViewById(R.id.button_save);
            fond = findViewById(R.id.linear_fond);
            save =findViewById(R.id.linear_save);
            textView = findViewById(R.id.textV1);
            textView_file_choose = findViewById(R.id.textV_choose);
            textView_file_choose.setMovementMethod(new ScrollingMovementMethod());

            maj_preferences();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Maj checkbox
    private void maj_preferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        auto_start_value = preferences.getBoolean("Autostart", false); // Lecture autostart
        if (auto_start_value)
            auto_start.setChecked(true);
        dark_value = preferences.getBoolean("Dark_theme", false); // Lecture dark theme
        if (dark_value)
            dark.setChecked(true);
        sound = preferences.getString("Sound", "Sound1"); // Lecture sound path
        textView_file_choose.setText(sound);
        SDcard = preferences.getBoolean("SDcard", false); // Lecture SDcard
        if (!SDcard) { // If no SDcard
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
    }
    //////////////////////////////////////////////////////////////////////////////////////////////// On click Cancel/Save
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
    public void FILE_CHOOSE(View view) {
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
    public void CLEAR(View view) {
        AlertDialog.Builder my_builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        my_builder
                .setCancelable(true)  // Demande validation pour quitter
                .setTitle(R.string.title_clear)
                .setMessage(R.string.message_clear)
                .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        textView_file_choose.setText("Sound1");
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
    //////////////////////////////////////////////////////////////////////////////////////////////// Save preferences
    private void save_values() {
        auto_start_value = auto_start.isChecked();  // Lectures checkbox
        dark_value = dark.isChecked();
        sound = textView_file_choose.getText().toString();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Dans preferences
        SharedPreferences.Editor editor = preferences.edit(); // Mode edition
        editor.putBoolean("Autostart", auto_start_value); // Write checkbox state
        editor.putBoolean("Dark_theme", dark_value);
        editor.putString("Sound", sound);
        editor.apply(); // Save
    }

}
