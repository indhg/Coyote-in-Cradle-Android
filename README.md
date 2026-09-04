<a id="top"></a>

<div align="center">

<h1>Coyote in Cradle · Android</h1>
<p><strong>AI 角色扮演 × 郊狼（DG-Lab）—— 手机端</strong></p>

<p>
  <a href="https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest"><img alt="Release" src="https://img.shields.io/badge/下载-APK-blue?style=flat-square&logo=android"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-非商用·可分享-green?style=flat-square"></a>
  <img alt="Version" src="https://img.shields.io/badge/版本-v1.1.7-lightgrey?style=flat-square">
</p>

<p>
  <a href="#中文">中文</a> · <a href="#english">English</a>
</p>

<p><sub>18+ · 成人向 · 请在自愿、知情、同意的前提下使用</sub></p>

</div>

---

<a id="中文"></a>

## 🇨🇳 中文

> **一句话**：一个 AI 角色扮演系统——角色会通过手机摄像头观察你、会通过麦克风听你声音，并在你授权后实时控制一台 **DG-Lab 郊狼**主机。完整闭环跑在 **本地 Android App**里：手机 = 角色大脑 = 控制台。
>

---

## ⬇️ 下载

| 端 | 下载 | 说明 |
|---|---|---|
| 📱 **安卓 App（v1.1.7）** | 👉 <https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest> | `Coyote-in-Cradle-*-release.apk`；独立 App，**BLE 直连郊狼**，无需 PC 与中继 |
| 🖥️ **PC 版（Windows）** | 👉 <https://github.com/indhg/AI-for-Coyote/releases/latest> | `Coyote-in-Cradle-setup-v*.exe`（安装版）或免装 zip；扫码配对郊狼，需中继 9998 |
| 🧩 **DLC 内容（R18，自行导入）** | 👉 <https://github.com/indhg/AI-for-Coyote-DLC/releases> | 正式角色稿 zh / en 大包，不随主包分发；下载后在程序内一键导入 |

---

## ✨ 它能做什么

| | 能力 | 说明 |
|---|---|---|
| 🎭 | **AI 角色闭环** | 每轮按「观察 → 描写 → 动作 → 发言」推进；回复为严格 JSON，台词与设备指令分离 |
| 👁️🎤 | **摄像头 / 麦克风观察** | 画面变暗或无人 → 角色逐渐不耐烦再到暴怒；呻吟分级：普通呻吟小幅加码、惨叫立即收敛 |
| 🤖 | **自动运行（Autopilot）** | 不需要打字，按间隔自动循环观察-描写-动作-发言；传感器跟随启停 |
| 🎛️ | **手动控制台** | A/B 双通道独立控制：保持强度、增减、清除、24 种内置波形（可循环）；通道开关与强度上限（1–200，默认 100） |
| 📱 | **手机 BLE 直连郊狼** | v3.0 协议，无需 PC 与中继；打开 App → 设置 → 蓝牙配对即用 |
| 🌐 | **中英一键切换** | 设置里的「界面语言」（跟随系统 / 中文 / English）；与「角色稿语言」联动、也可独立 |
| 🛡️ | **多层安全** | 强度上限、单指令步长 ≤40、蓝牙断连自动清零、急停（长按 1 秒）；过热自动降档 |
| 🧩 | **内容即装即用** | 内置体验版（中英）；正式角色经 DLC 大包在程序内一键导入（见「内容清单」） |
| 🆕 | **v1.1.7 debug 版本号清晰** | 系统应用信息与 APK 文件名都带 `1.1.7-dev.<时间戳>`，一眼分辨测试版与正式版 |

---

## 🧩 内容清单

内容分两档：

- **随包内置**（开箱即玩）：体验版（Trial）——触手 · 纯爱向试玩内容，含中英文稿（设置里切界面语言 + 角色稿语言即可）；
- **R18 正式内容**（不随主包分发，需自行导入）：正式角色稿经 **DLC 大包**导入后出现在角色卡。

| 内容 | 分发形态 | 语言 |
|---|---|---|
| 体验版（触手 · 纯爱向） | ✅ 内置主包（`assets/prompts/触手-角色提示词-纯爱.md` 及 `-EN.md`） | 中文 + 英文 |
| 触手 / 品评会  | DLC-zh 大包（R18，自行导入） | 中文 |
| 上述正式角色稿的英文版 | DLC-en 大包（自行导入） | 英文 |

> **当前正式角色支持**：触手、品评会（已上线）；哥布林、史莱姆、蛛后——**主程序不再提供支持**（已从 DLC-zh 移除相关稿）。入口仍可见但标「不支持」。

> 正式角色稿（`content/roles`）属 R18 内容：**本仓库与本发布包均不含**，请通过 DLC 大包获得并自行导入。

### 安装 DLC（正式角色，R18）

1. 到 **AI-for-Coyote-DLC** 仓库 Releases 下载 `Coyote-in-Cradle-DLC-zh-*.zip`（中文正式角色）或 `Coyote-in-Cradle-DLC-en-*.zip`（英文稿）：
   👉 **<https://github.com/indhg/AI-for-Coyote-DLC/releases>**
2. 打开 App → 设置 → 角色入口卡 → **「导入 DLC（打开文件管理）」**，勾选下载的 zip（可多选，也可只选单个 md）；
3. 导入成功 → 对应入口从「不支持」**点亮**，点角色名选中即可开玩，无需重启 App；
4. 也可以在任何文件管理器 / 浏览器里把 zip / md「分享」给本 App 直接导入。

**运行链路**

```text
Android App（Kotlin/Compose） ⇄ 本地 LLM（OpenAI 兼容）
        ⇄ 手机摄像头 / 麦克风
        ⇄ BLE（v3.0） ⇄ DG-Lab 郊狼主机（A/B 通道）

App 内置：角色运行时 / Safety 安全层 / 设备波形 / 自动运行 / 双语 UI / DLC 导入
```

---

## 🚀 快速开始（Android）

### 方式 A：装 APK（推荐）

1. 到 [Releases](https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest) 下载 `Coyote-in-Cradle-*-release.apk`；
2. 打开手机的「未知来源安装」授权，安装 APK；
3. 打开 App，先去 **设置 → AI 模型** 填 Base URL 与 API Key（或在 `config.yaml` / 环境变量里设），点「测试连接」验证、「保存并生效」即时切换；
4. 设置 → **郊狼** → 蓝牙配对（首次需授权位置 / 蓝牙 / 相机 / 麦克风）；
5. 开聊。

### 方式 B：从源码构建

<details>
<summary><b>环境要求</b></summary>

- Android Studio Hedgehog (2023.1.1) 或更新；
- Android SDK Platform 34、Build-Tools 35.0.0、Platform-Tools；
- JDK 17（Android Studio 自带）；
- 可选：Gradle（项目已带 wrapper）。

</details>

```bash
git clone https://github.com/indhg/Coyote-in-Cradle-Android.git
cd Coyote-in-Cradle-Android
./gradlew.bat :app:assembleDebug        # Windows
# ./gradlew :app:assembleDebug          # macOS / Linux
```

产物：`app/build/outputs/apk/debug/Coyote-in-Cradle-1.1.7-dev.<时间戳>-debug.apk`（debug 版带时间戳，方便区分 dev/正式构建）。

> 不打 release 流程：项目自用侧载 + GitHub Release 由 `tag v*` 触发 `release.yml`（`./gradlew assembleRelease` + 上传 release APK），详见 `.github/workflows/release.yml`。

---

## 🎛️ 使用要点

- **聊天**：聊天栏与 AI 对话；指令先校验后执行，被拒原因（超上限、通道关闭、急停中）会以 ✖ 卡片回显；中英文随界面切换。
- **手动控制**：设置页 → 控制台 A/B 双通道：保持强度 / 增减 / 清除 / 波形（可循环）；配件名与位置可改（敲回车生效）。
- **自动运行**：聊天栏顶部开关；摄像头 / 麦克风跟随启停。
- **急停**：大红按钮，或**长按 1 秒**（松手取消）；急停 = 全通道清零 + 清波形 + 暂停 AI；点「解除急停」恢复。
- **语言设置**：设置里两项——「界面语言」切菜单 / 按钮 / 提示语；「角色稿语言」切 AI 读哪份角色稿（默认跟随界面语言，手动改后独立）。
- **DLC 导入**：设置 → 角色入口卡 → 「导入 DLC（打开文件管理）」；zip / md 均可。
- **强度档**（轻/中/重 = ×0.7 / ×1.0 / ×1.3）：只修正电击输出，与对话内容无关；重启回默认「中」。

---

## 🛡️ 安全设计

一切来源（AI / 手动 / 地牢反馈）的指令都经过 `app/.../Safety.kt`：

1. 每通道独立强度上限（默认 100），AI / 手动都超不过；
2. 单条指令强度变化 ≤ 40，防跳变；
3. 波形 / 临时强度到点自动归零；持续波形仅在明确持有命令下保持；
4. 设备过热 → 该通道上限临时降为 20；
5. 蓝牙断开 → 自动清零；
6. 急停 = 清零 + 清波形 + 暂停循环；
7. 使用注意：电极不可跨心脏、不可置于颈部以上；不同配件分别调低对应上限。

---

## 🤖 AI 指令协议

模型每次回复为严格 JSON：

```json
{
  "line": "角色台词（原样显示）",
  "actions": [
    { "op": "temp_strength", "channel": "A", "value": 60, "duration_s": 3 },
    { "op": "add_strength",  "channel": "B", "delta": 10 },
    { "op": "pulse",         "channel": "A", "pattern": "短促连击", "duration_s": 2 },
    { "op": "clear",         "channel": "B" },
    { "op": "stop" }
  ]
}
```

操作代码名不变，界面显示名中英文随语言切换（详见英文版 §AI 指令协议）。

---

## ❓ 常见问题

- **正式角色是灰的、点不了？** = 该角色内容包未导入；或主程序不再支持该角色（见「内容清单」表格）。去 DLC 仓库下对应语言包，按上面「导入 DLC」导入即点亮。
- **观察行是灰的？** = 未授权相机 / 麦克风。不给授权也能玩，但没有观察反馈。
- **界面切了 English 但角色还是灰的？** = 该语言的角色稿没装（中文角色灰 = 装 DLC-zh；英文灰 = 装 DLC-en）。
- **导入 zip 提示失败？** = 确认是现行包（`content/roles/<角色>-角色提示词.md`，无 `-调教` 后缀），旧 legacy 包不兼容。
- **需要电脑 / 中继吗？** 不需要，手机 BLE 直连郊狼（v3.0）；PC 版才需要中继 + 扫码。
- **debug APK 装不上后调试**：debug 版带 `1.1.7-dev.<时间戳>`，与 release `1.1.7` 区分清楚；如旧版覆盖装不上去，先卸载再装。
- **「内容稿未安装…」弹窗**：当前角色内容未导入或主程序不再支持，按上面 FAQ 处理。

---


## ⚠️ 免责声明

本项目仅供**成年用户**在**自愿、知情、同意**的前提下用于个人娱乐。请：

- 遵守所在地区法律法规；
- 评估自身身体状况——心脏病、心脏起搏器等风险人群请勿使用；
- 控制强度与时长，随时可用急停中断（长按 1 秒）；
- 使用本项目造成的一切后果由使用者自行承担。

---

## 📜 许可证

- 本仓库为作者原创，采用**「可分享 · 禁商用」许可**（见 [LICENSE](LICENSE)）：允许免费使用、修改与自由转发分享（保留许可声明与作者署名），**禁止任何盈利目的的分发、转售与收费**；商用授权请联系作者；
- 设备协议部分参考 DG-LAB 公开 BLE 协议文档，与上游设备方无附属关系；
- 角色稿与主题包内容版权归作者，仅通过作者渠道获得并自行导入；本 GitHub 仓库托管代码主体，不含任何内容文件。

## 🙏 致谢与联系

- 反馈 / 建议 / 报错 → [issues](https://github.com/indhg/AI-for-Coyote/issues)
- 作者推特：<https://x.com/cinnanirch>
- 配套 PC 版（Windows）：[AI-for-Coyote](https://github.com/indhg/AI-for-Coyote)
- 配套 DLC 仓：[AI-for-Coyote-DLC](https://github.com/indhg/AI-for-Coyote-DLC)

[回到顶部](#top)

---

<a id="english"></a>

## 🇬🇧 English

> **In one sentence:** an AI role-playing system in which the character watches through your phone's camera, listens through the microphone, and drives a **Coyote (DG-Lab)** device in real time — a full "observe → describe → act → speak" loop inside a **standalone Android app**.
>
> **What makes it more than a chatbot:** the actions are real. The model emits structured device commands that pass through `Safety.kt` first (intensity caps / step limits / overheat protection / auto-clear on disconnect) — only validated commands touch the hardware.

---

## ⬇️ Download

| Platform | Download | Notes |
|---|---|---|
| 📱 **Android app (v1.1.7)** | 👉 <https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest> | `Coyote-in-Cradle-*-release.apk`; standalone app, **BLE direct to the Coyote** — no PC, no relay |
| 🖥️ **PC (Windows)** | 👉 <https://github.com/indhg/AI-for-Coyote/releases/latest> | `Coyote-in-Cradle-setup-v*.exe` (installer) or the portable zip; scan-to-pair, needs a relay on 9998 |
| 🧩 **DLC content (R18, install yourself)** | 👉 <https://github.com/indhg/AI-for-Coyote-DLC/releases> | Official-character zh / en packs, not shipped with the main build; import in-app |

---

## ✨ Highlights

| | Feature | What it does |
|---|---|---|
| 🎭 | **AI role loop** | Each round: observe → describe → act → speak; replies are strict JSON, dialogue separated from device commands |
| 👁️🎤 | **Camera / mic sensing** | Dark or empty frame → impatience, then anger; moans are graded — ordinary ones nudge intensity up, screams pull it straight down |
| 🤖 | **Autopilot** | Runs the whole loop on an interval without typing; sensors follow it |
| 🎛️ | **Manual console** | Independent A/B channels: hold, adjust, clear, 24 built-in waveforms (loopable); per-channel on/off and caps (1–200, default 100) |
| 📱 | **BLE direct to the Coyote** | v3.0 protocol over the phone's Bluetooth — no PC, no relay required |
| 🌐 | **One-tap bilingual UI** | UI language (System / 中文 / English) plus a separate script-language switch (follows UI by default) |
| 🛡️ | **Layered safety** | Caps, ≤40 step changes, overheat limit drop, auto-clear on Bluetooth disconnect, emergency stop (1s hold) |
| 🧩 | **Content, ready to load** | Trial version built in (ZH + EN); official characters installed in-app from DLC packs (see below) |
| 🆕 | **Clear debug versioning (v1.1.7)** | System "About" screen and APK filename show `1.1.7-dev.<timestamp>` for debug, plain `1.1.7` for release — instantly distinguishable |

---

## 🧩 Content

Content ships in two tiers:

- **Built into the main package** (ready to play): the Trial version — Tentacle · pure-love sample, with both Chinese and English scripts (use the UI / script language switches in Settings);
- **R18 official content** (not shipped; install it yourself): official character scripts arrive via **DLC packs** and appear on the character card once installed.

| Content | Distribution | Language |
|---|---|---|
| Trial (Tentacle · pure-love sample) | ✅ Built in (`assets/prompts/触手-角色提示词-纯爱.md` and `-EN.md`) | Chinese + English |
| Tentacle / Appraisal (official scripts) | DLC-zh pack (R18, install it yourself) | Chinese |
| English versions of the official scripts | DLC-en pack (install it yourself) | English |

> **Current official-role support:** Tentacle and Appraisal are supported. Goblin, Slime and Arachne are **no longer supported by the main app** (their scripts have been removed from DLC-zh) — entries stay visible but read "Not supported".

> Official scripts (`content/roles`) are R18 material: **neither this repo nor the main release contains them** — get them from the author's DLC packs and install them yourself.

### Installing DLC (official characters, R18)

1. Grab `Coyote-in-Cradle-DLC-zh-*.zip` (Chinese official characters) or `Coyote-in-Cradle-DLC-en-*.zip` (English scripts) from the **AI-for-Coyote-DLC** repo Releases:
   👉 **<https://github.com/indhg/AI-for-Coyote-DLC/releases>**
2. Open the app → Settings → role card → **"Import DLC (open file manager)"** and pick the zip (multi-select allowed; single .md works too);
3. On success the entry lights up from "Not supported" — tap the role name to select it; no restart needed;
4. You can also "Share" the zip/.md to this app from any file manager or browser.

**Runtime chain**

```text
Android app (Kotlin/Compose) ⇄ local LLM (OpenAI-compatible)
        ⇄ phone camera / microphone
        ⇄ BLE (v3.0) ⇄ DG-Lab Coyote device (A/B channels)

The app also owns: character runtime · Safety layer · device waveforms · autopilot · bilingual UI · DLC import
```

---

## 🚀 Quick start (Android)

### Option A — Install APK (recommended)

1. Grab `Coyote-in-Cradle-*-release.apk` from [Releases](https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest);
2. Enable "Install from unknown sources" for your browser / file manager, then install the APK;
3. Open the app, go to **Settings → AI model**, fill in the Base URL and API key (or via `config.yaml` / env var), tap "Test Connection" then "Save & Apply" — takes effect immediately;
4. Settings → **Coyote** → Bluetooth pair (you'll be prompted for Location / Bluetooth / Camera / Microphone the first time);
5. Start chatting.

### Option B — Build from source

<details>
<summary><b>Requirements</b></summary>

- Android Studio Hedgehog (2023.1.1) or newer;
- Android SDK Platform 34, Build-Tools 35.0.0, Platform-Tools;
- JDK 17 (bundled with Android Studio);
- Gradle (the project ships a wrapper).

</details>

```bash
git clone https://github.com/indhg/Coyote-in-Cradle-Android.git
cd Coyote-in-Cradle-Android
./gradlew.bat :app:assembleDebug        # Windows
# ./gradlew :app:assembleDebug          # macOS / Linux
```

Output: `app/build/outputs/apk/debug/Coyote-in-Cradle-1.1.7-dev.<timestamp>-debug.apk` (the debug suffix keeps dev builds visually separate from release builds).

> The release pipeline is tag-driven: `git tag v1.1.x` triggers `.github/workflows/release.yml` to run `./gradlew assembleRelease` and upload the release APK to the GitHub Release.

---

## 🎛️ Usage notes

- **Chat**: dialogue on the chat panel; commands are validated first — rejected reasons (over cap, channel off, E-Stop…) come back as ✖ cards, bilingual with the UI.
- **Manual control**: Settings → Console → A/B channels: hold / adjust / clear / waveforms; rename accessories and locations (press Enter to apply).
- **Autopilot**: switch at the top of the chat panel; camera / mic follow it.
- **E-Stop**: the big red button, or **hold for 1 second** (release to cancel). It clears every channel, clears waveforms, and pauses the AI loop.
- **Language settings**: two switches in Settings — **UI language** flips menus / buttons / toasts; **Script language** flips which role script the AI reads (follows UI by default, can be unlinked).
- **DLC import**: Settings → role card → "Import DLC (open file manager)"; zip / md both accepted.
- **Intensity levels** (Tender / Dominant / Rough = ×0.7 / ×1.0 / ×1.3): only scale the shock output, independent of the story; resets to "Dominant" on restart.

---

## 🛡️ Safety design

Every command — from AI, manual control, or dungeon feedback — goes through `app/.../Safety.kt`:

1. Independent per-channel intensity caps (default 100) that no source can exceed;
2. Intensity changes per command ≤ 40 (no jumps);
3. Waveforms / temporary intensity auto-zero when done;
4. Overheating drops that channel's cap to 20 temporarily;
5. Bluetooth disconnect clears everything;
6. E-Stop = clear + zero + pause;
7. Never place electrodes across the heart or above the neck; lower caps per accessory.

---

## 🤖 AI command protocol

```json
{
  "line": "Character dialogue (shown as-is)",
  "actions": [
    { "op": "temp_strength", "channel": "A", "value": 60, "duration_s": 3 },
    { "op": "add_strength",  "channel": "B", "delta": 10 },
    { "op": "pulse",         "channel": "A", "pattern": "短促连击", "duration_s": 2 },
    { "op": "clear",         "channel": "B" },
    { "op": "stop" }
  ]
}
```

Command names never change; their display labels on the chips are localized (`values/strings.xml` + `values-en/strings.xml`).

---

## ❓ FAQ

- **Official roles greyed out?** Their content pack isn't imported — or the role is no longer supported by the main app (see "Content" table). Get the matching language pack and import it via "Import DLC".
- **Observer line greyed?** Camera / mic permission missing. You can still play, but there is no observation feedback.
- **Switched UI to English but roles still grey?** The English scripts aren't installed — import DLC-en.
- **Zip import failed?** Make sure it's a current pack (`content/roles/<role>-角色提示词.md`, no `-调教` suffix); legacy packs are rejected.
- **Do I need a PC or relay?** No — BLE connects straight to the Coyote (v3.0) from the phone. Only the PC build needs the relay on 9998.
- **Debug vs release confusion?** Debug APKs (and their system "About" entry) show `1.1.7-dev.<timestamp>`; release shows plain `1.1.7`. If a debug APK refuses to install, uninstall the old build first.
- **"Content script not installed…" toast?** The role's content isn't imported, or the role is no longer supported — handle it as in the FAQ entries above.

---

## ⚠️ Disclaimer

For **adults only**, for personal entertainment under **voluntary, informed, and consensual** conditions. Follow local law; do not use with heart disease, pacemakers, or similar risks; control intensity and duration and use the E-Stop freely (hold for 1s); all consequences of use are the user's own.

---

## 📜 License

- The repository is the author's original work under a **"Free-Share, Non-Commercial" license** (see [LICENSE](LICENSE)): free personal use, modification and redistribution are allowed as long as this license notice and the author credit are kept; **any profit-driven distribution, resale or paid hosting is prohibited**. Commercial licensing — contact the author;
- Device protocol section references the public DG-LAB BLE documentation; this project is not affiliated with the device vendor;
- Character scripts and theme-pack content belong to the author and are obtained only through the author's channels, then imported by yourself; this GitHub repo hosts the code base and contains no content files.

## 🙏 Thanks & contact

- Bugs / ideas → GitHub [Issues](https://github.com/indhg/AI-for-Coyote/issues)
- Author on X (Twitter): <https://x.com/cinnanirch>
- Companion PC build (Windows): [AI-for-Coyote](https://github.com/indhg/AI-for-Coyote)
- Companion DLC repo: [AI-for-Coyote-DLC](https://github.com/indhg/AI-for-Coyote-DLC)

[Back to top](#top)