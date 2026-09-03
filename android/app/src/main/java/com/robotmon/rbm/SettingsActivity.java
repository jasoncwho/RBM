package com.robotmon.rbm;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.robotmon.rbm.bot.BotSettingsStore;
import com.robotmon.rbm.bot.SettingDef;
import com.robotmon.rbm.bot.SettingsCatalog;

import java.util.Arrays;
import java.util.List;

/**
 * Settings screen, ported from index.html's dynamically-generated settings
 * form (see {@code genSettings()}/{@code getSwitchButton()} there): renders
 * {@link SettingsCatalog}'s grouped {@link SettingDef}s as switches/number
 * fields/a dropdown, persisting each change immediately via
 * {@link BotSettingsStore} -- the same "save on every input change" behavior
 * index.html used with localStorage.
 */
public class SettingsActivity extends AppCompatActivity {

    private BotSettingsStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new BotSettingsStore(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        for (SettingsCatalog.Group group : SettingsCatalog.groups()) {
            root.addView(buildGroupHeading(group.heading));
            for (SettingDef def : group.items) {
                root.addView(buildRow(def));
            }
        }

        Button resetButton = new Button(this);
        resetButton.setText("Reset to defaults");
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resetParams.topMargin = dp(24);
        resetParams.gravity = Gravity.CENTER_HORIZONTAL;
        resetButton.setLayoutParams(resetParams);
        resetButton.setOnClickListener(v -> {
            store.reset();
            recreate();
        });
        root.addView(resetButton);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        setContentView(scrollView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private TextView buildGroupHeading(String heading) {
        TextView view = new TextView(this);
        view.setText(heading);
        view.setTextSize(18);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(20);
        params.bottomMargin = dp(4);
        view.setLayoutParams(params);
        return view;
    }

    private View buildRow(SettingDef def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(6), 0, dp(6));

        TextView title = new TextView(this);
        title.setText(def.title);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(title);

        row.addView(buildControl(def));
        return row;
    }

    private View buildControl(SettingDef def) {
        switch (def.type) {
            case BOOLEAN: {
                Switch toggle = new Switch(this);
                toggle.setChecked(store.getBoolean(def.key, def.defaultBoolean));
                toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                        store.putBoolean(def.key, isChecked));
                return toggle;
            }
            case NUMBER: {
                EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setLayoutParams(new LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.WRAP_CONTENT));
                input.setText(String.valueOf(store.getNumber(def.key, def.defaultNumber)));
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.length() == 0) {
                            return;
                        }
                        try {
                            int value = Math.max(def.min, Math.min(def.max, Integer.parseInt(s.toString())));
                            store.putNumber(def.key, value);
                        } catch (NumberFormatException ignored) {
                            // leave the last valid stored value in place while the user is mid-edit
                        }
                    }
                });
                return input;
            }
            case DROPDOWN: {
                Spinner spinner = new Spinner(this);
                List<String> titles = Arrays.asList(def.dropdownTitles);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                String storedKey = store.getDropdown(def.key, def.defaultDropdownKey);
                int selection = Math.max(0, Arrays.asList(def.dropdownKeys).indexOf(storedKey));
                spinner.setSelection(selection);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        store.putDropdown(def.key, def.dropdownKeys[position]);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                return spinner;
            }
            default:
                throw new IllegalStateException("Unhandled setting type: " + def.type);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
