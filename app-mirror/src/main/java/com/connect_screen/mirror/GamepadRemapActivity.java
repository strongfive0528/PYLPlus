package com.connect_screen.mirror;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.connect_screen.mirror.gamepad.GamepadRemap;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Second level screen: map every Xbox style button onto any other button.
 *
 * Rows use a clickable value button instead of a Spinner: a Spinner here reported its own
 * selection as the last entry on the first layout pass and also ignored setSelection() for the
 * initial value, which silently rewrote the whole table into "everything maps to 右".
 */
public class GamepadRemapActivity extends AppCompatActivity {

    private final List<MaterialButton> targetButtons = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiTheme.apply(this);
        setContentView(R.layout.activity_gamepad_remap);

        LinearLayout rows = findViewById(R.id.remapRows);
        int[] mapping = Pref.getGamepadRemap();
        State.log("手柄重映射读取: " + java.util.Arrays.toString(mapping));

        for (int source = 0; source < GamepadRemap.COUNT; source++) {
            View row = getLayoutInflater().inflate(R.layout.item_gamepad_remap, rows, false);
            TextView label = row.findViewById(R.id.remapSourceLabel);
            MaterialButton target = row.findViewById(R.id.remapTargetButton);
            label.setText(GamepadRemap.LABELS[source]);
            target.setText(targetLabel(mapping[source]));

            final int sourceIndex = source;
            target.setOnClickListener(v -> showTargetPicker(sourceIndex));

            rows.addView(row);
            targetButtons.add(target);
        }

        findViewById(R.id.resetRemapButton).setOnClickListener(v -> {
            Pref.resetGamepadRemap();
            recreate();
        });
    }

    private void showTargetPicker(int sourceIndex) {
        int current = Pref.getGamepadRemap()[sourceIndex];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(GamepadRemap.LABELS[sourceIndex])
                .setSingleChoiceItems(GamepadRemap.LABELS, current, (d, which) -> {
                    Pref.setGamepadRemapTarget(sourceIndex, which);
                    targetButtons.get(sourceIndex).setText(targetLabel(which));
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .create();
        DialogStyle.apply(dialog);
        dialog.show();
    }

    private String targetLabel(int buttonId) {
        return GamepadRemap.LABELS[buttonId] + " ▾";
    }
}
