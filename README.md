# MKUXQQ 毒圈

Minecraft 1.21.1 NeoForge 多阶段大逃杀毒圈 Mod。

## 功能

- 最多 5 阶段平滑缩圈；圆心和半径在缩圈时同步平滑移动。
- 第一圈使用管理员设置的中心点；后续安全区保证完整位于上一圈内。
- 从第二圈开始优先选择低海拔、周围平坦且无水/熔岩的位置。
- 圈外直接扣生命值，不消耗护甲；附带红色提示、屏幕闪红与受击抖动。
- 红色原版世界边界风格能量墙，缩圈时与安全区实时同步。
- 安全区探测器：查看当前圈、下一圈和个人提前揭示的下下圈。
- Xaero 小地图与世界地图兼容：当前圈红色、下一圈白色、个人揭示下下圈青蓝色。
- 支持主世界、下界和末地；创造与旁观模式免疫毒圈伤害。

## 指令

所有指令需要管理员权限等级 2。

```mcfunction
/poisoncircle center <x> <y> <z>
/poisoncircle start [初始半径] [最终半径]
/poisoncircle time <圈数> <等待秒数> <缩圈秒数>
/poisoncircle damage <基础伤害> [每圈增加伤害]
/poisoncircle command <圈数> <要执行的指令>
/poisoncircle detector
/poisoncircle status
/poisoncircle stop
```

推荐流程：先用 `/poisoncircle center` 设置第一圈中心，再用 `/poisoncircle start` 启动。

## 安装

1. 使用 Minecraft 1.21.1 与 NeoForge 21.1.234 或兼容版本。
2. 将 Release 中的 Jar 放进 `.minecraft/mods`。
3. 可选安装 Xaero 小地图与世界地图以显示毒圈范围。

作者：MKUXQQ
