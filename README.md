# 狂扁小朋友 × Minecraft：双引擎桥接原理示例

从本地开发工程中分离的源码示例，重点展示 **原版 Flash 与真实 Minecraft 同时运行，按系统分配权威，再合成画面**。不是用网页重写 Minecraft，也不是只给原角色换一张史蒂夫贴图。

本目录与原项目独立，原目录未移动、未删除。移除了本机绝对路径启动器、缓存、日志、历史测试结果、原版 SWF 和运行存档。导入资源后可重建桥接版 SWF；MC 依赖通过 Gradle 获取。

## 从哪里开始读

- [架构与权威划分](docs/架构原理.md)
- [接口与坐标映射](docs/桥接协议.md)
- [依赖、资源导入与运行](docs/运行说明.md)
- [演示步骤与验证范围](docs/验证与限制.md)
- [第三方资源与许可边界](THIRD_PARTY_NOTICES.md)

## 核心文件

| 文件 | 作用 |
|---|---|
| `brawler.as` | 原游戏场景、敌人、碰撞、受击事件与状态回写 |
| `patch_swf.py` | 从使用者提供的原版反编译脚本生成补丁 |
| `web/index.html` | 输入路由、轮询、Flash/MC 画面合成 |
| `web/motion.js` | 位置和画面帧对齐、平滑 |
| `fabric-mod/src/client/java/com/example/client/ExampleModClient.java` | HTTP 桥接、主循环、坐标和场景管理 |
| `fabric-mod/src/main/java/com/example/StreetCombat.java` | 原游戏敌人的 MC 战斗适配 |
| `fabric-mod/src/client/java/com/example/client/StreetTargets.java` | 原生物品使用时的街道目标查询 |
| `fabric-mod/src/client/java/com/example/client/BridgeImages.java` / `SceneOverlay.java` | 玩家、HUD、背包及实体/方块原生画面导出 |
| `manage.py` | 无本机固定路径的资源导入及运行辅助 |

## 快速检查

安装 Python 3.10+ 后，在本目录执行：

```powershell
python manage.py check
```

源码检查可在未导入游戏资源时通过，输出会明确提示资源尚未准备。完整运行步骤见运行说明。

## 发布定位

这是保留原有适配能力的单游戏技术示例，尚未把各模块进一步抽成通用 SDK。当前分离工作没有重新启动游戏或重新验证全部功能；原工程的历史验证不能视为此示例在所有机器上的保证。MC 1.20.1 / Fabric Loader 0.16.14 / Fabric API 0.92.2+1.20.1 / Fabric Loom 1.6.12 / Gradle 8.6 / Java 17。

本示例未选定自有代码开源许可证。上传公开仓库前由项目所有者确定；第三方资源不自动继承自有代码许可。
