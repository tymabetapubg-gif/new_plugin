package ua.tymaa.adminpanelmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Клієнтський мод "Адмін панель (tymaa)".
 * - клавіша H (змінюється в Налаштування -> Керування) відкриває панель;
 * - у меню ESC додається червоно-чорна кнопка "Адмін панель  Бінд: [H]".
 */
public class AdminPanelClient implements ClientModInitializer {

    public static KeyMapping OPEN;
    private static boolean wasDown;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tymaaadminpanel", "main"));

        OPEN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tymaaadminpanel.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_H,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new PanelScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                wasDown = true; // щоб клік, який відкрив меню, не спрацював
                ScreenEvents.afterRender(screen).register(
                        (s, graphics, mouseX, mouseY, tickDelta) -> drawPauseButton(client, s, graphics, mouseX, mouseY));
            }
        });
    }

    private static void drawPauseButton(Minecraft mc, Screen screen, GuiGraphics g, int mx, int my) {
        int x = 8, y = 8, w = 210, h = 22;
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;

        g.fill(x, y, x + w, y + h, hover ? 0xFFFF3B3B : 0xFFB01212);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF260909 : 0xFF0A0A0A);

        Font font = mc.font;
        g.drawString(font, "Адмін панель", x + 8, y + 7, 0xFFFF4040, false);
        String bind = "Бінд: [" + OPEN.getTranslatedKeyMessage().getString() + "]";
        g.drawString(font, bind, x + w - 8 - font.width(bind), y + 7, 0xFFBBBBBB, false);

        boolean down = mc.mouseHandler.isLeftPressed();
        if (down && !wasDown && hover) {
            mc.execute(() -> mc.setScreen(new PanelScreen()));
        }
        wasDown = down;
    }
}
