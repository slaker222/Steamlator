package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;

import java.util.ArrayList;
import java.util.List;

public final class MacrosDialog {
    public static final String PREF_PREFIX = "macros_";

    private MacrosDialog() {}

    public static void show(Activity activity, int slotIndex) {
        ContentDialog dialog = new ContentDialog(activity, R.layout.macros_dialog_list);
        dialog.setTitle(R.string.input_macros);
        dialog.setIcon(R.drawable.icon_gamepad);

        boolean dark = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean("dark_mode", false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        RecyclerView recyclerView = dialog.findViewById(R.id.RVMacros);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        List<Item> items = buildItems(slotIndex, prefs);
        Adapter adapter = new Adapter(items, prefs, dark);
        recyclerView.setAdapter(adapter);

        if (dark) {
            TextView hint = dialog.findViewById(R.id.TVHint);
            if (hint != null) hint.setTextColor(0xFFFFFFFF);
            View includeTriggers = dialog.findViewById(R.id.SWIncludeTriggers);
            if (includeTriggers instanceof TextView) ((TextView) includeTriggers).setTextColor(0xFFFFFFFF);
        }

        CompoundButton includeTriggers = dialog.findViewById(R.id.SWIncludeTriggers);
        String includeKey = prefKey(slotIndex, "IncludeTriggers");
        includeTriggers.setChecked(prefs.getBoolean(includeKey, false));
        includeTriggers.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(includeKey, isChecked).apply());

        View.OnClickListener enableAll = v -> {
            for (Item item : items) {
                item.enabled = true;
                prefs.edit().putBoolean(item.prefKey, true).apply();
            }
            adapter.notifyDataSetChanged();
        };
        View.OnClickListener disableAll = v -> {
            for (Item item : items) {
                item.enabled = false;
                prefs.edit().putBoolean(item.prefKey, false).apply();
            }
            adapter.notifyDataSetChanged();
        };

        Button enableAllButton = dialog.findViewById(R.id.BTEnableAllLocal);
        Button disableAllButton = dialog.findViewById(R.id.BTDisableAllLocal);
        enableAllButton.setOnClickListener(enableAll);
        disableAllButton.setOnClickListener(disableAll);

        dialog.show();
    }

    private static List<Item> buildItems(int slot, SharedPreferences prefs) {
        ArrayList<Item> list = new ArrayList<>();
        add(list, slot, prefs, "A", R.string.button_a);
        add(list, slot, prefs, "B", R.string.button_b);
        add(list, slot, prefs, "X", R.string.button_x);
        add(list, slot, prefs, "Y", R.string.button_y);
        add(list, slot, prefs, "LB", R.string.button_lb);
        add(list, slot, prefs, "RB", R.string.button_rb);
        add(list, slot, prefs, "Back", R.string.button_back);
        add(list, slot, prefs, "Start", R.string.button_start);
        add(list, slot, prefs, "L3", R.string.button_l3);
        add(list, slot, prefs, "R3", R.string.button_r3);
        add(list, slot, prefs, "DpadUp", R.string.dpad_up);
        add(list, slot, prefs, "DpadDown", R.string.dpad_down);
        add(list, slot, prefs, "DpadLeft", R.string.dpad_left);
        add(list, slot, prefs, "DpadRight", R.string.dpad_right);
        return list;
    }

    private static void add(List<Item> list, int slot, SharedPreferences prefs, String logical, int labelRes) {
        String key = prefKey(slot, logical);
        list.add(new Item(labelRes, key, prefs.getBoolean(key, false)));
    }

    public static String prefKey(int slot, String logical) {
        return PREF_PREFIX + "p" + (slot + 1) + "_" + logical;
    }

    private static final class Item {
        final int labelRes;
        final String prefKey;
        boolean enabled;

        Item(int labelRes, String prefKey, boolean enabled) {
            this.labelRes = labelRes;
            this.prefKey = prefKey;
            this.enabled = enabled;
        }
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView label;
        final ToggleButton toggle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.TVLabel);
            toggle = itemView.findViewById(R.id.TBToggle);
        }
    }

    private static final class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<Item> data;
        private final SharedPreferences prefs;
        private final boolean dark;
        private final android.content.res.ColorStateList whiteState;

        Adapter(List<Item> data, SharedPreferences prefs, boolean dark) {
            this.data = data;
            this.prefs = prefs;
            this.dark = dark;
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int white = 0xFFFFFFFF;
            this.whiteState = new android.content.res.ColorStateList(states, new int[]{white, white});
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_macro_toggle, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = data.get(position);
            holder.label.setText(holder.itemView.getContext().getString(item.labelRes));
            holder.toggle.setOnCheckedChangeListener(null);
            holder.toggle.setChecked(item.enabled);

            if (dark) {
                holder.label.setTextColor(0xFFFFFFFF);
                holder.toggle.setTextColor(whiteState);
            }

            holder.toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                item.enabled = isChecked;
                prefs.edit().putBoolean(item.prefKey, isChecked).apply();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
