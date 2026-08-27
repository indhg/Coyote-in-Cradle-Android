# AI for Coyote（安卓端）

纯安卓、零电脑依赖的 AI 郊狼角色扮演 App。AI 扮演「触手」，用手机摄像头 + 麦克风观察玩家反应，通过内嵌中继控制 DG-Lab 郊狼设备（官方郊狼 App 蓝牙驱动）。

## 状态

- [x] M0：聊天气泡 + 设置页 + DeepSeek 调用（reasoning 回退 + 4 轮重试）+ 游戏循环骨架 + 安全层
- [x] M1：设备闭环 = **BLE 直连郊狼 3.0**（官方蓝牙协议 coyote/v3，B0 帧 100ms 流式下发；中继方案已废弃，relay/ 目录保留未用）
- [x] M2：摄像头分级 + 麦克风音量分级接入循环（待真机验证）
- [ ] M3：双通道保底 / 断线重连 / DLC 分包 / APK 打包

## 构建

- Android SDK（compileSdk 35）+ JDK 17+
- `./gradlew assembleDebug` 出调试包；`assembleRelease` 出正式包
- 依赖走阿里云镜像（settings.gradle.kts），无需梯子

## 结构

```
app/src/main/java/com/indhg/aiforcoyote/
├── MainActivity.kt       入口（聊天 ⇄ 设置）
├── MainViewModel.kt      状态中枢：设置/消息/循环/安全
├── data/Settings.kt      DataStore 设置（API Key/昵称/风格/自动运行）
├── llm/DeepSeekClient.kt OpenAI 兼容客户端（vision 多段格式 + 推理泄漏回退）
├── llm/SystemPrompt.kt   系统提示词构建（角色提示词 assets + 设备映射注入）
├── game/Safety.kt        安全层（上限/步长/过热/清零）
├── game/BleCoyote.kt     BLE 直连设备驱动（B0/BF 指令、100ms 流式循环，官方 V3 协议）
├── game/DeviceOps.kt     设备指令接口（M0 Noop / M1 BleCoyote）
└── ui/                   聊天页 / 设置页 / 暗金主题
```

## 内容与许可

- 内置纯爱版提示词（assets/prompts/）；调教版作为 DLC 分包（M3）
- 18+ 虚构、双方自愿；成人内容仅限侧载分发
- 桌面版主仓库：github.com/indhg/AI-for-Coyote；DLC 仓库：github.com/indhg/AI-for-Coyote-DLC
