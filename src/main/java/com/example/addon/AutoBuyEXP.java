package com.example.kingsmptools.modules;

import com.example.kingsmptools.KingSMPTools;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoBuyEXP extends Module {
    private enum State { IDLE, WAIT_SHOP, WAIT_EXP_MENU, BUYING }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> lowPercent = sgGeneral.add(new IntSetting.Builder()
        .name("start-below-percent")
        .description("Start buying when shovel durability is below this percent.")
        .defaultValue(50).min(1).max(99).sliderMax(99).build());
    private final Setting<Integer> stopPercent = sgGeneral.add(new IntSetting.Builder()
        .name("stop-above-percent")
        .description("Stop buying when shovel durability is above this percent.")
        .defaultValue(51).min(2).max(100).sliderMax(100).build());
    private final Setting<Integer> clicksPerBatch = sgGeneral.add(new IntSetting.Builder()
        .name("clicks-per-batch")
        .description("Number of clicks on shop slot 24 per batch.")
        .defaultValue(64).min(1).max(64).sliderMax(64).build());
    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("click-delay-ticks")
        .description("Ticks between shop clicks; increase if the server lags.")
        .defaultValue(2).min(1).max(20).sliderMax(10).build());

    private State state = State.IDLE;
    private boolean cycleActive;
    private int waitTicks;
    private int clicks;
    private int batchClicks;

    public AutoBuyEXP() {
        super(KingSMPTools.CATEGORY, "kingsmp-auto-buy-exp",
            "Opens gear shop and clicks EXP purchase while the held shovel is low on durability.");
    }

    @Override
    public void onActivate() {
        state = State.IDLE;
        cycleActive = false;
        waitTicks = 0;
        clicks = 0;
        batchClicks = 0;
    }

    @Override
    public void onDeactivate() {
        state = State.IDLE;
        cycleActive = false;
        waitTicks = 0;
        clicks = 0;
        batchClicks = 0;
    }

    private int shovelDurabilityPercent() {
        if (mc.player == null) return -1;
        ItemStack stack = mc.player.getMainHandStack();
        if (!(stack.getItem() instanceof ShovelItem) || stack.getMaxDamage() <= 0) return -1;
        return Math.round(100.0f * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage());
    }

    private int findExpBottleSlot() {
        if (mc.player == null) return -1;
        ScreenHandler handler = mc.player.currentScreenHandler;
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isOf(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }

    private boolean clickSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return false;
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (slot < 0 || slot >= handler.slots.size()) return false;
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        return true;
    }

    private void openShop() {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendChatCommand("shop open gear");
        state = State.WAIT_SHOP;
        waitTicks = 20;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        int durability = shovelDurabilityPercent();
        if (durability < 0) {
            // Do not run unless a shovel is held in the main hand.
            state = State.IDLE;
            cycleActive = false;
            return;
        }

        if (durability > stopPercent.get()) {
            state = State.IDLE;
            cycleActive = false;
            return;
        }

        if (!cycleActive) {
            if (durability < lowPercent.get()) cycleActive = true;
            else return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (state) {
            case IDLE -> {
                // Don't overwrite an open screen; let the user close it first.
                if (mc.currentScreen != null) return;
                openShop();
            }
            case WAIT_SHOP -> {
                int expSlot = findExpBottleSlot();
                if (expSlot < 0) {
                    waitTicks = 10;
                    return;
                }
                if (!clickSlot(expSlot)) {
                    state = State.IDLE;
                    return;
                }
                state = State.WAIT_EXP_MENU;
                waitTicks = 10;
            }
            case WAIT_EXP_MENU -> {
                if (mc.player.currentScreenHandler.slots.size() <= 24) {
                    state = State.IDLE;
                    return;
                }
                clicks = 0;
                state = State.BUYING;
            }
            case BUYING -> {
                if (mc.player.currentScreenHandler.slots.size() <= 24) {
                    state = State.IDLE;
                    return;
                }
                if (!clickSlot(24)) {
                    state = State.IDLE;
                    return;
                }
                clicks++;
                batchClicks++;
                waitTicks = delayTicks.get() - 1;

                if (batchClicks >= clicksPerBatch.get()) {
                    batchClicks = 0;
                    state = State.IDLE;
                    // Close the shop so the next cycle can reopen it cleanly.
                    if (mc.player.currentScreen != null) {
                        mc.player.closeHandledScreen();
                    }
                    waitTicks = 10;
                }
            }
        }
    }
}
