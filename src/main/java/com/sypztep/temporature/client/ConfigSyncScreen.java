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

        // Description text (word-wrapped)
        int descY = 8;
        Component descText = Component.translatable("screen.temporature.config_sync.description");
        int descH = font.split(descText, panelW).size() * (font.lineHeight + 2);
        UIText desc = new UIText(panelX, descY, panelW, descText).setCentered(true);
        addRenderableOnly(desc);

        // Hash display
        int hashY = descY + descH + 4;
        UILabel hashLabel = new UILabel(panelX, hashY, panelW,
                Component.translatable("screen.temporature.config_sync.hash",
                        Integer.toHexString(serverHash))).setColor(0xFFB0E4CC).setCentered(true);
        addRenderableOnly(hashLabel);

        // Diff scroll panel — only changed fields
        int scrollTop = hashY + hashLabel.getHeight() + 8;
        int scrollH = btnY - scrollTop - 8;
        int lineH = font.lineHeight + 6;

        List<ConfigDiff> changedOnly = diffs.stream().filter(d -> d.changed).toList();

        UIScrollPanel diffPanel = new UIScrollPanel(panelX, scrollTop, panelW, scrollH,
                Component.translatable("screen.temporature.config_sync.changes", changedOnly.size())) {
            @Override
            protected void renderScrollContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta,
                                               int contentX, int contentY, int contentWidth) {
                int y = contentY - getScrollOffset();

                if (changedOnly.isEmpty()) {
                    graphics.text(font,
                            Component.translatable("screen.temporature.config_sync.no_changes")
                                    .withStyle(ChatFormatting.GREEN),
                            contentX, y, 0xFFFFFFFF, true);
                    setTotalContentHeight(lineH);
                    return;
                }

                for (ConfigDiff diff : changedOnly) {
                    MutableComponent line = Component.literal(diff.name + ": ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(diff.clientVal))
                                    .withStyle(ChatFormatting.RED))
                            .append(Component.literal(" → ")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(String.valueOf(diff.serverVal))
                                    .withStyle(ChatFormatting.GREEN));

                    graphics.text(font, line, contentX, y, 0xFFFFFFFF, true);
                    y += lineH;
                }
                setTotalContentHeight(changedOnly.size() * lineH);
            }
        };

        addRenderableWidget(diffPanel);

        // Buttons
        int btnW = (panelW - 6) / 2;
        UIButton acceptBtn = new UIButton(0, 0, btnW, btnH,
                Component.translatable("screen.temporature.config_sync.accept"),
                _ -> {
                    TemporatureServerConfig.applyFrom(serverCfg);
                    TemporatureServerConfig.setSyncedFromServer(true);
                    UISounds.play(SoundEvents.EXPERIENCE_ORB_PICKUP,1,1);
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record ConfigDiff(String name, Object clientVal, Object serverVal, boolean changed) {}

    private static List<ConfigDiff> buildDiffs(TemporatureServerConfig client, TemporatureServerConfig server) {
        List<ConfigDiff> list = new ArrayList<>();
        for (Field field : TemporatureServerConfig.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(RequireSync.class)) continue;
            field.setAccessible(true);
            try {
                Object clientVal = field.get(client);
                Object serverVal = field.get(server);
                list.add(new ConfigDiff(field.getName(), clientVal, serverVal, !clientVal.equals(serverVal)));
            } catch (IllegalAccessException e) {
                Temporature.LOGGER.error("Failed to diff field '{}': {}", field.getName(), e.getMessage());
            }
        }
        return list;
    }
}