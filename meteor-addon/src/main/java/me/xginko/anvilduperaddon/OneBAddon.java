package me.xginko.anvilduperaddon;

import com.mojang.logging.LogUtils;
import me.xginko.anvilduperaddon.modules.AnvilDuper;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class OneBAddon extends MeteorAddon {

    public static final Category CATEGORY = new Category("1b1t");

    @Override
    public void onInitialize() {
        LogUtils.getLogger().info("Loading 1b1t addon.");
        Modules.get().add(new AnvilDuper());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "me.xginko.anvilduperaddon";
    }
}
