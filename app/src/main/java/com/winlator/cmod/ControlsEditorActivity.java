package com.winlator.cmod;

import android.net.Uri;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.R;
import com.winlator.cmod.inputcontrols.Binding;
import com.winlator.cmod.inputcontrols.ControlElement;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.widget.InputControlsView;
import com.winlator.cmod.widget.NumberPicker;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import java.io.FileWriter;

public class ControlsEditorActivity extends AppCompatActivity implements View.OnClickListener {
    private InputControlsView inputControlsView;
    private ControlsProfile profile;
    private ControlElement iconPickerElement;
    private LinearLayout iconPickerList;
    private final ActivityResultLauncher<String> importIconImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImportIconImageSelected);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        AppUtils.hideSystemUI(this);
        setContentView(R.layout.controls_editor_activity);

        inputControlsView = new InputControlsView(this);
        inputControlsView.setEditMode(true);
        inputControlsView.setOverlayOpacity(0.6f);

        profile = InputControlsManager.loadProfile(this, ControlsProfile.getProfileFile(this, getIntent().getIntExtra("profile_id", 0)));
        ((TextView)findViewById(R.id.TVProfileName)).setText(profile.getName());
        inputControlsView.setProfile(profile);

        FrameLayout container = findViewById(R.id.FLContainer);
        container.addView(inputControlsView, 0);

        container.findViewById(R.id.BTAddElement).setOnClickListener(this);
        container.findViewById(R.id.BTRemoveElement).setOnClickListener(this);
        container.findViewById(R.id.BTElementSettings).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BTAddElement:
                if (!inputControlsView.addElement()) {
                    AppUtils.showToast(this, R.string.no_profile_selected);
                }
                break;
            case R.id.BTRemoveElement:
                if (!inputControlsView.removeElement()) {
                    AppUtils.showToast(this, R.string.no_control_element_selected);
                }
                break;
            case R.id.BTElementSettings:
                ControlElement selectedElement = inputControlsView.getSelectedElement();
                if (selectedElement != null) {
                    showControlElementSettings(v);
                }
                else AppUtils.showToast(this, R.string.no_control_element_selected);
                break;
        }
    }

    private void showControlElementSettings(View anchorView) {
        final ControlElement element = inputControlsView.getSelectedElement();
        View view = LayoutInflater.from(this).inflate(R.layout.control_element_settings, null);

        final Runnable updateLayout = () -> {
            ControlElement.Type type = element.getType();
            view.findViewById(R.id.LLShape).setVisibility(View.GONE);
            view.findViewById(R.id.CBToggleSwitch).setVisibility(View.GONE);
            view.findViewById(R.id.LLCustomTextIcon).setVisibility(View.GONE);
            view.findViewById(R.id.LLRangeOptions).setVisibility(View.GONE);
            view.findViewById(R.id.LLSwipePadTexts).setVisibility(View.GONE);
            view.findViewById(R.id.LLRadialMenuOptions).setVisibility(View.GONE);

            if (type == ControlElement.Type.BUTTON) {
                view.findViewById(R.id.LLShape).setVisibility(View.VISIBLE);
                view.findViewById(R.id.CBToggleSwitch).setVisibility(View.VISIBLE);
                view.findViewById(R.id.LLCustomTextIcon).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.RANGE_BUTTON) {
                view.findViewById(R.id.LLRangeOptions).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.SWIPE_PAD) {
                view.findViewById(R.id.LLSwipePadTexts).setVisibility(View.VISIBLE);
            }
            else if (type == ControlElement.Type.RADIAL_MENU) {
                view.findViewById(R.id.LLRadialMenuOptions).setVisibility(View.VISIBLE);
            }

            loadBindingSpinners(element, view);
        };

        loadTypeSpinner(element, view.findViewById(R.id.SType), updateLayout);
        loadShapeSpinner(element, view.findViewById(R.id.SShape));
        loadRangeSpinner(element, view.findViewById(R.id.SRange));

        RadioGroup rgOrientation = view.findViewById(R.id.RGOrientation);
        rgOrientation.check(element.getOrientation() == 1 ? R.id.RBVertical : R.id.RBHorizontal);
        rgOrientation.setOnCheckedChangeListener((group, checkedId) -> {
            element.setOrientation((byte)(checkedId == R.id.RBVertical ? 1 : 0));
            profile.save();
            inputControlsView.invalidate();
        });

        NumberPicker npColumns = view.findViewById(R.id.NPColumns);
        npColumns.setValue(element.getBindingCount());
        npColumns.setOnValueChangeListener((numberPicker, value) -> {
            element.setBindingCount(value);
            profile.save();
            inputControlsView.invalidate();
        });

        NumberPicker npBindings = view.findViewById(R.id.NPBindings);
        npBindings.setValue(element.getBindingCount());
        npBindings.setOnValueChangeListener((numberPicker, value) -> {
            element.setBindingCount(value);
            loadBindingSpinners(element, view);
            profile.save();
            inputControlsView.invalidate();
        });

        final TextView tvScale = view.findViewById(R.id.TVScale);
        SeekBar sbScale = view.findViewById(R.id.SBScale);
        sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvScale.setText(progress+"%");
                if (fromUser) {
                    progress = (int)Mathf.roundTo(progress, 5);
                    seekBar.setProgress(progress);
                    element.setScale(progress / 100.0f);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbScale.setProgress((int)(element.getScale() * 100));

        CheckBox cbToggleSwitch = view.findViewById(R.id.CBToggleSwitch);
        cbToggleSwitch.setChecked(element.isToggleSwitch());
        cbToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            element.setToggleSwitch(isChecked);
            profile.save();
        });

        final EditText etCustomText = view.findViewById(R.id.ETCustomText);
        etCustomText.setText(element.getText());
        final LinearLayout llIconList = view.findViewById(R.id.LLIconList);
        int selectedIconId = element.getIconId() & 0xFF;
        loadIcons(llIconList, selectedIconId);
        view.findViewById(R.id.BTImportIconImage).setOnClickListener((v) -> {
            iconPickerElement = element;
            iconPickerList = llIconList;
            importIconImageLauncher.launch("image/*");
        });

        final EditText etSwipeCenter = view.findViewById(R.id.ETSwipeCenter);
        final EditText etSwipeUp = view.findViewById(R.id.ETSwipeUp);
        final EditText etSwipeRight = view.findViewById(R.id.ETSwipeRight);
        final EditText etSwipeDown = view.findViewById(R.id.ETSwipeDown);
        final EditText etSwipeLeft = view.findViewById(R.id.ETSwipeLeft);
        String[] swipeTexts = element.getSwipePadTexts();
        etSwipeCenter.setText(swipeTexts[0]);
        etSwipeUp.setText(swipeTexts[1]);
        etSwipeRight.setText(swipeTexts[2]);
        etSwipeDown.setText(swipeTexts[3]);
        etSwipeLeft.setText(swipeTexts[4]);

        updateLayout.run();

        PopupWindow popupWindow = AppUtils.showPopupWindow(anchorView, view, 340, 0);
        popupWindow.setOnDismissListener(() -> {
            String text = etCustomText.getText().toString().trim();
            Integer iconId = null;
            for (int i = 0; i < llIconList.getChildCount(); i++) {
                View child = llIconList.getChildAt(i);
                if (child.isSelected()) {
                    iconId = (Integer) child.getTag();
                    break;
                }
            }

            element.setText(text);
            if (iconId != null) {
                element.setIconId(iconId);
                element.setCustomIconPath("");
            }
            
            if (element.getType() == ControlElement.Type.SWIPE_PAD) {
                element.setSwipePadTexts(new String[]{
                        etSwipeCenter.getText().toString().trim(),
                        etSwipeUp.getText().toString().trim(),
                        etSwipeRight.getText().toString().trim(),
                        etSwipeDown.getText().toString().trim(),
                        etSwipeLeft.getText().toString().trim()
                });
            }
            
            profile.save();
            inputControlsView.invalidate();
            iconPickerElement = null;
            iconPickerList = null;
        });
    }

    private void onImportIconImageSelected(Uri uri) {
        if (uri == null || iconPickerElement == null) return;

        File customIconsDir = getCustomIconsDir();
        if (!customIconsDir.exists()) customIconsDir.mkdirs();
        int newIconId = findNextAvailableIconId();
        if (newIconId < 0) return;
        File customIconFile = new File(customIconsDir, newIconId + ".png");

        Bitmap sourceBitmap;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return;
            sourceBitmap = BitmapFactory.decodeStream(inputStream);
        }
        catch (IOException e) {
            AppUtils.showToast(this, R.string.unable_to_find_file);
            return;
        }
        if (sourceBitmap == null) {
            AppUtils.showToast(this, R.string.unable_to_find_file);
            return;
        }

        showImportStyleDialog(sourceBitmap, customIconFile, newIconId);
    }

    private void loadTypeSpinner(final ControlElement element, Spinner spinner, Runnable callback) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Type.names()));
        spinner.setSelection(element.getType().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setType(ControlElement.Type.values()[position]);
                profile.save();
                callback.run();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadShapeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Shape.names()));
        spinner.setSelection(element.getShape().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setShape(ControlElement.Shape.values()[position]);
                profile.save();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadBindingSpinners(ControlElement element, View view) {
        LinearLayout container = view.findViewById(R.id.LLBindings);
        container.removeAllViews();

        ControlElement.Type type = element.getType();
        if (type == ControlElement.Type.BUTTON) {
            loadBindingSpinner(element, container, 0, R.string.binding);
        }
        else if (type == ControlElement.Type.D_PAD || type == ControlElement.Type.STICK || type == ControlElement.Type.TRACKPAD || type == ControlElement.Type.SWIPE_PAD) {
            loadBindingSpinner(element, container, 0, R.string.binding_up);
            loadBindingSpinner(element, container, 1, R.string.binding_right);
            loadBindingSpinner(element, container, 2, R.string.binding_down);
            loadBindingSpinner(element, container, 3, R.string.binding_left);
        }
        else if (type == ControlElement.Type.RADIAL_MENU) {
            int bindingCount = element.getBindingCount();
            for (int i = 0; i < bindingCount; i++) {
                loadBindingSpinner(element, container, i, R.string.binding);
            }
        }
    }

    private void loadBindingSpinner(final ControlElement element, LinearLayout container, final int index, int titleResId) {
        View view = LayoutInflater.from(this).inflate(R.layout.binding_field, container, false);
        ((TextView)view.findViewById(R.id.TVTitle)).setText(titleResId);
        final Spinner sBindingType = view.findViewById(R.id.SBindingType);
        final Spinner sBinding = view.findViewById(R.id.SBinding);

        Runnable update = () -> {
            String[] bindingEntries = null;
            switch (sBindingType.getSelectedItemPosition()) {
                case 0:
                    bindingEntries = Binding.keyboardBindingLabels();
                    break;
                case 1:
                    bindingEntries = Binding.mouseBindingLabels();
                    break;
                case 2:
                    bindingEntries = Binding.gamepadBindingLabels();
                    break;
            }

            sBinding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bindingEntries));
            AppUtils.setSpinnerSelectionFromValue(sBinding, element.getBindingAt(index).toString());
        };

        sBindingType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                update.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Binding selectedBinding = element.getBindingAt(index);
        if (selectedBinding.isKeyboard()) {
            sBindingType.setSelection(0, false);
        }
        else if (selectedBinding.isMouse()) {
            sBindingType.setSelection(1, false);
        }
        else if (selectedBinding.isGamepad()) {
            sBindingType.setSelection(2, false);
        }

        sBinding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Binding binding = Binding.NONE;
                switch (sBindingType.getSelectedItemPosition()) {
                    case 0:
                        binding = Binding.keyboardBindingValues()[position];
                        break;
                    case 1:
                        binding = Binding.mouseBindingValues()[position];
                        break;
                    case 2:
                        binding = Binding.gamepadBindingValues()[position];
                        break;
                }

                if (binding != element.getBindingAt(index)) {
                    element.setBindingAt(index, binding);
                    profile.save();
                    inputControlsView.invalidate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        update.run();
        container.addView(view);
    }

    private void loadRangeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Range.names()));
        spinner.setSelection(element.getRange().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setRange(ControlElement.Range.values()[position]);
                profile.save();
                inputControlsView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadIcons(final LinearLayout parent, int selectedId) {
        parent.removeAllViews();
        int[] iconIds = getAllIconIds();

        int size = (int)UnitUtils.dpToPx(40);
        int margin = (int)UnitUtils.dpToPx(2);
        int padding = (int)UnitUtils.dpToPx(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, 0, margin, 0);

        for (final int id : iconIds) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(params);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setBackgroundResource(R.drawable.icon_background);
            imageView.setTag(id);
            imageView.setSelected(id == selectedId);
            imageView.setOnClickListener((v) -> {
                for (int i = 0; i < parent.getChildCount(); i++) parent.getChildAt(i).setSelected(false);
                imageView.setSelected(true);
            });

            imageView.setImageBitmap(loadIconBitmap(id));

            parent.addView(imageView);
        }
    }

    private File getCustomIconsDir() {
        return new File(getFilesDir(), "inputcontrols/icons");
    }

    private int findNextAvailableIconId() {
        boolean[] used = new boolean[256];
        for (int id : getAllIconIds()) {
            if (id >= 0 && id <= 255) used[id] = true;
        }
        for (int id = 1; id <= 255; id++) {
            if (!used[id]) return id;
        }
        AppUtils.showToast(this, R.string.unable_to_import_profile);
        return -1;
    }

    private int[] getAllIconIds() {
        TreeSet<Integer> ids = new TreeSet<>();
        try {
            String[] assetFilenames = getAssets().list("inputcontrols/icons/");
            if (assetFilenames != null) {
                for (String filename : assetFilenames) {
                    ids.add(Integer.parseInt(FileUtils.getBasename(filename)));
                }
            }
        }
        catch (IOException | NumberFormatException ignored) {}

        File customIconsDir = getCustomIconsDir();
        File[] customFiles = customIconsDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (customFiles != null) {
            for (File file : customFiles) {
                try {
                    ids.add(Integer.parseInt(FileUtils.getBasename(file.getName())));
                }
                catch (NumberFormatException ignored) {}
            }
        }

        ArrayList<Integer> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        int[] result = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) result[i] = sorted.get(i);
        return result;
    }

    private android.graphics.Bitmap loadIconBitmap(int iconId) {
        try (InputStream is = getAssets().open("inputcontrols/icons/" + iconId + ".png")) {
            return BitmapFactory.decodeStream(is);
        }
        catch (IOException ignored) {}

        File customIconFile = new File(getCustomIconsDir(), iconId + ".png");
        if (customIconFile.isFile()) return BitmapFactory.decodeFile(customIconFile.getAbsolutePath());
        return null;
    }

    private void showImportStyleDialog(Bitmap sourceBitmap, File outputFile, int iconId) {
        Bitmap whiteStyleBitmap = createWhiteStyleBitmap(sourceBitmap);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF6E6E6E);

        LinearLayout previewRow = new LinearLayout(this);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setWeightSum(2f);

        LinearLayout originalColumn = new LinearLayout(this);
        originalColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams originalColumnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        originalColumnParams.setMargins(0, 0, 8, 0);
        originalColumn.setLayoutParams(originalColumnParams);

        ImageView ivOriginal = new ImageView(this);
        ivOriginal.setImageBitmap(sourceBitmap);
        ivOriginal.setAdjustViewBounds(true);
        ivOriginal.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) UnitUtils.dpToPx(120));
        ivOriginal.setLayoutParams(p1);

        Button btOriginal = new Button(this);
        btOriginal.setText(R.string.use_original_style);
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        b1.setMargins(0, 10, 0, 0);
        btOriginal.setLayoutParams(b1);

        LinearLayout whiteColumn = new LinearLayout(this);
        whiteColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams whiteColumnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        whiteColumnParams.setMargins(8, 0, 0, 0);
        whiteColumn.setLayoutParams(whiteColumnParams);

        ImageView ivWhite = new ImageView(this);
        ivWhite.setImageBitmap(whiteStyleBitmap);
        ivWhite.setAdjustViewBounds(true);
        ivWhite.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) UnitUtils.dpToPx(120));
        ivWhite.setLayoutParams(p2);

        Button btWhite = new Button(this);
        btWhite.setText(R.string.use_white_style);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        b2.setMargins(0, 10, 0, 0);
        btWhite.setLayoutParams(b2);

        originalColumn.addView(ivOriginal);
        originalColumn.addView(btOriginal);
        whiteColumn.addView(ivWhite);
        whiteColumn.addView(btWhite);

        previewRow.addView(originalColumn);
        previewRow.addView(whiteColumn);
        root.addView(previewRow);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.import_image)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        btOriginal.setOnClickListener(v -> {
            if (saveImportedIconVariant(sourceBitmap, outputFile, iconId, false)) dialog.dismiss();
        });
        btWhite.setOnClickListener(v -> {
            if (saveImportedIconVariant(whiteStyleBitmap, outputFile, iconId, true)) dialog.dismiss();
        });

        dialog.show();
    }

    private boolean saveImportedIconVariant(Bitmap bitmap, File outputFile, int iconId, boolean tinted) {
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
        }
        catch (IOException e) {
            AppUtils.showToast(this, R.string.unable_to_find_file);
            return false;
        }

        if (!writeIconTintMetadata(iconId, tinted ? "1" : "0")) {
            AppUtils.showToast(this, R.string.unable_to_find_file);
            return false;
        }

        iconPickerElement.setCustomIconPath("");
        iconPickerElement.setIconId(iconId);
        if (iconPickerList != null) loadIcons(iconPickerList, iconId);
        profile.save();
        inputControlsView.invalidate();
        return true;
    }

    private boolean writeIconTintMetadata(int iconId, String value) {
        File metaFile = new File(getCustomIconsDir(), iconId + ".meta");
        try (FileWriter writer = new FileWriter(metaFile, false)) {
            writer.write(value);
            writer.flush();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    private Bitmap createWhiteStyleBitmap(Bitmap source) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);  // Custom slide animations for exiting
    }

}
