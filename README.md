# 屏易连（Connect Screen）

语言 / Language：[中文](#中文) ｜ [English](#english)

---

## 中文

**目录**

- [功能概述](#功能概述)
- [相对上游的改动](#相对上游的改动)
- [系统要求](#系统要求)
- [安装](#安装)
- [权限说明](#权限说明)
- [使用说明](#使用说明)
- [已知限制](#已知限制)
- [从源码构建](#从源码构建)
- [版权与致谢](#版权与致谢)
- [免责声明](#免责声明)

本仓库是 Gitee 项目「安卓屏连 / connect-screen」的衍生版本，在其基础上增加了虚拟手柄相关功能，并调整了部分界面与设置项。原始项目版权归原作者所有，来源说明与致谢见下文「版权与致谢」。

### 功能概述

本应用将 Android 设备的画面通过有线或无线方式输出到外接显示器、电视或电脑，并在投屏过程中提供输入、音频与显示参数的调节能力。主要功能如下。

1. 有线直连显示。通过 USB 或 DisplayLink 扩展坞输出画面；在系统未开放桌面模式时，借助权限实现双屏异显。
2. 无线投屏。内置与 GameStream / Moonlight 兼容的服务端，同一局域网内的 Moonlight 客户端可以连接本机进行串流。
3. 输入转发。将 Moonlight 客户端的手柄、键盘与鼠标输入转发到本机。
4. 虚拟手柄。在拥有 Shizuku 权限时创建虚拟手柄设备，使客户端手柄输入在本机可被识别为标准的按键与轴事件。
5. 音频控制。可选择由客户端或本机播放声音。
6. 单应用投屏、触控板与触摸屏、熄屏、自动旋转、自动缩放去黑边、分辨率与 DPI 调整等辅助功能。

### 相对上游的改动

1. 新增虚拟手柄支持。在已授予 Shizuku 权限的前提下，通过 `/dev/uinput` 创建 Xbox 风格的虚拟手柄，将客户端手柄输入映射为 Android 可识别的按键与轴事件。相关实现参考自 GKME，详见文末致谢。
2. 新增 LT/RT（L2/R2）触发模式设置，可在以下三种模式中选择。

   | 模式 | 行为 |
   | --- | --- |
   | 开关触发（兼容优先） | 仅发送按键按下与松开事件 |
   | 线性扳机（保留力度） | 仅发送 0 至 255 的力度数值 |
   | 同时输出（按键 + 力度） | 同时发送按键事件与力度数值 |

3. 新增音频播放位置设置。可选择遵循客户端请求、始终由客户端播放、始终由本机播放。
4. 新增「声音采集方式」设置。可在自动、强制使用 REMOTE_SUBMIX、强制使用系统内录之间选择，默认使用系统内录。
5. 设置页新增「设置生效提醒」区块与「应用设置并重启」按钮。
6. 界面按 Material 设计风格重新排版。功能入口与操作逻辑保持不变。
7. 修正左右摇杆上下方向与客户端相反的问题。

### 系统要求

| 项目 | 要求 |
| --- | --- |
| 系统版本 | Android 9 及以上（API 28+） |
| 处理器架构 | arm64-v8a 或 armeabi-v7a |
| 虚拟手柄功能 | 需安装并运行 Shizuku，并授予本应用权限 |
| 无线投屏 | 手机与客户端位于同一局域网 |
| 有线直连 | 设备支持 USB Host 或 DisplayLink |

### 安装

1. 在仓库的 Releases 页面下载安装包，文件名为 `ConnectScreen-<版本号>-release.apk`，例如 `ConnectScreen-1.7.2-release.apk`。
2. 在设备上允许安装来自未知来源的应用后安装该文件。
3. 安装包同时包含 arm64-v8a 与 armeabi-v7a 两种架构的原生库。

安装包说明：

- Release 安装包使用发布签名密钥，与调试版本（debug）的签名不同。若设备上已安装调试版本，需先卸载调试版本再安装本安装包；卸载会清除应用数据。

### 权限说明

| 权限 | 用途 | 是否必需 |
| --- | --- | --- |
| 屏幕录制（MediaProjection） | 捕获屏幕内容用于串流 | 必需 |
| 录音（RECORD_AUDIO） | 采集系统音频并发送给客户端 | 需要转发音频时必需；不授权时串流很可能没有声音 |
| 通知（POST_NOTIFICATIONS） | 显示前台服务通知 | 必需 |
| 悬浮窗（SYSTEM_ALERT_WINDOW） | 显示悬浮返回按钮等控件 | 可选 |
| 无障碍服务（Accessibility Service） | 在副屏模拟触摸事件 | 使用触摸相关功能时必需 |
| Shizuku | 执行需要提权的系统操作，包括创建虚拟手柄 | 使用虚拟手柄等功能时必需 |
| USB Host | 有线直连外接显示设备 | 使用有线直连时必需 |

权限均通过系统界面或 Shizuku 授权，应用不会绕过系统权限限制。

### 使用说明

#### 1. 首次使用

1. 启动应用并按界面提示授予屏幕录制、通知等权限。
2. 若需要使用虚拟手柄或系统级操作，安装并启动 Shizuku，然后在应用内完成授权。
3. 在设置页确认投屏、显示与输入相关参数。

#### 2. 无线连接 Moonlight 客户端

1. 确保手机与客户端处于同一局域网。
2. 应用启动后会在网络中发布服务，在 Moonlight 客户端中搜索并连接。
3. 连接建立后，客户端的键盘、鼠标与手柄输入将转发到本机。

#### 3. 虚拟手柄与扳机模式

1. 在设置页的「Moonlight 手柄 LT/RT（L2/R2）触发模式」中选择所需模式。
2. 若目标游戏仅识别按键，选择「开关触发」；若需要读取按压力度，选择「线性扳机」；若目标游戏同时接受两种输入，可选择「同时输出」。
3. 更改模式后需重新建立串流连接，新的虚拟手柄设备才会按新能力创建。

#### 4. 音频播放位置

1. 设置页中「忽略客户端的音频播放位置请求」开关关闭时，声音播放位置遵循客户端请求。
2. 打开该开关后，可选择「始终在 Moonlight 客户端播放」或「始终在手机上播放」。
3. 该设置在新的串流会话创建时读取，修改后需重新连接。

#### 5. 声音采集方式

本应用有两种采集系统声音的实现，设置页的「声音采集方式」用于指定使用哪一种。

| 选项 | 说明 | 前提条件 |
| --- | --- | --- |
| 自动 | Shizuku 用户服务就绪时使用 REMOTE_SUBMIX，否则使用系统内录 | 无 |
| 强制使用 REMOTE_SUBMIX | 始终使用 REMOTE_SUBMIX 采集 | 需要 Shizuku，且系统版本为 Android 12 及以上 |
| 强制使用系统内录 | 始终使用系统内录采集（默认） | 需要麦克风权限 |

每次建立会话时，应用内日志会写明实际采用的采集方式。若所选路径当前不可用（例如 Shizuku 用户服务尚未就绪），日志还会记录原因与回退结果。当串流没有声音时，可切换到另一种采集方式后重新连接。

#### 6. 设置生效方式

设置项在应用运行期间即被写入，但涉及服务与串流的设置需要重新启动服务才会生效。设置页顶部的「设置生效提醒」区块提供「应用设置并重启」按钮，点击并确认后应用将结束当前服务并自动重新启动。执行该操作会断开当前串流，重新启动时可能需要重新授权屏幕录制。

### 已知限制

1. 虚拟手柄当前仅支持单个手柄设备。
2. 虚拟手柄的可用性取决于设备是否允许通过 Shizuku 访问 `/dev/uinput`，部分机型或系统版本可能不可用。
3. 「同时输出（按键 + 力度）」模式下，少数游戏可能将同一次扳机操作处理两次，如出现该情况请改用另外两种模式之一。
4. 强制由客户端播放音频时，若系统录音权限或音频捕获失败，声音仍可能无法按预期输出；该设置不会绕过系统权限。
5. 部分厂商系统限制了桌面模式或外接显示能力，实际可用范围取决于设备。

### 从源码构建

环境要求：JDK 17、Android SDK（Platform 34）、Android NDK 28.0.13004108、CMake 3.31.6。

```powershell
$env:JAVA_HOME = "<JDK 17 路径>"
$env:ANDROID_SDK_ROOT = "<Android SDK 路径>"
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT

# 调试版本
.\gradlew.bat :app-mirror:assembleDebug

# 发布版本
.\gradlew.bat :app-mirror:assembleRelease
```

发布版本的签名配置从仓库根目录的 `keystore.properties` 读取；该文件不纳入版本控制。文件内容格式如下。

```properties
storeFile=<keystore 文件路径>
storePassword=<store 密码>
keyAlias=<密钥别名>
keyPassword=<密钥密码>
```

若该文件不存在，构建将输出未签名的安装包，需要自行签名后安装。

### 版权与致谢

#### 1. 许可证

本项目依据 [GNU General Public License v3.0](LICENSE) 发布。原始项目中已经包含 GPL 声明与版权信息的源文件保持原样，不因本仓库的修改而变更其授权条款。

#### 2. 上游项目

| 项目 | 地址 | 说明 |
| --- | --- | --- |
| 安卓屏连 / connect-screen | https://gitee.com/connect-screen/connect-screen | 本仓库的上游项目，提供投屏、串流与显示相关功能的原始实现 |
| connect-screen.com | https://github.com/taowen/connect-screen.com | 用户手册站点 |

上游项目版权归其原作者所有。本仓库的衍生改动仅涉及上文「相对上游的改动」中列出的内容。

#### 3. 虚拟手柄实现来源

| 项目 | 地址 | 说明 |
| --- | --- | --- |
| GKME（Gamepad Emu） | https://github.com/4zyz4/GKME | 本仓库虚拟手柄功能中的 uinput 实现参考该项目，遵循 GNU General Public License v3.0 或更高版本，版权归 4zyz4 所有 |

#### 4. 第三方组件

| 组件 | 地址 | 用途 |
| --- | --- | --- |
| moonlight-common-c | https://github.com/moonlight-stream/moonlight-common-c | GameStream 协议实现 |
| Simple-Web-Server | https://gitlab.com/eidheim/Simple-Web-Server | HTTP 与 HTTPS 服务端 |
| nanors | https://github.com/sleepybishop/nanors | Reed-Solomon 纠错 |
| openssl-cmake | https://github.com/viaduck/openssl-cmake | OpenSSL 构建脚本 |
| libwordexp | https://github.com/leleliu008/libwordexp | wordexp 实现 |
| termux-x11 | https://github.com/termux/termux-x11 | X11 相关组件 |
| Shizuku | https://github.com/RikkaApps/Shizuku | 提权服务框架 |
| libsu | https://github.com/topjohnwu/libsu | root 相关支持 |
| hiddenapibypass | https://github.com/LSPosed/AndroidHiddenApiBypass | 隐藏 API 调用 |
| ZXing | https://github.com/zxing/zxing | 二维码生成与解析 |
| jmDNS | https://github.com/jmdns/jmdns | 局域网服务发现 |
| NanoHTTPD | https://github.com/NanoHttpd/nanohttpd | 内嵌 HTTP 服务 |
| Bouncy Castle | https://github.com/bcgit/bc-java | 加密算法实现 |

#### 5. 参考资料

* http://nightmare.press/
* https://github.com/eiyooooo/Easycontrol
* https://github.com/eiyooooo/Easycontrol_For_Car
* https://github.com/timschneeb/awesome-shizuku
* https://github.com/Genymobile/scrcpy
* https://github.com/AkiChase/scrcpy-mask/blob/master/README-zh.md
* https://github.com/dkrivoruchko/ScreenStream
* https://github.com/sdex/ActivityManager
* https://github.com/farmerbb/SecondScreen
* https://github.com/jiuqianyuan/gkd/blob/main/app/src/main/kotlin/li/songe/gkd/shizuku/UserService.kt
* https://github.com/Live-Block/Flyme-FreeForm/blob/flyme/app/src/main/java/com/sunshine/freeform/ui/freeform/FreeformService.kt
* https://github.com/MagicianGuo/Android-SettingTools
* https://github.com/jiesou/Android-Screener
* https://github.com/itzuo/SceenLive/blob/master/app/src/main/java/com/zxj/screenlive/VideoCodec.java
* https://github.com/WuDi-ZhanShen/AndroidUHidPureJava
* https://github.com/WuDi-ZhanShen/ScreenOff
* https://github.com/keymapperorg/KeyMapper/
* https://www.cnblogs.com/zuojie
* https://blog.csdn.net/liaosongmao1/article/details/136129774

#### 6. DisplayLink 商标声明

本应用不是 DisplayLink 官方应用。

本应用使用了 DisplayLink 的驱动程序（.so 文件）用于支持 DisplayLink 设备的连接功能。DisplayLink 是 Synaptics Incorporated 的注册商标。本应用仅将该驱动程序用于实现与 DisplayLink 设备的兼容性，未对驱动程序进行任何修改。

- DisplayLink 驱动程序的所有权利均属于 Synaptics Incorporated。
- 本应用仅将 DisplayLink 驱动用于其预期用途，即支持 DisplayLink 设备的连接。
- 用户在使用 DisplayLink 相关功能时应遵守 Synaptics Incorporated 的相关许可条款。
- 本应用与 Synaptics Incorporated 没有任何官方关联，不代表或暗示与 Synaptics Incorporated 存在任何合作关系。

如有任何与 DisplayLink 相关的法律问题，请直接联系 Synaptics Incorporated：www.synaptics.com

### 免责声明

本应用按现状提供，不对可用性、稳定性或特定用途的适用性作出任何担保。使用本应用需要修改或绕过部分系统限制，可能导致设备异常、数据丢失或保修失效，相关风险由使用者自行承担。

[↑ 返回顶部](#中文)

---

## English

**Contents**

- [Overview](#overview)
- [Changes Relative to Upstream](#changes-relative-to-upstream)
- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Permissions](#permissions)
- [Usage](#usage)
- [Known Limitations](#known-limitations)
- [Building from Source](#building-from-source)
- [Copyright and Credits](#copyright-and-credits)
- [Disclaimer](#disclaimer)

This repository is a derivative version of the Gitee project "安卓屏连 / connect-screen". It adds virtual gamepad support and modifies some interface elements and settings. Copyright of the original project belongs to its original authors; see "Copyright and Credits" below for attribution.

Note: the application interface is currently in Chinese only.

### Overview

This application outputs the Android device screen to an external monitor, television or computer, over a wired or wireless connection, and provides adjustment of input, audio and display parameters during a session. Its main features are listed below.

1. Wired display output. Outputs the screen through USB or a DisplayLink dock. When the system does not expose a desktop mode, it uses privileged access to provide dual-display output.
2. Wireless streaming. Includes a server compatible with GameStream / Moonlight. A Moonlight client on the same local network can connect to this device for streaming.
3. Input forwarding. Forwards gamepad, keyboard and mouse input from the Moonlight client to this device.
4. Virtual gamepad. Creates a virtual gamepad device when Shizuku permission is granted, so that client gamepad input is recognized on this device as standard key and axis events.
5. Audio control. Selects whether sound is played by the client or by this device.
6. Auxiliary features: single-application streaming, touchpad and touchscreen, screen off, automatic rotation, automatic scaling to remove black borders, and resolution and DPI adjustment.

### Changes Relative to Upstream

1. Added virtual gamepad support. When Shizuku permission has been granted, an Xbox-style virtual gamepad is created through `/dev/uinput`, mapping client gamepad input to key and axis events recognized by Android. The implementation references GKME; see the credits below.
2. Added the LT/RT (L2/R2) trigger mode setting. Three modes are available.

   | Mode | Behavior |
   | --- | --- |
   | Switch trigger (compatibility first) | Sends key press and release events only |
   | Analog trigger (preserve pressure) | Sends the 0 to 255 pressure value only |
   | Combined output (key and pressure) | Sends both key events and the pressure value |

3. Added the audio playback position setting. The options are: follow the client request, always play on the client, always play on this device.
4. Added the audio capture method setting. The options are automatic, always use REMOTE_SUBMIX, and always use in-app capture. The default is in-app capture.
5. Added the "settings take effect" notice block and the "apply settings and restart" button to the settings screen.
6. Rearranged the interface in Material design style. Feature entry points and interaction logic are unchanged.
7. Fixed the inverted up/down direction of both analog sticks relative to the client.

### System Requirements

| Item | Requirement |
| --- | --- |
| OS version | Android 9 or later (API 28+) |
| CPU architecture | arm64-v8a or armeabi-v7a |
| Virtual gamepad | Shizuku must be installed, running, and granted to this application |
| Wireless streaming | Device and client on the same local network |
| Wired display output | Device supports USB Host or DisplayLink |

### Installation

1. Download the package from the Releases page of this repository. The file name is `ConnectScreen-<version>-release.apk`, for example `ConnectScreen-1.7.2-release.apk`.
2. Allow installation from unknown sources on the device, then install the file.
3. The package contains native libraries for both arm64-v8a and armeabi-v7a.

Notes on the package:

- The release package is signed with the release key, which differs from the debug signing key. If a debug build is already installed on the device, uninstall it before installing this package. Uninstalling removes application data.

### Permissions

| Permission | Purpose | Required |
| --- | --- | --- |
| Screen capture (MediaProjection) | Captures the screen for streaming | Yes |
| Record audio (RECORD_AUDIO) | Captures system audio and sends it to the client | Required for audio forwarding; without it the stream is likely to have no sound |
| Notifications (POST_NOTIFICATIONS) | Displays the foreground service notification | Yes |
| Overlay (SYSTEM_ALERT_WINDOW) | Displays floating controls | Optional |
| Accessibility service | Simulates touch events on the secondary display | Required for touch features |
| Shizuku | Performs privileged system operations, including creating the virtual gamepad | Required for the virtual gamepad and similar features |
| USB Host | Wired connection to an external display device | Required for wired display output |

All permissions are granted through the system interface or Shizuku. The application does not bypass system permission restrictions.

### Usage

#### 1. First use

1. Start the application and grant the screen capture and notification permissions as prompted.
2. If the virtual gamepad or other system-level operations are needed, install and start Shizuku, then grant permission inside the application.
3. Check the streaming, display and input settings on the settings screen.

#### 2. Connecting a Moonlight client over the network

1. Make sure the device and the client are on the same local network.
2. After the application starts, it publishes its service on the network. Search for and connect to it from the Moonlight client.
3. Once connected, keyboard, mouse and gamepad input from the client is forwarded to this device.

#### 3. Virtual gamepad and trigger modes

1. Select the required mode in "Moonlight 手柄 LT/RT（L2/R2）触发模式" on the settings screen.
2. If the target application recognizes key events only, select the switch trigger mode. If it reads trigger pressure, select the analog trigger mode. If it accepts both inputs, the combined output mode can be used.
3. After changing the mode, the streaming connection must be re-established so that a new virtual gamepad device is created with the new capabilities.

#### 4. Audio playback position

1. When the "ignore the audio playback position request from the client" switch is off, the playback position follows the client request.
2. When the switch is on, select either "always play on the Moonlight client" or "always play on the phone".
3. This setting is read when a new streaming session is created, so reconnection is required after changing it.

#### 5. Audio capture method

The application has two implementations for capturing system audio. The "audio capture method" setting selects which one is used.

| Option | Description | Requirement |
| --- | --- | --- |
| Automatic | Uses REMOTE_SUBMIX when the Shizuku user service is ready, otherwise uses in-app capture | None |
| Always use REMOTE_SUBMIX | Always captures through REMOTE_SUBMIX | Shizuku, and Android 12 or later |
| Always use in-app capture | Always uses in-app capture (default) | Microphone permission |

Each session records the capture method that was actually used in the in-application log. When the selected method is unavailable, for example because the Shizuku user service is not ready yet, the log also records the reason and the fallback. If a stream has no sound, switch to the other capture method and reconnect.

#### 6. How settings take effect

Settings are written while the application is running, but settings that affect the service and the streaming session take effect only after the service is restarted. The "settings take effect" block at the top of the settings screen provides an "apply settings and restart" button. After it is pressed and confirmed, the application stops the current service and restarts automatically. This disconnects the current streaming session, and the screen capture permission may be requested again after the restart.

### Known Limitations

1. The virtual gamepad supports a single gamepad device at present.
2. The virtual gamepad depends on whether the device allows access to `/dev/uinput` through Shizuku. It may be unavailable on some device models or system versions.
3. In the combined output mode, a small number of applications may process a single trigger action twice. If this happens, use one of the other two modes.
4. When audio playback on the client is forced, sound may still fail to be output as expected if the recording permission or audio capture fails. This setting does not bypass system permissions.
5. Some vendor systems restrict the desktop mode or external display capability. The actual usable range depends on the device.

### Building from Source

Requirements: JDK 17, Android SDK (Platform 34), Android NDK 28.0.13004108, CMake 3.31.6.

```powershell
$env:JAVA_HOME = "<path to JDK 17>"
$env:ANDROID_SDK_ROOT = "<path to Android SDK>"
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT

# Debug build
.\gradlew.bat :app-mirror:assembleDebug

# Release build
.\gradlew.bat :app-mirror:assembleRelease
```

The release signing configuration is read from `keystore.properties` in the repository root. That file is not part of version control. Its format is shown below.

```properties
storeFile=<path to the keystore file>
storePassword=<store password>
keyAlias=<key alias>
keyPassword=<key password>
```

If the file is absent, the build produces an unsigned package, which must be signed before installation.

### Copyright and Credits

#### 1. License

This project is released under the [GNU General Public License v3.0](LICENSE). Source files that already contain GPL notices and copyright information in the original project are kept unchanged, and their licensing terms are not altered by the modifications in this repository.

#### 2. Upstream projects

| Project | Address | Description |
| --- | --- | --- |
| 安卓屏连 / connect-screen | https://gitee.com/connect-screen/connect-screen | Upstream project of this repository; provides the original implementation of the streaming, casting and display features |
| connect-screen.com | https://github.com/taowen/connect-screen.com | User manual site |

Copyright of the upstream project belongs to its original authors. The modifications in this repository are limited to the items listed in "Changes Relative to Upstream" above.

#### 3. Source of the virtual gamepad implementation

| Project | Address | Description |
| --- | --- | --- |
| GKME (Gamepad Emu) | https://github.com/4zyz4/GKME | The uinput implementation of the virtual gamepad feature references this project. It is released under the GNU General Public License v3.0 or later, copyright 4zyz4 |

#### 4. Third-party components

| Component | Address | Purpose |
| --- | --- | --- |
| moonlight-common-c | https://github.com/moonlight-stream/moonlight-common-c | GameStream protocol implementation |
| Simple-Web-Server | https://gitlab.com/eidheim/Simple-Web-Server | HTTP and HTTPS server |
| nanors | https://github.com/sleepybishop/nanors | Reed-Solomon error correction |
| openssl-cmake | https://github.com/viaduck/openssl-cmake | OpenSSL build scripts |
| libwordexp | https://github.com/leleliu008/libwordexp | wordexp implementation |
| termux-x11 | https://github.com/termux/termux-x11 | X11 related components |
| Shizuku | https://github.com/RikkaApps/Shizuku | Privileged service framework |
| libsu | https://github.com/topjohnwu/libsu | Root related support |
| hiddenapibypass | https://github.com/LSPosed/AndroidHiddenApiBypass | Hidden API access |
| ZXing | https://github.com/zxing/zxing | QR code generation and parsing |
| jmDNS | https://github.com/jmdns/jmdns | Local network service discovery |
| NanoHTTPD | https://github.com/NanoHttpd/nanohttpd | Embedded HTTP server |
| Bouncy Castle | https://github.com/bcgit/bc-java | Cryptographic algorithms |

#### 5. References

* http://nightmare.press/
* https://github.com/eiyooooo/Easycontrol
* https://github.com/eiyooooo/Easycontrol_For_Car
* https://github.com/timschneeb/awesome-shizuku
* https://github.com/Genymobile/scrcpy
* https://github.com/AkiChase/scrcpy-mask/blob/master/README-zh.md
* https://github.com/dkrivoruchko/ScreenStream
* https://github.com/sdex/ActivityManager
* https://github.com/farmerbb/SecondScreen
* https://github.com/jiuqianyuan/gkd/blob/main/app/src/main/kotlin/li/songe/gkd/shizuku/UserService.kt
* https://github.com/Live-Block/Flyme-FreeForm/blob/flyme/app/src/main/java/com/sunshine/freeform/ui/freeform/FreeformService.kt
* https://github.com/MagicianGuo/Android-SettingTools
* https://github.com/jiesou/Android-Screener
* https://github.com/itzuo/SceenLive/blob/master/app/src/main/java/com/zxj/screenlive/VideoCodec.java
* https://github.com/WuDi-ZhanShen/AndroidUHidPureJava
* https://github.com/WuDi-ZhanShen/ScreenOff
* https://github.com/keymapperorg/KeyMapper/
* https://www.cnblogs.com/zuojie
* https://blog.csdn.net/liaosongmao1/article/details/136129774

#### 6. DisplayLink trademark notice

This application is not an official DisplayLink application.

This application uses the DisplayLink driver (.so files) to support connection of DisplayLink devices. DisplayLink is a registered trademark of Synaptics Incorporated. The driver is used only to provide compatibility with DisplayLink devices, and no modification of the driver has been made.

- All rights to the DisplayLink driver belong to Synaptics Incorporated.
- This application uses the DisplayLink driver only for its intended purpose, namely supporting the connection of DisplayLink devices.
- Users of DisplayLink related features must comply with the relevant license terms of Synaptics Incorporated.
- This application has no official association with Synaptics Incorporated and does not represent or imply any cooperative relationship with Synaptics Incorporated.

For any legal question related to DisplayLink, contact Synaptics Incorporated directly: www.synaptics.com

### Disclaimer

This application is provided as is, without warranty of any kind regarding availability, stability, or fitness for a particular purpose. Using this application requires modifying or bypassing certain system restrictions, which may cause device malfunction, data loss, or voiding of the warranty. All such risks are borne by the user.

[↑ Back to top](#english)
