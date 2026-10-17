package com.example.kingsmptools.modules;

import com.example.kingsmptools.KingSMPTools;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoOrders extends Module {
    public AutoOrders() {
        super(KingSMPTools.CATEGORY, "kingsmp-orders",
            "Opens the KingSMP orders menu. Automatic order placement needs verified menu/command details.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            toggle();
            return;
        }

        mc.getNetworkHandler().sendChatCommand("orders");
        toggle();
    }
}
