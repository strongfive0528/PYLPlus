package com.connect_screen.mirror.gamepad;

import android.os.RemoteException;

import com.connect_screen.mirror.State;
import com.connect_screen.mirror.Pref;
import com.connect_screen.mirror.shizuku.IUserService;

/** App-process side of the gamepad bridge. It never accesses /dev/uinput. */
public final class GamepadBridge {
    private GamepadBridge() {
    }

    public static boolean create() {
        IUserService service = State.userService;
        if (service == null) return false;
        try {
            return service.createGamepad(Pref.getGamepadTriggerMode());
        } catch (RemoteException e) {
            State.log("创建虚拟手柄失败: " + e.getMessage());
            return false;
        }
    }

    public static boolean update(int buttons, int leftTrigger, int rightTrigger,
                                 int leftX, int leftY, int rightX, int rightY) {
        IUserService service = State.userService;
        if (service == null) return false;
        try {
            return service.updateGamepad(buttons, leftTrigger, rightTrigger,
                    leftX, leftY, rightX, rightY);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static void release() {
        IUserService service = State.userService;
        if (service == null) return;
        try {
            service.releaseGamepad();
        } catch (RemoteException ignored) {
        }
    }
}
