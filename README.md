# Coyote in Cradle（安卓端）

AI×郊狼角色扮演 App。AI 扮演「触手」，用手机摄像头 + 麦克风观察玩家反应，**BLE 直连** DG-Lab 郊狼 3.0 设备。

## ⬇️ 下载安装包（Android）

下载请点这里喵：

👉 <https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest>

## 📦 相关仓库

本项目是**多端应用**，除本仓库（安卓端）外还有两个配套仓库：

| 仓库 | 地址 |
|---|---|
| 🖥️ **PC 主仓库** | [indhg/AI-for-Coyote](https://github.com/indhg/AI-for-Coyote) |
| 🧪 **DLC 拓展仓库** | [indhg/AI-for-Coyote-DLC](https://github.com/indhg/AI-for-Coyote-DLC) |

## 🚀 快速上手

### 1. 下载安装

1. 到 [Release 页](https://github.com/indhg/Coyote-in-Cradle-Android/releases/latest) 下载最新 `Coyote-in-Cradle-<版本>-release.apk`；
2. 手机上打开安装；提示「未知来源」时允许该来源即可（自用侧载包，未上商店）。

### 2. 三步开始玩

1. **填 API Key**：聊天页右上角「设置」→ 粘贴你的 DeepSeek API Key → 可点「测试连接」验证；其余先保持默认。
2. **连接郊狼**：设置页点「连接郊狼」——郊狼开机并靠近手机，扫描列表里点信号最强的那个。首次建议先在官方郊狼 App 里配对一次设备（配对后手机会记住它，本 App 优先按名字直连）。
   ⚠️ 请不要在官方 App 已连接郊狼时使用本 App：郊狼同一时间只能连一个控制端。
3. **回聊天页开聊**：自动运行默认开启，AI 用摄像头 + 麦克风观察你的反应，实时输出台词并控制设备。断开/退出会立即清零强度（安全兜底）。

### 3. 安装DLC

1. 到 [DLC 仓库](https://github.com/indhg/AI-for-Coyote-DLC) Release 页下载 `Coyote-in-Cradle-DLC1.zip`；
2. 设置页 →「角色设置」里点「调教」→「导入调教版（选择 .zip 或 .md）」→ 选刚下载的 zip；
3. 导入成功「调教」即点亮，切换即用。zip 里须含「触手-角色提示词-调教.md」（官方 Release 包已含）。

### 4. 常见问题

- **扫描列表全是「无名」？** 正常。点信号最强的试连；连过一次会记住地址，下次自动直连。
- **观察行是灰的？** 给 App 授权相机 + 麦克风（不给也能玩，但是没有观察反馈）。
- **连不上？** 先断开官方郊狼 App，再重开郊狼电源试一次。
