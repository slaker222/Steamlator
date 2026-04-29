package com.winlator.cmod.contentdialog;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;

import java.util.Arrays;
import java.util.List;

public class LimitFpsDialog extends ContentDialog {
    private final XServerDisplayActivity activity;
    private final Spinner sFps;

    public LimitFpsDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.limit_fps_dialog);
        this.activity = activity;

        setTitle(R.string.limit_fps);
        setIcon(R.drawable.icon_settings);

        sFps = findViewById(R.id.SFpsLimit);

        List<String> items = Arrays.asList(
                activity.getString(R.string.unlimited),
                "30",
                "60",
                "120"
        );
        sFps.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, items));

        // We can't reliably query current DXVK_FRAME_RATE from the running process here,
        // so default to "Unlimited" and apply instantly on selection.
        sFps.setSelection(0);

        sFps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySelection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.LLBottomBar).setVisibility(View.GONE);
        setOnConfirmCallback(this::applySelection);
    }

    private void applySelection() {
        int position = sFps.getSelectedItemPosition();
        int limit = position == 1 ? 30 : position == 2 ? 60 : position == 3 ? 120 : 0;
        activity.setGameFpsLimit(limit);
    }
}

