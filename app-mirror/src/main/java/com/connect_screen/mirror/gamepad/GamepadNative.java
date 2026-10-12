package com.connect_screen.mirror.gamepad;

/**
 * Minimal uinput backend derived from GKME (GPL-3.0-or-later).
 * It is loaded and called exclusively by the Shizuku UserService process.
 */
public final class GamepadNative {
    static {
        System.loadLibrary("gkme_uinput");
    }

    private GamepadNative() {
    }

    public static native int create(int triggerMode);

    public static native void write(int fd, int triggerMode, int buttons, int leftTrigger, int rightTrigger,
                                    int leftX, int leftY, int rightX, int rightY);

    public static native void destroy(int fd);
}
