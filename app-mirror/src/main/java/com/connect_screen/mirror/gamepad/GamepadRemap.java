package com.connect_screen.mirror.gamepad;

/**
 * Button remapping shared by the remap screen and the virtual gamepad writer.
 *
 * The incoming state comes from Moonlight: digital buttons arrive as a Moonlight button flag
 * mask, the two triggers arrive as separate analog values. Every source button is rewritten to
 * its configured target, so buttons and triggers can be mapped onto each other.
 */
public final class GamepadRemap {

    public static final int A = 0;
    public static final int B = 1;
    public static final int X = 2;
    public static final int Y = 3;
    public static final int LB = 4;
    public static final int RB = 5;
    public static final int LT = 6;
    public static final int RT = 7;
    public static final int LS = 8;
    public static final int RS = 9;
    public static final int SELECT = 10;
    public static final int START = 11;
    public static final int GUIDE = 12;
    public static final int DPAD_UP = 13;
    public static final int DPAD_DOWN = 14;
    public static final int DPAD_LEFT = 15;
    public static final int DPAD_RIGHT = 16;

    public static final int COUNT = 17;

    /** Names shown in the remap screen; the index equals the button id. */
    public static final String[] LABELS = {
            "A", "B", "X", "Y", "LB", "RB", "LT", "RT", "LS", "RS",
            "SELECT", "START", "GUIDE", "上", "下", "左", "右"};

    /** Moonlight button flags; triggers have no flag because they are analog. */
    private static final int[] MASKS = {
            0x1000, 0x2000, 0x4000, 0x8000, 0x0100, 0x0200, 0, 0, 0x0040, 0x0080,
            0x0020, 0x0010, 0x0400, 0x0001, 0x0002, 0x0004, 0x0008};

    /** A digital press aimed at a trigger is sent as a full pull. */
    private static final int FULL_TRIGGER = 255;

    private GamepadRemap() {
    }

    public static boolean isTrigger(int id) {
        return id == LT || id == RT;
    }

    public static int[] identity() {
        int[] mapping = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            mapping[i] = i;
        }
        return mapping;
    }

    /**
     * Rewrites an incoming gamepad state through {@code mapping}. The result is packed as
     * {@code (buttons << 16) | (leftTrigger << 8) | rightTrigger} so the hot input path stays
     * allocation free.
     */
    public static long apply(int[] mapping, int buttons, int leftTrigger, int rightTrigger) {
        int outButtons = 0;
        int outLeft = 0;
        int outRight = 0;
        for (int source = 0; source < COUNT; source++) {
            boolean pressed;
            if (source == LT) {
                pressed = leftTrigger > 0;
            } else if (source == RT) {
                pressed = rightTrigger > 0;
            } else {
                pressed = (buttons & MASKS[source]) != 0;
            }
            if (!pressed) {
                continue;
            }
            int target = (mapping != null && source < mapping.length) ? mapping[source] : source;
            if (target == LT) {
                outLeft = Math.max(outLeft, source == LT ? leftTrigger : FULL_TRIGGER);
            } else if (target == RT) {
                outRight = Math.max(outRight, source == RT ? rightTrigger : FULL_TRIGGER);
            } else if (target >= 0 && target < COUNT) {
                outButtons |= MASKS[target];
            }
        }
        return ((long) outButtons << 16) | ((long) outLeft << 8) | outRight;
    }

    public static int buttonsOf(long packed) {
        return (int) (packed >> 16);
    }

    public static int leftTriggerOf(long packed) {
        return (int) ((packed >> 8) & 0xFF);
    }

    public static int rightTriggerOf(long packed) {
        return (int) (packed & 0xFF);
    }
}
