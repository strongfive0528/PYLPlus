/*
 * Minimal Xbox-style uinput gamepad backend derived from GKME's
 * android/src/main/cpp/uinput_gamepad.c (GPL-3.0-or-later).
 * Copyright (C) 2026 GKME contributors.
 */
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/input.h>

#define UINPUT_IOCTL_BASE 'U'
#define UI_DEV_CREATE_ _IO(UINPUT_IOCTL_BASE, 1)
#define UI_DEV_DESTROY_ _IO(UINPUT_IOCTL_BASE, 2)
#define UI_SET_EVBIT_ _IOW(UINPUT_IOCTL_BASE, 100, int)
#define UI_SET_KEYBIT_ _IOW(UINPUT_IOCTL_BASE, 101, int)
#define UI_SET_ABSBIT_ _IOW(UINPUT_IOCTL_BASE, 103, int)

struct cs_input_id { uint16_t bustype, vendor, product, version; };
struct cs_uinput_setup { struct cs_input_id id; char name[80]; uint32_t ff_effects_max; };
struct cs_abs_setup { uint16_t code; struct input_absinfo absinfo; };
#define UI_DEV_SETUP_ _IOW(UINPUT_IOCTL_BASE, 3, struct cs_uinput_setup)
#define UI_ABS_SETUP_ _IOW(UINPUT_IOCTL_BASE, 4, struct cs_abs_setup)

/* Standard Linux gamepad codes, interpreted by Android's Generic.kl. */
static const int key_codes[] = {BTN_SOUTH, BTN_EAST, BTN_NORTH, BTN_WEST,
    BTN_TL, BTN_TR, BTN_SELECT, BTN_START, BTN_MODE, BTN_THUMBL, BTN_THUMBR};
static const int key_masks[] = {0x1000, 0x2000, 0x4000, 0x8000, 0x0100, 0x0200, 0x0020, 0x0010, 0x0400, 0x0040, 0x0080};
static const int axes[] = {ABS_X, ABS_Y, ABS_Z, ABS_RZ, ABS_BRAKE, ABS_GAS, ABS_HAT0X, ABS_HAT0Y};
static const int min_values[] = {-32768, -32768, -32768, -32768, 0, 0, -1, -1};
static const int max_values[] = {32767, 32767, 32767, 32767, 255, 255, 1, 1};

static int clamp16(int v) { return v < -32768 ? -32768 : (v > 32767 ? 32767 : v); }
static int clamp8(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
/* GameStream stick Y is positive upward; Android's ABS_Y/ABS_RZ is negative upward. */
static int android_stick_y(int v) {
    int clamped = clamp16(v);
    return clamped == -32768 ? 32767 : -clamped;
}

JNIEXPORT jint JNICALL Java_com_connect_1screen_mirror_gamepad_GamepadNative_create(JNIEnv *env, jclass clazz, jint mode) {
    int fd = open("/dev/uinput", O_RDWR | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) return -errno;
    if (ioctl(fd, UI_SET_EVBIT_, EV_KEY) < 0 || ioctl(fd, UI_SET_EVBIT_, EV_ABS) < 0) goto fail;
    for (int i = 0; i < 11; i++) if (ioctl(fd, UI_SET_KEYBIT_, key_codes[i]) < 0) goto fail;
    if (mode != 1 && (ioctl(fd, UI_SET_KEYBIT_, BTN_TL2) < 0 ||
                      ioctl(fd, UI_SET_KEYBIT_, BTN_TR2) < 0)) goto fail;
    for (int i = 0; i < 8; i++) {
        if (mode == 0 && (i == 4 || i == 5)) continue;
        struct cs_abs_setup axis; memset(&axis, 0, sizeof(axis));
        axis.code = axes[i]; axis.absinfo.minimum = min_values[i]; axis.absinfo.maximum = max_values[i];
        if (ioctl(fd, UI_ABS_SETUP_, &axis) < 0 || ioctl(fd, UI_SET_ABSBIT_, axes[i]) < 0) goto fail;
    }
    struct cs_uinput_setup setup; memset(&setup, 0, sizeof(setup));
    /* Use our own identity so Android selects Generic.kl. The Xbox 02fd layout
       has analog trigger axes but does not map BTN_TL2/BTN_TR2 at all. */
    setup.id.bustype = 0x0006; setup.id.vendor = 0x4c53; setup.id.product = 0x0001; setup.id.version = 1;
    strncpy(setup.name, "Connect Screen Virtual Gamepad", sizeof(setup.name) - 1);
    if (ioctl(fd, UI_DEV_SETUP_, &setup) < 0 || ioctl(fd, UI_DEV_CREATE_) < 0) goto fail;
    return fd;
fail: { int error = errno; close(fd); return -error; }
}

JNIEXPORT void JNICALL Java_com_connect_1screen_mirror_gamepad_GamepadNative_write(JNIEnv *env, jclass clazz, jint fd, jint mode, jint buttons, jint lt, jint rt, jint lx, jint ly, jint rx, jint ry) {
    struct input_event events[22]; memset(events, 0, sizeof(events)); int n = 0;
    for (int i = 0; i < 11; i++) { events[n].type = EV_KEY; events[n].code = key_codes[i]; events[n++].value = (buttons & key_masks[i]) != 0; }
    if (mode != 1) {
        events[n].type = EV_KEY; events[n].code = BTN_TL2; events[n++].value = lt > 0;
        events[n].type = EV_KEY; events[n].code = BTN_TR2; events[n++].value = rt > 0;
    }
    int values[] = {clamp16(lx), android_stick_y(ly), clamp16(rx), android_stick_y(ry), clamp8(lt), clamp8(rt), ((buttons >> 3) & 1) - ((buttons >> 2) & 1), ((buttons >> 1) & 1) - (buttons & 1)};
    for (int i = 0; i < 8; i++) {
        if (mode == 0 && (i == 4 || i == 5)) continue;
        events[n].type = EV_ABS; events[n].code = axes[i]; events[n++].value = values[i];
    }
    events[n].type = EV_SYN; events[n].code = SYN_REPORT; n++;
    const char *p = (const char *) events; size_t left = (size_t)n * sizeof(*events);
    while (left) { ssize_t written = write(fd, p, left); if (written < 0) { if (errno == EINTR) continue; break; } p += written; left -= (size_t)written; }
}

JNIEXPORT void JNICALL Java_com_connect_1screen_mirror_gamepad_GamepadNative_destroy(JNIEnv *env, jclass clazz, jint fd) {
    if (fd >= 0) { ioctl(fd, UI_DEV_DESTROY_); close(fd); }
}
