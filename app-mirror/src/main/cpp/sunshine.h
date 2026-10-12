#pragma once
#include "stream.h"
#include "moonlight-common-c/src/input.h"

namespace sunshine_callbacks {
    void callJavaOnPinRequested();
    void captureVideoLoop(void *channel_data, safe::mail_t mail, const video::config_t& config, const audio::config_t& audioConfig);
    void captureAudioLoop(void *channel_data, safe::mail_t mail, const audio::config_t& config);
    void callJavaOnTouch(SS_TOUCH_PACKET* touchPacket);
    void callJavaOnAbsMouseMove(NV_ABS_MOUSE_MOVE_PACKET* packet);
    void callJavaOnMouseButton(std::uint8_t button, bool release);
    void callJavaOnConnectScreenClientDiscovered(std::string connectScreenClient);
    void callJavaSetConnectScreenServerUuid(std::string uuid);
    void callJavaOnKeyboard(uint16_t modcode, bool release, uint8_t flags);
}

extern "C" {
namespace sunshine_callbacks {
    bool createGamepad();
    bool updateGamepad(std::uint32_t buttons, std::uint8_t leftTrigger, std::uint8_t rightTrigger,
                       std::int16_t leftX, std::int16_t leftY, std::int16_t rightX, std::int16_t rightY);
    void releaseGamepad();
}
}
