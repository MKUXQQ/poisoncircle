package com.poisoncircle;

import java.math.BigDecimal;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lobby controls submit normal permission-checked commands. */
public final class LobbyControlScreen extends Screen {
    private static final int INK = 0xFF2D190F, BORDER = 0xFF6B4028, PANEL = 0xFFF0D9A6, HOVER = 0xFFE1BC78;
    private String radius = ControllerFormSettings.get("lobby.radius", "32");
    private String feedback = "需要管理员权限 · 执行结果见聊天栏";
    private int left, top, cardWidth;
    private EditBox radiusBox;
    private boolean dirty, error;
    private long saveAt;

    public LobbyControlScreen() { super(Component.literal("大厅控制器")); }
    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        cardWidth = Math.min(400, width - 16);
        left = (width - cardWidth) / 2;
        top = (height - 230) / 2;
        radiusBox = new EditBox(font, left + 18, top + 71, cardWidth - 36, 12, Component.literal("大厅半径"));
        radiusBox.setMaxLength(32);
        radiusBox.setBordered(false);
        radiusBox.setTextColor(INK);
        radiusBox.setTextShadow(false);
        radiusBox.setValue(radius);
        radiusBox.setResponder(value -> { radius = value; dirty = true; saveAt = System.nanoTime() + 350_000_000L; });
        addRenderableWidget(radiusBox);
        button("使用当前位置设置大厅", 97, () -> submit("set"));
        button("关闭大厅保护", 124, () -> submit("disable"));
        button("查询大厅状态", 151, () -> submit("status"));
        button("关闭界面", 178, this::onClose);
    }

    private void button(String text, int y, Runnable action) {
        addRenderableWidget(new Button(left + 12, top + y, cardWidth - 24, 20, Component.literal(text),
            button -> action.run(), narration -> narration.get()) {
            @Override protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
                gui.fill(getX(), getY(), getRight(), getBottom(), BORDER);
                gui.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, isHoveredOrFocused() ? HOVER : PANEL);
                var label = net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(), getWidth() - 8));
                gui.text(font, label, getX() + (getWidth() - font.width(label)) / 2, getY() + 6, INK, false);
            }
        });
    }

    static String buildCommand(String action, String radius) {
        return switch (action) {
            case "set" -> {
                double number;
                try { number = Double.parseDouble(radius.strip()); }
                catch (NumberFormatException exception) { throw new IllegalArgumentException("半径须为 1–4096 的有限数字"); }
                if (!Double.isFinite(number) || number < 1 || number > 4096)
                    throw new IllegalArgumentException("半径须为 1–4096 的有限数字");
                yield "poisoncircle lobby set " + BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
            }
            case "disable", "status" -> "poisoncircle lobby " + action;
            default -> throw new IllegalArgumentException("不支持的大厅操作");
        };
    }

    private void submit(String action) {
        try {
            String command = buildCommand(action, radius);
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) throw new IllegalArgumentException("未连接服务器");
            connection.sendCommand(command);
            feedback = "已发送大厅命令 · 结果见聊天栏";
            error = false;
        } catch (IllegalArgumentException exception) {
            feedback = exception.getMessage();
            error = true;
        }
    }

    private void save() { ControllerFormSettings.save(Map.of("lobby.radius", radius)); dirty = false; }
    @Override public void onClose() { if (dirty) save(); super.onClose(); }
    @Override public void tick() { if (dirty && System.nanoTime() >= saveAt) save(); }

    @Override public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, width, height, 0xA008101A);
        gui.fill(left, top, left + cardWidth, top + 230, BORDER);
        gui.fill(left + 2, top + 2, left + cardWidth - 2, top + 228, PANEL);
        gui.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 12, top + 12, INK, false);
        gui.text(font, "同维度全高度 · 进入大厅即退出本局并免毒", left + 12, top + 30, INK, false);
        gui.text(font, "大厅半径 / 1–4096 格", left + 12, top + 51, INK, false);
        gui.fill(left + 12, top + 65, left + cardWidth - 12, top + 89, BORDER);
        gui.fill(left + 13, top + 66, left + cardWidth - 13, top + 88, 0xFFFFEAC4);
        gui.text(font, font.plainSubstrByWidth(feedback, cardWidth - 24), left + 12, top + 211, error ? 0xFF9B1717 : INK, false);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }
}
