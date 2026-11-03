package com.poisoncircle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Command-backed controls; the server remains the authority for permissions and results. */
public final class PoisonCircleControlScreen extends Screen {
    private static final int ACCENT = 0xFF2D190F;
    private static final int MUTED = 0xFF3D281B;
    private static final int PANEL = 0xFFF0D9A6;
    private static final int PANEL_DARK = 0xFFE1BC78;
    private static final int BORDER = 0xFF6B4028;
    private static final String[] TABS = {"运行", "阶段", "伤害"};
    private final Map<String, String> values = new HashMap<>();
    private final List<Label> labels = new ArrayList<>();
    private final Map<String, EditBox> fields = new HashMap<>();
    private int tab, left, top, cardWidth;
    private long stopDeadline;
    private Button stopButton;
    private String feedback = "需要管理员权限 · 执行结果见聊天栏";
    private boolean error;
    private boolean settingsDirty;
    private long settingsSaveAt;
    private int shownStage;
    private long stageSwitchAt;

    public PoisonCircleControlScreen() {
        super(Component.literal("毒圈控制台"));
        values.put("initial", ControllerFormSettings.get("initial", "500"));
        values.put("final", "0");
        values.put("round", ControllerFormSettings.get("round", "1"));
        for (int i = 1; i <= 5; i++) {
            values.put("wait." + i, ControllerFormSettings.get("wait." + i, "120"));
            values.put("shrink." + i, ControllerFormSettings.get("shrink." + i, "120"));
            values.put("command." + i, ControllerFormSettings.get("command." + i, ""));
        }
        values.put("wait", values.get("wait.1"));
        values.put("shrink", values.get("shrink.1"));
        values.put("command", values.get("command.1"));
        values.put("base", ControllerFormSettings.get("base", "2"));
        values.put("increment", ControllerFormSettings.get("increment", "2"));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        cardWidth = Math.min(520, width - 16);
        left = (width - cardWidth) / 2;
        top = (height - 224) / 2;
        labels.clear();
        fields.clear();
        stopButton = null;
        int inner = cardWidth - 24;
        int tabWidth = (inner - 8) / 3;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            Button button = button(TABS[i], 12 + i * (tabWidth + 4), 46, tabWidth,
                () -> { tab = index; stopDeadline = 0; rebuildWidgets(); });
            button.active = tab != i;
        }
        button("×", cardWidth - 32, 9, 20, this::onClose);
        if (tab == 0) initRun(inner);
        else if (tab == 1) initStage(inner);
        else initDamage(inner);
    }

    private void initRun(int inner) {
        int third = (inner - 12) / 3;
        field("initial", "初始半径 / 格", 12, 74, third, 32);
        field("final", "最终半径 / 固定 0", 18 + third, 74, third, 1);
        fields.get("final").setEditable(false);
        button("启动毒圈", 24 + third * 2, 85, inner - third * 2 - 12,
            () -> submit("start", values.get("initial"), values.get("final")));
        button("使用当前位置设为中心", 12, 121, inner, () -> submit("center"));
        stopButton = button("停止毒圈", 12, 155, inner, () -> {
            if (System.nanoTime() < stopDeadline) {
                submit("stop");
                stopDeadline = 0;
                stopButton.setMessage(Component.literal("停止毒圈"));
            } else {
                stopDeadline = System.nanoTime() + 5_000_000_000L;
                stopButton.setMessage(Component.literal("再次点击确认停止"));
                feedback = "5 秒内再次点击停止按钮确认";
                error = false;
            }
        });
    }

    private void initStage(int inner) {
        int round = parseRound();
        String waitKey = "wait." + round, shrinkKey = "shrink." + round, commandKey = "command." + round;
        int third = (inner - 12) / 3;
        shownStage = round;
        stageSwitchAt = 0;
        field("round", "阶段 / 1–5", 12, 74, third, 1);
        field(waitKey, "等待 / 秒", 18 + third, 74, third, 10);
        field(shrinkKey, "缩圈 / 秒", 24 + third * 2, 74, inner - third * 2 - 12, 10);
        button("应用阶段时间", 12, 111, inner,
            () -> submit("time", values.get("round"), values.get(waitKey), values.get(shrinkKey)));
        field(commandKey, "第 " + round + " 圈开始时执行的命令", 12, 141, inner, 2048);
        button("保存阶段命令", 12, 175, inner,
            () -> submit("command", values.get("round"), values.get(commandKey)));
    }

    private int parseRound() {
        try { return Math.max(1, Math.min(5, Integer.parseInt(values.get("round")))); }
        catch (RuntimeException exception) { return 1; }
    }

    private void initDamage(int inner) {
        int half = (inner - 6) / 2;
        field("base", "基础伤害 / ≥ 0", 12, 78, half, 32);
        field("increment", "每圈增量 / ≥ 0", 18 + half, 78, inner - half - 6, 32);
        button("应用伤害设置", 12, 121, inner,
            () -> submit("damage", values.get("base"), values.get("increment")));
        labels.add(new Label("每圈增量留空：保留服务器当前增量", 12, 155));
        labels.add(new Label("数值为待提交值，当前配置以服务器为准", 12, 174));
    }

    private void field(String key, String label, int x, int y, int fieldWidth, int maxLength) {
        labels.add(new Label(label, x, y));
        EditBox box = new EditBox(font, left + x + 4, top + y + 17, fieldWidth - 8, 12, Component.literal(label));
        box.setMaxLength(maxLength);
        box.setValue(values.get(key));
        box.setTextColor(ACCENT);
        box.setTextShadow(false);
        box.setBordered(false);
        box.setResponder(value -> {
            values.put(key, value);
            settingsDirty = true;
            settingsSaveAt = System.nanoTime() + 350_000_000L;
            if (key.equals("round") && tab == 1) stageSwitchAt = settingsSaveAt;
        });
        fields.put(key, box);
        addRenderableWidget(box);
    }

    private Button button(String text, int x, int y, int buttonWidth, Runnable action) {
        return addRenderableWidget(new Button(left + x, top + y, buttonWidth, 20,
            Component.literal(text), button -> action.run(), narration -> narration.get()) {
            @Override
            protected void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
                int background = !active ? BORDER : isHoveredOrFocused() ? PANEL_DARK : PANEL;
                gui.fill(getX(), getY(), getRight(), getBottom(), BORDER);
                gui.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, background);
                var label = net.minecraft.locale.Language.getInstance().getVisualOrder(
                    font.substrByWidth(getMessage().copy().withStyle(ChatFormatting.BOLD), getWidth() - 8));
                gui.text(font, label, getX() + (getWidth() - font.width(label)) / 2,
                    getY() + (getHeight() - 8) / 2, active ? ACCENT : PANEL, false);
            }
        });
    }

    private void submit(String action, String... arguments) {
        try {
            String command = buildCommand(action, arguments);
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) throw new IllegalArgumentException("未连接服务器，无法发送命令");
            connection.sendCommand(command);
            feedback = "已发送 /poisoncircle " + action + " · 结果见聊天栏";
            error = false;
        } catch (IllegalArgumentException exception) {
            feedback = exception.getMessage();
            error = true;
        }
    }

    /** Kept independent of widget state so command validation can be checked without opening a screen. */
    static String buildCommand(String action, String... arguments) {
        String suffix = switch (action) {
            case "start" -> {
                double initial = number(arguments[0], "初始半径", 1, 29_999_984);
                yield "start " + decimal(initial) + " 0";
            }
            case "center" -> arguments.length == 0 ? "center" : "center "
                + decimal(number(arguments[0], "X", -29_999_984, 29_999_984)) + " "
                + decimal(number(arguments[1], "Y", -20_000_000, 20_000_000)) + " "
                + decimal(number(arguments[2], "Z", -29_999_984, 29_999_984));
            case "time" -> "time " + integer(arguments[0], "阶段", 1, 5) + " "
                + integer(arguments[1], "等待秒数", 0, Integer.MAX_VALUE / 20) + " "
                + integer(arguments[2], "缩圈秒数", 1, Integer.MAX_VALUE / 20);
            case "command" -> {
                int round = integer(arguments[0], "阶段", 1, 5);
                String command = arguments[1].strip();
                if (command.startsWith("/")) command = command.substring(1).stripLeading();
                if (command.isBlank() || command.length() > 2048 || command.codePoints().anyMatch(Character::isISOControl))
                    throw new IllegalArgumentException("阶段命令须为 1–2048 字符的单行命令");
                yield "command " + round + " " + command;
            }
            case "damage" -> "damage " + decimal(number(arguments[0], "基础伤害", 0, Double.MAX_VALUE))
                + (arguments[1].isBlank() ? "" : " " + decimal(number(arguments[1], "每圈增量", 0, Double.MAX_VALUE)));
            case "stop", "detector" -> action;
            default -> throw new IllegalArgumentException("不支持的毒圈操作");
        };
        return "poisoncircle " + suffix;
    }

    private static double number(String text, String label, double min, double max) {
        try {
            double value = Double.parseDouble(text.strip());
            if (Double.isFinite(value) && value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + "须为范围内的有限数字（" + decimal(min) + " 起）");
    }

    private static int integer(String text, String label, int min, int max) {
        try {
            int value = Integer.parseInt(text.strip());
            if (value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + "须为 " + min + "–" + max + " 的整数");
    }

    private static String decimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void saveFormValues() {
        ControllerFormSettings.save(values);
        settingsDirty = false;
    }

    @Override
    public void onClose() {
        if (settingsDirty) saveFormValues();
        super.onClose();
    }

    private void refreshStageFields() {
        if (tab != 1) return;
        int round = parseRound();
        String[] keys = {"wait." + round, "shrink." + round, "command." + round};
        String[] fallbacks = {"120", "120", ""};
        for (int i = 0; i < keys.length; i++) {
            String value = values.getOrDefault(keys[i], ControllerFormSettings.get(keys[i], fallbacks[i]));
            values.putIfAbsent(keys[i], value);
            EditBox field = fields.get(keys[i]);
            if (field == null) {
                rebuildWidgets();
                return;
            }
            if (!field.isFocused() && !field.getValue().equals(value)) field.setValue(value);
        }
        shownStage = round;
        stageSwitchAt = 0;
    }

    @Override
    public void tick() {
        if (settingsDirty && System.nanoTime() >= settingsSaveAt) saveFormValues();
        if (stageSwitchAt != 0 && System.nanoTime() >= stageSwitchAt && parseRound() != shownStage) refreshStageFields();
        if (stopDeadline != 0 && System.nanoTime() >= stopDeadline) {
            stopDeadline = 0;
            if (stopButton != null) stopButton.setMessage(Component.literal("停止毒圈"));
            feedback = "停止确认已取消";
            error = false;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, width, height, 0xA008101A);
        gui.fill(left, top, left + cardWidth, top + 224, BORDER);
        gui.fill(left + 2, top + 2, left + cardWidth - 2, top + 222, PANEL);
        gui.fill(left + 2, top + 2, left + cardWidth - 2, top + 4, PANEL_DARK);
        gui.fill(left + 12, top + 69, left + cardWidth - 12, top + 70, BORDER);
        gui.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 12, top + 12, ACCENT, false);
        CircleSyncPayload circle = PoisonCircleClient.currentCircle();
        String state = circle == null ? "当前维度 · 未运行" : circle.collapsed() ? "安全区已消失 · 全域伤害"
            : "第 " + circle.round() + "/5 圈 · 半径 " + Math.round(circle.currentRadius()) + " · "
                + (circle.waiting() ? "等待 " : "缩圈 ") + circle.remainingSeconds() + " 秒";
        gui.text(font, font.plainSubstrByWidth(state, cardWidth - 24), left + 12, top + 30, MUTED, false);
        for (Label label : labels)
            gui.text(font, Component.literal(label.text()).withStyle(ChatFormatting.BOLD),
                left + label.x(), top + label.y(), MUTED, false);
        for (var child : children()) {
            if (child instanceof EditBox box) {
                int x = box.getX() - 4, y = box.getY() - 6;
                gui.fill(x, y, x + box.getWidth() + 8, y + 20, box.isFocused() ? ACCENT : BORDER);
                gui.fill(x + 1, y + 1, x + box.getWidth() + 7, y + 19, 0xFFFFEAC4);
            }
        }
        gui.fill(left + 12, top + 202, left + cardWidth - 12, top + 203, BORDER);
        gui.text(font, font.plainSubstrByWidth(feedback, cardWidth - 24), left + 12, top + 210,
            error ? 0xFFA32920 : ACCENT, false);
        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    private record Label(String text, int x, int y) { }
}
