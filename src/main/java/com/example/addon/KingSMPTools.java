package com.example.kingsmptools;

import com.example.kingsmptools.modules.AutoBuyEXP;
import com.example.kingsmptools.modules.AutoOrders;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class KingSMPTools extends MeteorAddon {
    public static final Category CATEGORY = new Category("KingSMP");

    @Override
    public void onInitialize() {
        Modules.get().add(new AutoBuyEXP());
        Modules.get().add(new AutoOrders());
    }

    @Override
    public String getPackage() {
        return "com.example.kingsmptools";
    }
}
