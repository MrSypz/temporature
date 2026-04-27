package com.sypztep.temporature.client;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.config.RequireSync;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.plateau.client.v1.ui.layout.Layout;
import com.sypztep.plateau.client.v1.ui.layout.RowLayout;
import com.sypztep.plateau.client.v1.ui.screen.PlateauScreen;
import com.sypztep.plateau.client.v1.ui.widget.UIButton;
import com.sypztep.plateau.client.v1.ui.widget.UILabel;
import com.sypztep.plateau.client.v1.ui.widget.UIScrollPanel;
import com.sypztep.plateau.client.v1.ui.widget.UIText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ConfigSyncScreen extends PlateauScreen {
    private final TemporatureServerConfig serverCfg;
    private final int serverHash;
    private final List<ConfigDiff> diffs;

    public ConfigSyncScreen(TemporatureServerConfig serverCfg, int serverHash) {
        super(Component.translatable("screen.temporature.config_sync.title"));
        this.serverCfg = serverCfg;
        this.serverHash = serverHash;
        this.diffs = buildDiffs(TemporatureServerConfig.getInstance(), serverCfg);
    }

    @Override
    protected void initComponents() {
        int panelW = Layout.clampWidth(width - 40, 260, 400);
        int panelX = Layout.centerX(width, panelW);

        int btnH = 20;
        int bottomMargin = 10;
        int btnY = height - bottomMargin - btnH;

        int descY = 8;
        Component descText = Component.translatable("screen.temporature.config_sync.description");
        int descH = font.split(descText, panelW).size() * (font.lineHeight + 2);
        UIText desc = new UIText(panelX, descY, panelW, descText).setCentered(true);
        addRenderableOnly(desc);

        int hashY = descY + descH + 4;
        UILabel hashLabel = new UILabel(panelX, hashY, panelW,
                Component.translatable("screen.temporature.config_sync.hash",
                        Integer.toHexString(serverHash))).setColor(0xFFB0E4CC).setCentered(true);
        addRenderableOnly(hashLabel);

        int scrollTop = hashY + hashLabel.getHeight() + 8;
        int scrollH = btnY - scrollTop - 8;

        List<ConfigDiff> changedOnly = diffs.stream().filter(d -> d.changed).toList();

        // Use the named inner class instead of an anonymous one
        DiffScrollPanel diffPanel = new DiffScrollPanel(
                panelX, scrollTop, panelW, scrollH,
                Component.translatable("screen.temporature.config_sync.changes", changedOnly.size()),
                changedOnly
        );
        addRenderableWidget(diffPanel);

        int btnW = (panelW - 6) / 2;
        UIButton acceptBtn = new UIButton(0, 0, btnW, btnH,
                Component.translatable("screen.temporature.config_sync.accept"),
                _ -> {
                    TemporatureServerConfig.applyFrom(serverCfg);
                    TemporatureServerConfig.setSyncedFromServer(true);
                    UISounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);
                    onClose();
                });

        UIButton denyBtn = new UIButton(0, 0, btnW, btnH,
                Component.translatable("screen.temporature.config_sync.deny"),
                _ -> {
                    if (minecraft.getConnection() != null) {
                        minecraft.getConnection().getConnection().disconnect(
                                Component.translatable("screen.temporature.config_sync.denied"));
                    }
                });

        RowLayout row = new RowLayout(panelX, btnY, btnH).gap(6);
        row.add(acceptBtn);
        row.add(denyBtn);
        row.apply();

        addRenderableWidget(acceptBtn);
        addRenderableWidget(denyBtn);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private record ConfigDiff(String name, Object clientVal, Object serverVal, boolean changed) {}

    /**
     * Scrollable panel that renders config diffs in a three-column grid:
     *   | name        | your value   | server value |
     *   [field name]  | [clientVal]  |  [serverVal] |
     *   [field name]  | [clientVal]  |  [serverVal] |
     *   [field name]  | [clientVal]  |  [serverVal] |
     *   [field name]  | [clientVal]  |  [serverVal] |
     *   [field name]  | [clientVal]  |  [serverVal] |
     *
     * Column widths are split proportionally across the available content width
     * so values never overflow or wrap awkwardly on narrow screens.
     */
    private static class DiffScrollPanel extends UIScrollPanel {

        private static final int LINE_H = 12;
        private static final int ROW_PAD = 4;
        private static final int ROW_H = LINE_H + ROW_PAD;
        private static final int ROW_H_PADDING = 2;
        private static final float COL_NAME   = 0.40f;
        private static final float COL_FROM   = 0.30f;

        private final List<ConfigDiff> changedOnly;

        public DiffScrollPanel(int x, int y, int width, int height,
                               Component title, List<ConfigDiff> changedOnly) {
            super(x, y, width, height, title);
            this.changedOnly = changedOnly;
        }



        @Override
        protected void renderScrollContent(GuiGraphicsExtractor graphics,
                                           int mouseX, int mouseY, float delta,
                                           int contentX, int contentY, int contentWidth) {
            int nameW = (int) (contentWidth * COL_NAME);
            int fromW = (int) (contentWidth * COL_FROM);

            int xFrom = contentX + nameW;
            int xTo   = xFrom + fromW;

            int sep0X = contentX;         // left border
            int sep1X = xFrom - 4;         // after name
            int sep2X = xTo - 4;           // after client val
            int sep3X = contentX + contentWidth - 1; // right border

            int y = contentY - getScrollOffset();

            // Header row
            graphics.fill(contentX, y - 1, contentX + contentWidth, y + ROW_H - 1, 0x33FFFFFF);
            graphics.text(font, Component.translatable("screen.temporature.config_sync.col_name")
                    .withStyle(ChatFormatting.WHITE), contentX + ROW_PAD, y + ROW_H_PADDING, 0xFFFFFFFF, true);
            graphics.text(font, Component.translatable("screen.temporature.config_sync.col_yours")
                    .withStyle(ChatFormatting.WHITE), xFrom, y  + ROW_H_PADDING, 0xFFFFFFFF, true);
            graphics.text(font, Component.translatable("screen.temporature.config_sync.col_server")
                    .withStyle(ChatFormatting.WHITE), xTo, y  + ROW_H_PADDING, 0xFFFFFFFF, true);
            y += ROW_H;

            if (changedOnly.isEmpty()) {
                graphics.text(font,
                        Component.translatable("screen.temporature.config_sync.no_changes")
                                .withStyle(ChatFormatting.GREEN),
                        contentX + ROW_PAD, y, 0xFFFFFFFF, true);
                setTotalContentHeight(ROW_H * 2);
                return;
            }

            int totalH = (changedOnly.size() + 1) * ROW_H; // +1 for header
            int gridTop = contentY - getScrollOffset();

            // Vertical separators spanning full height including header
            for (int sx : new int[]{sep0X, sep1X, sep2X, sep3X}) {
                graphics.fill(sx, gridTop, sx + 1, gridTop + totalH, 0x55FFFFFF);
            }

            for (int i = 0; i < changedOnly.size(); i++) {
                ConfigDiff diff = changedOnly.get(i);

                if (i % 2 == 0) {
                    graphics.fill(contentX, y - 1, contentX + contentWidth, y + ROW_H - 1, 0x18FFFFFF);
                }

                graphics.text(font,
                        Component.literal(diff.name()).withStyle(ChatFormatting.GRAY),
                        contentX + ROW_PAD, y + ROW_H_PADDING, 0xFFFFFFFF, true);
                graphics.text(font,
                        Component.literal(String.valueOf(diff.clientVal())).withStyle(ChatFormatting.RED),
                        xFrom, y + ROW_H_PADDING, 0xFFFFFFFF, true);
                graphics.text(font,
                        Component.literal(String.valueOf(diff.serverVal())).withStyle(ChatFormatting.GREEN),
                        xTo, y + ROW_H_PADDING, 0xFFFFFFFF, true);

                y += ROW_H;
            }

            setTotalContentHeight(totalH);
        }
    }

    // -------------------------------------------------------------------------
    // Diff builder
    // -------------------------------------------------------------------------

    private static List<ConfigDiff> buildDiffs(TemporatureServerConfig client,
                                               TemporatureServerConfig server) {
        List<ConfigDiff> list = new ArrayList<>();
        for (Field field : TemporatureServerConfig.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(RequireSync.class)) continue;
            field.setAccessible(true);
            try {
                Object clientVal = field.get(client);
                Object serverVal = field.get(server);
                list.add(new ConfigDiff(field.getName(), clientVal, serverVal,
                        !clientVal.equals(serverVal)));
            } catch (IllegalAccessException e) {
                Temporature.LOGGER.error("Failed to diff field '{}': {}", field.getName(), e.getMessage());
            }
        }
        return list;
    }
}