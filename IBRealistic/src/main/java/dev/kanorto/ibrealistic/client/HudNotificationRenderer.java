package dev.kanorto.ibrealistic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Unified HUD notification system rendered below the crosshair.
 * Supports timed notifications with fade-out and persistent damage status bars.
 * Client-side only — no server dependencies.
 */
public class HudNotificationRenderer {

    // ─── LAYOUT CONSTANTS ───
    private static final int CROSSHAIR_OFFSET_Y = 15;
    private static final int LINE_SPACING = 10;
    private static final long FADE_DURATION_MS = 500;

    // ─── BAR DIMENSIONS ───
    private static final int BAR_WIDTH = 30;
    private static final int BAR_HEIGHT = 6;
    private static final int BAR_GAP = 4;
    private static final int REPAIR_BAR_WIDTH = BAR_WIDTH * 2;

    // ─── COLORS ───
    private static final int COLOR_GREEN = 0xFF55FF55;
    private static final int COLOR_YELLOW = 0xFFFFFF55;
    private static final int COLOR_RED = 0xFFFF5555;
    private static final int COLOR_BLUE = 0xFF5555FF;
    private static final int COLOR_BAR_BG = 0xFF333333;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_SERVICE = 0xFF55FFFF;
    private static final float TEMP_THRESHOLD_LOW = 1f / 3f;
    private static final float TEMP_THRESHOLD_HIGH = 2f / 3f;
    private static final int COLOR_SEPARATOR = 0xFF666666;

    // ─── NOTIFICATION STATE ───
    private static final ConcurrentLinkedDeque<TimedNotification> notifications = new ConcurrentLinkedDeque<>();
    private static volatile DamageHudState damageHud = null;

    // ─── INNER CLASSES ───

    private static final class TimedNotification {
        final String text;
        final int color;
        final long expireTimeMs;

        TimedNotification(String text, int color, long durationMs) {
            this.text = text;
            this.color = color;
            this.expireTimeMs = System.currentTimeMillis() + durationMs;
        }
    }

    private static final class DamageHudState {
        final float[] tireWear;
        final float engineTemp;
        final float bodyDamage;
        final boolean inServiceZone;
        final float repairProgress;

        DamageHudState(float[] tireWear, float engineTemp, float bodyDamage,
                       boolean inServiceZone, float repairProgress) {
            this.tireWear = tireWear;
            this.engineTemp = engineTemp;
            this.bodyDamage = bodyDamage;
            this.inServiceZone = inServiceZone;
            this.repairProgress = repairProgress;
        }
    }

    // ─── PUBLIC API ───

    /**
     * Adds a timed notification that fades out after the given duration.
     */
    public static void addNotification(String text, int color, long durationMs) {
        notifications.addFirst(new TimedNotification(text, color, durationMs));
    }

    /**
     * Sets persistent damage HUD state. Call every tick to keep visible.
     *
     * @param tireWear       wear values [FL, FR, RL, RR] in 0.0–1.0 (0 = new, 1 = destroyed)
     * @param engineTemp     normalized temperature 0.0–1.0 (0 = cold, 1 = overheating)
     * @param bodyDamage     body damage 0.0–1.0 (0 = pristine, 1 = totalled)
     * @param inServiceZone  whether the player is in a service/repair zone
     * @param repairProgress repair progress 0.0–1.0 (only relevant when inServiceZone)
     */
    public static void setDamageHud(float[] tireWear, float engineTemp, float bodyDamage,
                                     boolean inServiceZone, float repairProgress) {
        if (tireWear == null || tireWear.length < 4) return;
        damageHud = new DamageHudState(
                new float[]{tireWear[0], tireWear[1], tireWear[2], tireWear[3]},
                engineTemp, bodyDamage, inServiceZone, repairProgress
        );
    }

    /**
     * Clears all timed notifications and the damage HUD.
     */
    public static void clear() {
        notifications.clear();
        damageHud = null;
    }

    // ─── RENDER ENTRY POINT ───

    /**
     * Main render method — call from HudRenderCallback.
     */
    public static void render(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int currentY = screenHeight / 2 + CROSSHAIR_OFFSET_Y;

        currentY = renderTimedNotifications(drawContext, textRenderer, centerX, currentY);

        DamageHudState hud = damageHud;
        if (hud != null) {
            renderDamageHud(drawContext, textRenderer, centerX, currentY, hud);
        }
    }

    // ─── TIMED NOTIFICATIONS ───

    private static int renderTimedNotifications(DrawContext drawContext, TextRenderer textRenderer,
                                                int centerX, int y) {
        long now = System.currentTimeMillis();
        Iterator<TimedNotification> it = notifications.iterator();

        while (it.hasNext()) {
            TimedNotification notif = it.next();
            if (now >= notif.expireTimeMs) {
                it.remove();
                continue;
            }

            long remaining = notif.expireTimeMs - now;
            float alpha = remaining < FADE_DURATION_MS
                    ? (float) remaining / FADE_DURATION_MS
                    : 1.0f;

            int alphaInt = (int) (alpha * 255) & 0xFF;
            int color = (notif.color & 0x00FFFFFF) | (alphaInt << 24);

            int textWidth = textRenderer.getWidth(notif.text);
            int x = centerX - textWidth / 2;
            drawShadowedText(drawContext, textRenderer, notif.text, x, y, color);

            y += LINE_SPACING;
        }

        return y;
    }

    // ─── DAMAGE HUD ───

    private static void renderDamageHud(DrawContext drawContext, TextRenderer textRenderer,
                                        int centerX, int y, DamageHudState hud) {
        y = renderTireWearLine(drawContext, textRenderer, centerX, y, hud);
        y = renderEngineDamageLine(drawContext, textRenderer, centerX, y, hud);

        if (hud.inServiceZone) {
            renderRepairLine(drawContext, textRenderer, centerX, y, hud.repairProgress);
        }
    }

    // ─── TIRE WEAR LINE ───

    private static int renderTireWearLine(DrawContext drawContext, TextRenderer textRenderer,
                                          int centerX, int y, DamageHudState hud) {
        String[] tireLabels = {"FL", "FR", "RL", "RR"};
        int labelWidth = textRenderer.getWidth("FL") + 1;
        int tireBlockWidth = labelWidth + BAR_WIDTH + BAR_GAP;
        int totalTireWidth = tireBlockWidth * 4 - BAR_GAP;

        int startX = centerX - totalTireWidth / 2;
        int barX = startX;

        for (int i = 0; i < 4; i++) {
            float wear = clamp(hud.tireWear[i], 0f, 1f);
            float health = 1f - wear;
            int barColor = healthColor(health);

            drawShadowedText(drawContext, textRenderer, tireLabels[i], barX, y, COLOR_LABEL);

            int bx = barX + labelWidth;
            int by = y + 1;
            int filledWidth = Math.round(BAR_WIDTH * health);

            drawContext.fill(bx, by, bx + BAR_WIDTH, by + BAR_HEIGHT, COLOR_BAR_BG);
            if (filledWidth > 0) {
                drawContext.fill(bx, by, bx + filledWidth, by + BAR_HEIGHT, barColor);
            }

            barX += tireBlockWidth;
        }

        return y + LINE_SPACING + 2;
    }

    // ─── ENGINE & BODY DAMAGE LINE ───

    private static int renderEngineDamageLine(DrawContext drawContext, TextRenderer textRenderer,
                                              int centerX, int y, DamageHudState hud) {
        // Pre-calculate layout to center the entire line
        String engLabel = "ENG";
        String dmgLabel = "DMG";
        int engLabelW = textRenderer.getWidth(engLabel) + 2;
        int dmgLabelW = textRenderer.getWidth(dmgLabel) + 2;

        float temp = clamp(hud.engineTemp, 0f, 1f);
        float dmg = clamp(hud.bodyDamage, 0f, 1f);
        String tempPct = " " + (int) (temp * 100) + "%";
        String dmgPct = " " + (int) (dmg * 100) + "%";
        int tempPctW = textRenderer.getWidth(tempPct);
        int dmgPctW = textRenderer.getWidth(dmgPct);

        int sepWidth = textRenderer.getWidth(" | ");
        int totalWidth = engLabelW + BAR_WIDTH + tempPctW + sepWidth + dmgLabelW + BAR_WIDTH + dmgPctW;
        int x = centerX - totalWidth / 2;

        // Engine temperature bar
        int tempColor = temperatureColor(temp);
        drawShadowedText(drawContext, textRenderer, engLabel, x, y, COLOR_LABEL);
        x += engLabelW;
        int filledTemp = Math.round(BAR_WIDTH * temp);
        drawContext.fill(x, y + 1, x + BAR_WIDTH, y + 1 + BAR_HEIGHT, COLOR_BAR_BG);
        if (filledTemp > 0) {
            drawContext.fill(x, y + 1, x + filledTemp, y + 1 + BAR_HEIGHT, tempColor);
        }
        x += BAR_WIDTH;
        drawShadowedText(drawContext, textRenderer, tempPct, x, y, COLOR_LABEL);
        x += tempPctW;

        // Separator
        drawShadowedText(drawContext, textRenderer, " | ", x, y, COLOR_SEPARATOR);
        x += sepWidth;

        // Body damage bar
        float dmgHealth = 1f - dmg;
        int dmgColor = healthColor(dmgHealth);
        drawShadowedText(drawContext, textRenderer, dmgLabel, x, y, COLOR_LABEL);
        x += dmgLabelW;
        int filledDmg = Math.round(BAR_WIDTH * dmgHealth);
        drawContext.fill(x, y + 1, x + BAR_WIDTH, y + 1 + BAR_HEIGHT, COLOR_BAR_BG);
        if (filledDmg > 0) {
            drawContext.fill(x, y + 1, x + filledDmg, y + 1 + BAR_HEIGHT, dmgColor);
        }
        x += BAR_WIDTH;
        drawShadowedText(drawContext, textRenderer, dmgPct, x, y, COLOR_LABEL);

        return y + LINE_SPACING + 2;
    }

    // ─── REPAIR PROGRESS LINE ───

    private static void renderRepairLine(DrawContext drawContext, TextRenderer textRenderer,
                                         int centerX, int y, float progress) {
        progress = clamp(progress, 0f, 1f);
        String label = "Repairing... ";
        String pct = " " + (int) (progress * 100) + "%";
        int labelW = textRenderer.getWidth(label);
        int pctW = textRenderer.getWidth(pct);
        int totalWidth = labelW + REPAIR_BAR_WIDTH + pctW;
        int x = centerX - totalWidth / 2;

        drawShadowedText(drawContext, textRenderer, label, x, y, COLOR_SERVICE);

        int barX = x + labelW;
        int filledRepair = Math.round(REPAIR_BAR_WIDTH * progress);
        drawContext.fill(barX, y + 1, barX + REPAIR_BAR_WIDTH, y + 1 + BAR_HEIGHT, COLOR_BAR_BG);
        if (filledRepair > 0) {
            drawContext.fill(barX, y + 1, barX + filledRepair, y + 1 + BAR_HEIGHT, COLOR_SERVICE);
        }

        drawShadowedText(drawContext, textRenderer, pct, barX + REPAIR_BAR_WIDTH, y, COLOR_SERVICE);
    }

    // ─── VERSION-GUARDED TEXT HELPER ───

    private static void drawShadowedText(DrawContext drawContext, TextRenderer textRenderer,
                                         String text, int x, int y, int color) {
        //? <=1.20.4 {
        drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
        //?}
        //? >=1.21 {
        /*drawContext.drawText(textRenderer, text, x, y, color, true);
        *///?}
    }

    // ─── COLOR HELPERS ───

    /**
     * Health color gradient: red (0.0) → yellow (0.5) → green (1.0).
     */
    private static int healthColor(float value) {
        value = clamp(value, 0f, 1f);
        if (value < 0.5f) {
            return blendColors(COLOR_RED, COLOR_YELLOW, value * 2f);
        }
        return blendColors(COLOR_YELLOW, COLOR_GREEN, (value - 0.5f) * 2f);
    }

    /**
     * Temperature color gradient: blue (0.0) → green (0.33) → yellow (0.66) → red (1.0).
     */
    private static int temperatureColor(float value) {
        value = clamp(value, 0f, 1f);
        if (value < TEMP_THRESHOLD_LOW) {
            return blendColors(COLOR_BLUE, COLOR_GREEN, value / TEMP_THRESHOLD_LOW);
        } else if (value < TEMP_THRESHOLD_HIGH) {
            return blendColors(COLOR_GREEN, COLOR_YELLOW, (value - TEMP_THRESHOLD_LOW) / (TEMP_THRESHOLD_HIGH - TEMP_THRESHOLD_LOW));
        }
        return blendColors(COLOR_YELLOW, COLOR_RED, (value - TEMP_THRESHOLD_HIGH) / (1f - TEMP_THRESHOLD_HIGH));
    }

    private static int blendColors(int from, int to, float t) {
        t = clamp(t, 0f, 1f);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
