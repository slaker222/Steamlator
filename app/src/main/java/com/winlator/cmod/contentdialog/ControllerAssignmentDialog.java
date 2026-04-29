package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.inputcontrols.ControllerManager;

public class ControllerAssignmentDialog {
    private final ContentDialog dialog;
    private final ControllerManager controllerManager;
    private final CheckBox[] checkBoxes = new CheckBox[4];
    private final TextView[] deviceNameTextViews = new TextView[4];
    private final Button[] assignButtons = new Button[4];
    private final Button[] macrosButtons = new Button[4];
    private final CheckBox[] vibrateBoxes = new CheckBox[4];
    private final Button[] resetButtons = new Button[4];
    private final TextView restartRequiredView;
    private final int initialPlayerCount;
    private final Activity hostActivity;

    public static void show(Context context) {
        ControllerManager.getInstance().init(context);
        int initialPlayerCount = ControllerManager.getInstance().getEnabledPlayerCount();
        new ControllerAssignmentDialog((Activity) context, initialPlayerCount).showContentDialog();
    }

    private ControllerAssignmentDialog(Activity activity, int initialPlayerCount) {
        boolean dark = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean("dark_mode", false);
        ContextThemeWrapper themed =
                new ContextThemeWrapper(activity, dark ? R.style.ContentDialog : R.style.AppTheme);

        this.dialog = new ContentDialog(themed, R.layout.controller_assignment_dialog);
        this.dialog.setTitle(R.string.controller_manager);
        this.controllerManager = ControllerManager.getInstance();
        this.initialPlayerCount = initialPlayerCount;
        this.hostActivity = activity;

        initializeViews();
        restartRequiredView = dialog.getContentView().findViewById(R.id.TVRestartRequired);

        if (dark) {
            View root = dialog.getContentView();
            if (root instanceof ViewGroup) setTextColorForDialog((ViewGroup) root, 0xFFFFFFFF);
        }

        populateView();
        setupListeners();
    }

    private static int dp(Context context, int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    @SuppressWarnings("deprecation")
    public void showContentDialog() {
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;

        int widthPx;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.view.WindowMetrics metrics = window.getWindowManager().getCurrentWindowMetrics();
            widthPx = metrics.getBounds().width();
        } else {
            Point point = new Point();
            window.getWindowManager().getDefaultDisplay().getSize(point);
            widthPx = point.x;
        }

        int capPx = dp(dialog.getContext(), 540);
        int target = Math.min((int) (widthPx * 0.90f), capPx);
        window.setLayout(target, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void initializeViews() {
        View view = dialog.getContentView();

        checkBoxes[0] = view.findViewById(R.id.CBPlayer1);
        deviceNameTextViews[0] = view.findViewById(R.id.TVPlayer1DeviceName);
        assignButtons[0] = view.findViewById(R.id.BTNAssignP1);
        macrosButtons[0] = view.findViewById(R.id.BTNMacrosP1);
        vibrateBoxes[0] = view.findViewById(R.id.CBVibrateP1);
        resetButtons[0] = view.findViewById(R.id.BTNResetP1);

        checkBoxes[1] = view.findViewById(R.id.CBPlayer2);
        deviceNameTextViews[1] = view.findViewById(R.id.TVPlayer2DeviceName);
        assignButtons[1] = view.findViewById(R.id.BTNAssignP2);
        macrosButtons[1] = view.findViewById(R.id.BTNMacrosP2);
        vibrateBoxes[1] = view.findViewById(R.id.CBVibrateP2);
        resetButtons[1] = view.findViewById(R.id.BTNResetP2);

        checkBoxes[2] = view.findViewById(R.id.CBPlayer3);
        deviceNameTextViews[2] = view.findViewById(R.id.TVPlayer3DeviceName);
        assignButtons[2] = view.findViewById(R.id.BTNAssignP3);
        macrosButtons[2] = view.findViewById(R.id.BTNMacrosP3);
        vibrateBoxes[2] = view.findViewById(R.id.CBVibrateP3);
        resetButtons[2] = view.findViewById(R.id.BTNResetP3);

        checkBoxes[3] = view.findViewById(R.id.CBPlayer4);
        deviceNameTextViews[3] = view.findViewById(R.id.TVPlayer4DeviceName);
        assignButtons[3] = view.findViewById(R.id.BTNAssignP4);
        macrosButtons[3] = view.findViewById(R.id.BTNMacrosP4);
        vibrateBoxes[3] = view.findViewById(R.id.CBVibrateP4);
        resetButtons[3] = view.findViewById(R.id.BTNResetP4);
    }

    private void populateView() {
        controllerManager.scanForDevices();

        for (int i = 0; i < 4; i++) {
            checkBoxes[i].setChecked(controllerManager.isSlotEnabled(i));
            vibrateBoxes[i].setChecked(controllerManager.isVibrationEnabled(i));

            InputDevice device = controllerManager.getAssignedDeviceForSlot(i);
            deviceNameTextViews[i].setText(
                    device != null ? device.getName() : dialog.getContext().getString(R.string.not_assigned)
            );
            deviceNameTextViews[i].setSelected(true);
        }
    }

    private void setupListeners() {
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i;

            checkBoxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                controllerManager.setSlotEnabled(slotIndex, isChecked);
                if (!isChecked) {
                    for (int j = slotIndex + 1; j < 4; j++) {
                        if (controllerManager.isSlotEnabled(j)) controllerManager.setSlotEnabled(j, false);
                    }
                } else {
                    for (int j = 0; j < slotIndex; j++) {
                        if (!controllerManager.isSlotEnabled(j)) controllerManager.setSlotEnabled(j, true);
                    }
                }
                populateView();
                restartRequiredView.setVisibility(
                        controllerManager.getEnabledPlayerCount() != initialPlayerCount
                                ? View.VISIBLE
                                : View.GONE
                );
            });

            vibrateBoxes[i].setOnCheckedChangeListener((buttonView, checked) ->
                    controllerManager.setVibrationEnabled(slotIndex, checked));

            macrosButtons[i].setOnClickListener(v -> MacrosDialog.show(hostActivity, slotIndex));

            resetButtons[i].setOnClickListener(v -> {
                controllerManager.unassignSlot(slotIndex);
                populateView();
            });

            assignButtons[i].setOnClickListener(v -> {
                String message = dialog.getContext().getString(R.string.press_any_button_for_player)
                        + " " + (slotIndex + 1);
                dialog.setMessage(message);
                dialog.setOnControllerInputListener(device -> {
                    if (!ControllerManager.isGameController(device)) return;
                    controllerManager.assignDeviceToSlot(slotIndex, device);
                    dialog.setMessage(null);
                    dialog.setOnControllerInputListener(null);
                    populateView();
                });
            });
        }

        dialog.setOnConfirmCallback(controllerManager::saveAssignments);
    }

    private void setTextColorForDialog(ViewGroup viewGroup, int color) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                setTextColorForDialog((ViewGroup) child, color);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            }
        }
    }
}
