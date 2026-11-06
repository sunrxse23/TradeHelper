package dev.tradehelper.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class TradeHelperClient implements ClientModInitializer {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("tradehelper", "trade_overlay");
    private static KeyMapping toggleKey;
    private static boolean enabled = true;

    // Local-only observation: records when a trade first becomes exhausted.
    // Minecraft runs at 20 ticks/second; a full day is 24,000 ticks (~20 minutes).
    private static final Map<String, Long> exhaustedAt = new HashMap<>();
    private static long clientTicks = 0;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tradehelper", "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradehelper.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTicks++;
            while (toggleKey.consumeClick()) enabled = !enabled;

            if (client.player != null && client.player.containerMenu instanceof MerchantMenu menu) {
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

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT, HUD_ID, TradeHelperClient::renderHud);
    }

    private static String tradeKey(MerchantOffer offer) {
        return offer.getCostA().getItem().toString() + ":" + offer.getCostA().getCount()
                + "|" + offer.getCostB().getItem().toString() + ":" + offer.getCostB().getCount()
                + "->" + offer.getResult().getItem().toString() + ":" + offer.getResult().getCount();
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(client.player.containerMenu instanceof MerchantMenu menu)) return;

        int x = 8;
        int y = 8;
        graphics.fill(x - 4, y - 4, x + 238, y + 22 + menu.getOffers().size() * 22, 0xB0101420);
        graphics.drawString(client.font, "TradeHelper  •  Villager trades", x, y, 0xFFE9F2FF, false);
        y += 14;

        int shown = 0;
        for (MerchantOffer offer : menu.getOffers()) {
            if (shown++ >= 8) break;
            ItemStack a = offer.getCostA();
            ItemStack b = offer.getCostB();
            ItemStack result = offer.getResult();

            int emeraldCost = emeraldEquivalent(a, b);
            String cost = itemLabel(a);
            if (!b.isEmpty()) cost += " + " + itemLabel(b);

            String line;
            int color = 0xFFD8E4F0;
            if (offer.getUses() >= offer.getMaxUses()) {
                long elapsed = clientTicks - exhaustedAt.getOrDefault(tradeKey(offer), clientTicks);
                long remaining = Math.max(0, 24000 - elapsed);
                line = "RESTOCK? " + cost + " -> " + itemLabel(result) + "  (" + ticksToTime(remaining) + " est.)";
                color = 0xFFFFC66D;
            } else {
                line = cost + " -> " + itemLabel(result) + "  [" + offer.getUses() + "/" + offer.getMaxUses() + "]";
            }
            graphics.drawString(client.font, line, x, y, color, false);
            y += 12;

            if (emeraldCost >= 0 && result.getItem().toString().contains("emerald")) {
                int profit = result.getCount() - emeraldCost;
                graphics.drawString(client.font, "  emerald net: " + (profit >= 0 ? "+" : "") + profit,
                        x + 5, y, profit >= 0 ? 0xFF75E6A0 : 0xFFFF7777, false);
                y += 11;
            }
        }
        graphics.drawString(client.font, "H = toggle overlay  •  Restock time is an estimate",
                x, y + 3, 0xFF9AA9BC, false);
    }

    private static int emeraldEquivalent(ItemStack a, ItemStack b) {
        int total = 0;
        boolean found = false;
        if (a.getItem().toString().contains("emerald")) { total += a.getCount(); found = true; }
        if (b != null && !b.isEmpty() && b.getItem().toString().contains("emerald")) {
            total += b.getCount(); found = true;
        }
        return found ? total : -1;
    }

    private static String itemLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "—";
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }

    private static String ticksToTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }
}
