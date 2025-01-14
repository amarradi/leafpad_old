package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String EXTRA_NOTE_ID = "com.git.amarradi.leafpad";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String DESIGN_MODE = "system";

    public List<Note> notes;
    public SimpleAdapter adapter;
    public ListView listView;
    private boolean showHidden = false;

    List<Map<String, String>> data = new ArrayList<>();

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupSharedPreferences();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String themes = sharedPreferences.getString(DESIGN_MODE, "");
        changeTheme(themes);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        adapter = new SimpleAdapter(this, data,
                R.layout.note_list_item,
                new String[]{"title", "date", "time", "state"},
                new int[]{R.id.title_text, R.id.created_at, R.id.time_txt});

        //updateDataset();

        listView = findViewById(R.id.note_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapter, v, position, id) -> {

            // Hole die ID aus der angezeigten Datenliste
            String noteId = data.get(position).get("id");

            // Finde die Notiz in der "notes"-Liste anhand der ID
            Note selectedNote = null;
            for (Note note : notes) {
                if (note.getId().equals(noteId)) {
                    selectedNote = note;
                    break;
                }
            }

            if (selectedNote != null) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(EXTRA_NOTE_ID, selectedNote.getId());
                // Log.d("MainActivity", "Selected Note: ID=" + selectedNote.getId() + ", Title=" + selectedNote.getTitle());
                startActivity(intent);
            } //else {
              //  Log.e("MainActivity", "Error: Note not found for ID=" + noteId);
            //}
        });

        ExtendedFloatingActionButton extendedFloatingActionButton = findViewById(R.id.fab_action_add);
        extendedFloatingActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
            startActivity(intent);
        });
        listView.setEmptyView(findViewById(R.id.emptyElement));
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme".equals(key)) {
            loadThemeFromPreference(sharedPreferences);
        }
    }

    private void loadThemeFromPreference(SharedPreferences sharedPreferences) {
        changeTheme(sharedPreferences.getString(getString(R.string.theme_key), getString(R.string.system_preference_option_value)));
    }

    private void changeTheme(String themeValue) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(themeValue));
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DESIGN_MODE, themeValue);
        editor.apply();
    }

    private int toNightMode(String themeValue) {
        if("lightmode".equals(themeValue)) {
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if("darkmode".equals(themeValue)) {
                return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.item_show_hidden);
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        //invalidateOptionsMenu();
        return true;
    }

    @SuppressLint({"NonConstantResourceId"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.item_show_hidden:
                toggleShowHidden(item);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(MenuItem item) {
        showHidden = !showHidden;
        if (!showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        updateListView();
        invalidateOptionsMenu();
    }


    public void updateListView() {
        updateDataset();

        ((SimpleAdapter) listView.getAdapter()).notifyDataSetChanged();
    }


    public void updateDataset() {
        //Log.d("MainActivity", "updateDataset() called");
        notes = Leaf.loadAll(this, showHidden);
        data.clear();
        for (Note note : notes) {
            if (showHidden && note.isHide()) {
                Map<String, String> datum = new HashMap<>();
                datum.put("id", note.getId());
                datum.put("title", note.getTitle());
                datum.put("body", note.getBody());
                datum.put("date", note.getDate());
                datum.put("time", note.getTime());
                data.add(datum);
            } else if(!showHidden && !note.isHide()){
                Map<String, String> datum = new HashMap<>();
                datum.put("id", note.getId());
                datum.put("title", note.getTitle());
                datum.put("body", note.getBody());
                datum.put("date", note.getDate());
                datum.put("time", note.getTime());
                data.add(datum);
            }
        }
        // Debugging: Ausgabe der geladenen Daten
        //for (Map<String, String> datum : data) {
        //    Log.d("MainActivity", "Datum: " + datum);  // Hier wird jedes Element der Liste geloggt
        //}

        //Log.d("MainActivity", "Displayed Notes: " + data.size());
    }
}