<a id="top"></a>

<div align="center">
  <h1>Coyote in Cradle · Android</h1>
  <p>AI × 郊狼角色扮演 App</p>
  <p><a href="#中文">中文</a> · <a href="#english">English</a></p>
</div>

<a id="中文"></a>

## 中文

AI 扮演主题角色（**触手**／**品评会**〔DLC〕，档位：纯爱／调教／凌辱），用手机摄像头 + 麦克风观察玩家反应，**BLE 直连** DG-Lab 郊狼 3.0 设备（无需电脑、无需中继）。

## ⬇️ 下载安装包（Android）

下载请点这里喵：

👉 <https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest>

**本项目为作者原创，采用「可分享 · 禁商用」许可（见 [LICENSE](LICENSE)）：允许免费转发分享与个人使用，**禁止任何盈利目的的分发、转售与收费**；商用授权请联系作者。** 作者推特主页欢迎来支持喵～<https://x.com/cinnanirch>

## 📦 相关仓库

本项目是**多端应用**。除本仓库（安卓端）外，还有两个配套仓库：

| 仓库 | 地址 |
|---|---|
| 🖥️ **PC 主仓库** | [indhg/AI-for-Coyote](https://github.com/indhg/AI-for-Coyote) |
| 🧪 **DLC 拓展仓库** | [indhg/AI-for-Coyote-DLC](https://github.com/indhg/AI-for-Coyote-DLC) |

## 🚀 快速上手

### 1. 下载安装

1. 到 [Release 页](https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest) 下载最新的 `Coyote-in-Cradle-<版本>-release.apk`；
2. 手机上打开安装；提示「未知来源」时允许该来源即可（自用侧载包，未上商店）。

### 2. 三步开始玩

1. **填写 API Key**：聊天页右上角「设置」→粘贴你的 DeepSeek API Key →可点「测试连接」验证；其余先保持默认。
   - 使用中转站时：Base URL 填中转站地址、Key 填中转站的（**官方与中转站密钥不通用**）；对话抽风可把设置里的「JSON 模式」关掉（部分中转站不支持，程序有兜底解析）。
2. **连接郊狼**：设置页点「连接郊狼」——郊狼开机并靠近手机，在列表中点击信号最强的那个。首次建议先在官方郊狼 App 里配对一次设备（配对后手机会记住它，本 App 优先按名字直连）。
   - ⚠️ 请不要在官方 App 已连接郊狼时使用本 App：郊狼同一时间只能连接一个控制端。
3. **回聊天页开聊**：自动运行默认开启，AI 用摄像头 + 麦克风观察你的反应，实时输出台词并控制设备。断开／退出会立即清零强度（安全兜底）。

### 3. 安装 DLC

1. 到 [DLC 仓库](https://github.com/indhg/AI-for-Coyote-DLC) Release 页下载 `Coyote-in-Cradle-DLC1.zip`（触手·调教）和／或 `Coyote-in-Cradle-DLC2.zip`（品评会·调教）；
2. 设置页 →主题卡 →「导入 DLC（打开文件管理）」→在文件管理器里勾选一个或多个 zip／md →确定；
3. 导入成功后，对应主题／档位即点亮，切换即可使用。也可以在任何文件管理器／浏览器里把 zip／md「分享」给本 App，直接导入。

### 4. 常见问题

- **扫描列表全是「无名」？** 正常。点击信号最强的设备试连；连过一次后会记住地址，下次自动直连。
- **观察行是灰的？** 给 App 授权相机 + 麦克风（不给也能玩，但是没有观察反馈）。
- **连不上？** 先断开官方郊狼 App，再重开郊狼电源试一次。
- **只有设备动作没有台词／报错**：设置页查看「测试连接」结果；提示密钥无效 = Base URL 与 Key 不配套；提示参数不支持 = 核对模型名或关闭「JSON 模式」。
- **更新**：设置页「更新」区自动检查新版本（可关闭）；发现新版本后点击即可去 GitHub 下载新 APK。

[回到顶部](#top)

<a id="english"></a>

## English

An AI role-playing app where the AI plays a themed character（**Tentacles**／**Appraisal Event**〔DLC〕; tiers: Pure Love／Training／Humiliation）, observes the player through the phone camera and microphone, and connects **directly over BLE** to a DG-Lab Coyote 3.0 device（no computer or relay required）.

## ⬇️ Download（Android）

Download the latest APK here:

👉 <https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest>

**This project is the author's original work under a “Free-Share, Non-Commercial” license (see [LICENSE](LICENSE)): free sharing and personal use are allowed — any profit-driven distribution, resale or paid hosting is prohibited. For commercial licensing, contact the author.** Welcome to support the author on Twitter: <https://x.com/cinnanirch>

## 📦 Related repositories

This is a **multi-platform application**. Alongside this Android repository, there are two companion repositories:

| Repository | Link |
|---|---|
| 🖥️ **PC repository** | [indhg/AI-for-Coyote](https://github.com/indhg/AI-for-Coyote) |
| 🧪 **DLC repository** | [indhg/AI-for-Coyote-DLC](https://github.com/indhg/AI-for-Coyote-DLC) |

## 🚀 Quick start

### 1. Download and install

1. Open the [Release page](https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest) and download the latest `Coyote-in-Cradle-<version>-release.apk`.
2. Open it on the phone and install it. If Android asks for permission for an “unknown source”, allow that source（this is a personal sideloaded package and is not published in an app store）.

### 2. Start playing in three steps

1. **Enter the API Key**: open “Settings” in the upper-right corner of the chat page, paste your DeepSeek API Key, and optionally tap “Test Connection”. Keep the other values at their defaults for now.
   - With a relay provider: enter the provider’s Base URL and its key（**official and relay-provider keys are not interchangeable**）. If chat behaves strangely, turn off “JSON Mode” in Settings; some relay providers do not support it and the app has a fallback parser.
2. **Connect the Coyote**: tap “Connect Coyote” in Settings. Turn on the Coyote, keep it near the phone, and tap the device with the strongest signal in the list. It is recommended to pair the device once in the official Coyote App first（the phone remembers it, and this app prefers a direct connection by name afterward）.
   - ⚠️ Do not use this app while the official App is connected to the Coyote: the Coyote can have only one control client at a time.
3. **Return to chat**: autopilot is enabled by default. The AI observes your reactions through the camera and microphone, outputs dialogue, and controls the device in real time. Disconnecting or exiting immediately clears intensity as a safety fallback.

### 3. Install DLC

1. Open the [DLC repository](https://github.com/indhg/AI-for-Coyote-DLC) Release page and download `Coyote-in-Cradle-DLC1.zip`（Tentacles · Training）and／or `Coyote-in-Cradle-DLC2.zip`（Appraisal Event · Training）.
2. In Settings, choose a theme card → “Import DLC（Open File Manager）” → select one or more zip／md files in the file manager → confirm.
3. After a successful import, the corresponding theme／tier lights up and can be selected. You can also share a zip or md file to this app from any file manager or browser to import it directly.

### 4. FAQ

- **The scan list only shows “Unnamed” devices?** That is normal. Try the device with the strongest signal; after the first connection, its address is remembered and direct connection is attempted automatically next time.
- **The observation row is grey?** Grant the app camera and microphone permissions（the app still works without them, but there is no observation feedback）.
- **Cannot connect?** Disconnect the official Coyote App first, then restart the Coyote and try again.
- **Device actions appear but there is no dialogue／an error appears**: check the result under “Test Connection” in Settings. An invalid-key message means the Base URL and Key do not match; an unsupported-parameter message means you should check the model name or turn off “JSON Mode”.
- **Updates**: the “Updates” section in Settings checks for new versions automatically（can be disabled）. Tap a discovered update to download the new APK from GitHub.

[Back to top](#top)
