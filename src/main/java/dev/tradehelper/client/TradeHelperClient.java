
package dev.tradehelper.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class TradeHelperClient implements ClientModInitializer {
    private static KeyMapping toggleKey;
    private static boolean enabled = true;

    private static final Map<String, Long> exhaustedAt = new HashMap<>();
    private static long clientTicks = 0;

    private static final int PANEL_WIDTH = 158;
    private static final int PANEL_HEIGHT = 174;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tradehelper", "main"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradehelper.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTicks++;

            while (toggleKey.consumeClick()) {
                enabled = !enabled;
            }

            if (client.player != null
                    && client.player.containerMenu instanceof MerchantMenu menu) {
                for (MerchantOffer offer : menu.getOffers()) {
                    String key = tradeKey(offer);

                    if (offer.getUses() >= offer.getMaxUses()) {
                        exhaustedAt.putIfAbsent(key, clientTicks);
                    } else {
                        exhaustedAt.remove(key);
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof MerchantScreen) {
                ScreenEvents.afterExtract(screen).register(
                        (currentScreen, graphics, mouseX, mouseY, tickProgress) ->
                                renderPanel(currentScreen, graphics)
                );
            }
        });
    }

    private static String tradeKey(MerchantOffer offer) {
        return offer.getCostA().getItem() + ":" + offer.getCostA().getCount()
                + "|" + offer.getCostB().getItem() + ":" + offer.getCostB().getCount()
                + "->" + offer.getResult().getItem() + ":" + offer.getResult().getCount();
    }

    private static void renderPanel(
            net.minecraft.client.gui.screens.Screen screen,
            GuiGraphicsExtractor graphics
    ) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null
                || !(client.player.containerMenu instanceof MerchantMenu menu)) return;

        int menuLeft = (screen.width - 276) / 2;
        int x = menuLeft - PANEL_WIDTH - 8;
        int y = (screen.height - PANEL_HEIGHT) / 2;

        if (x < 4) x = 4;

        int bottom = y + PANEL_HEIGHT;

        // Panel background and border
        graphics.fill(x, y, x + PANEL_WIDTH, bottom, 0xEE101722);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 3, 0xFF35C98B);
        graphics.fill(x, y + 3, x + 1, bottom, 0xFF344354);
        graphics.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, bottom, 0xFF344354);
        graphics.fill(x, bottom - 1, x + PANEL_WIDTH, bottom, 0xFF344354);

        int textX = x + 9;
        int textY = y + 10;

        graphics.text(client.font, "TRADEHELPER", textX, textY, 0xFF65E6A4, false);
        textY += 14;

        graphics.fill(textX, textY, x + PANEL_WIDTH - 9, textY + 1, 0xFF354455);
        textY += 7;

        int shown = 0;

        for (MerchantOffer offer : menu.getOffers()) {
            if (shown >= 6) break;

            ItemStack a = offer.getCostA();
            ItemStack b = offer.getCostB();
            ItemStack result = offer.getResult();

            int emeraldCost = emeraldEquivalent(a, b);
            int profit = -1;

            if (emeraldCost >= 0 && result.getItem().toString().contains("emerald")) {
                profit = result.getCount() - emeraldCost;
            }

            boolean exhausted = offer.getUses() >= offer.getMaxUses();

            String cost = itemLabel(a);
            if (!b.isEmpty()) cost += " + " + itemLabel(b);

            graphics.text(
                    client.font,
                    trim(client, cost + " → " + itemLabel(result), PANEL_WIDTH - 18),
                    textX,
                    textY,
                    0xFFE5EDF5,
                    false
            );

            textY += 11;

            if (exhausted) {
                long elapsed = clientTicks - exhaustedAt.getOrDefault(
                        tradeKey(offer), clientTicks);

                long remaining = Math.max(0, 24000 - elapsed);

                graphics.text(
                        client.font,
                        "RESTOCK ~" + ticksToTime(remaining),
                        textX,
                        textY,
                        0xFFFFC66D,
                        false
                );
            } else if (profit != -1) {
                graphics.text(
                        client.font,
                        "Emerald net: " + (profit >= 0 ? "+" : "") + profit,
                        textX,
                        textY,
                        profit >= 0 ? 0xFF65E6A4 : 0xFFFF7777,
                        false
                );
            } else {
                graphics.text(
                        client.font,
                        "Uses: " + offer.getUses() + "/" + offer.getMaxUses(),
                        textX,
                        textY,
                        0xFF9AA9BC,
                        false
                );
            }

            textY += 17;
            shown++;
        }

        if (menu.getOffers().size() > 6) {
            graphics.text(
                    client.font,
                    "+" + (menu.getOffers().size() - 6) + " more trades",
                    textX,
                    y + PANEL_HEIGHT - 24,
                    0xFF9AA9BC,
                    false
            );
        }

        graphics.text(
                client.font,
                "H  •  Toggle panel",
                textX,
                y + PANEL_HEIGHT - 12,
                0xFF65E6A4,
                false
        );
    }

    private static String trim(Minecraft client, String text, int maxWidth) {
        while (!text.isEmpty() && client.font.width(text) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static int emeraldEquivalent(ItemStack a, ItemStack b) {
        int total = 0;
        boolean found = false;

        if (a.getItem().toString().contains("emerald")) {
            total += a.getCount();
            found = true;
        }

        if (b != null && !b.isEmpty()
                && b.getItem().toString().contains("emerald")) {
            total += b.getCount();
            found = true;
        }

        return found ? total : -1;
    }

    private static String itemLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "—";
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }

    private static String ticksToTime(long ticks) {
        long seconds = ticks / 20;
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}
