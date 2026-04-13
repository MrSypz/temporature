package com.sypztep.client;

import com.sypztep.config.TemporatureServerConfig;
import com.sypztep.plateau.client.v1.ui.layout.Layout;
import com.sypztep.plateau.client.v1.ui.layout.RowLayout;
import com.sypztep.plateau.client.v1.ui.screen.PlateauScreen;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v1.ui.widget.UIButton;
import com.sypztep.plateau.client.v1.ui.widget.UILabel;
import com.sypztep.plateau.client.v1.ui.widget.UIScrollPanel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

        // Description label
        int descY = 8;
        UILabel desc = new UILabel(panelX, descY, panelW,
                Component.translatable("screen.temporature.config_sync.description")).setCentered(true);
        addRenderableOnly(desc);

        // Hash display
        int hashY = descY + desc.getHeight() + 4;
        UILabel hashLabel = new UILabel(panelX, hashY, panelW,
                Component.translatable("screen.temporature.config_sync.hash",
                        Integer.toHexString(serverHash))).setCentered(true);
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
                            .append(Component.literal(" \u2192 ")
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
                btn -> {
                    TemporatureServerConfig.applyFrom(serverCfg);
                    TemporatureServerConfig.setSyncedFromServer(true);
                    onClose();
                });

        UIButton denyBtn = new UIButton(0, 0, btnW, btnH,
                Component.translatable("screen.temporature.config_sync.deny"),
                btn -> {
                    if (minecraft != null && minecraft.getConnection() != null) {
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

    private record ConfigDiff(String name, Object clientVal, Object serverVal, boolean changed) {}

    private static List<ConfigDiff> buildDiffs(TemporatureServerConfig client, TemporatureServerConfig server) {
        List<ConfigDiff> list = new ArrayList<>();
        diff(list, "enableTemperatureSystem", client.enableTemperatureSystem, server.enableTemperatureSystem);
        diff(list, "minHabitableTemp", client.minHabitableTemp, server.minHabitableTemp);
        diff(list, "maxHabitableTemp", client.maxHabitableTemp, server.maxHabitableTemp);
        diff(list, "tempRate", client.tempRate, server.tempRate);
        diff(list, "tempDamageInterval", client.tempDamageInterval, server.tempDamageInterval);
        diff(list, "tempBaseDamage", client.tempBaseDamage, server.tempBaseDamage);
        diff(list, "blockScanRadius", client.blockScanRadius, server.blockScanRadius);
        diff(list, "waterSoakSpeed", client.waterSoakSpeed, server.waterSoakSpeed);
        diff(list, "rainSoakSpeed", client.rainSoakSpeed, server.rainSoakSpeed);
        diff(list, "maxRainWetness", client.maxRainWetness, server.maxRainWetness);
        diff(list, "dryRate", client.dryRate, server.dryRate);
        diff(list, "hotDryBonus", client.hotDryBonus, server.hotDryBonus);
        diff(list, "coldDryMultiplier", client.coldDryMultiplier, server.coldDryMultiplier);
        diff(list, "defaultWaterTemp", client.defaultWaterTemp, server.defaultWaterTemp);
        return list;
    }

    private static void diff(List<ConfigDiff> list, String name, Object client, Object server) {
        list.add(new ConfigDiff(name, client, server, !client.equals(server)));
    }
}
