package me.xginko.anvilduperaddon.modules;

import me.xginko.anvilduperaddon.OneBAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.SharedConstants;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;

public class AnvilDuper extends Module {

    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRenaming = settings.createGroup("Renaming");

    private final Setting<List<Item>> items = sgDefault.add(new ItemListSetting.Builder()
        .name("Items to dupe")
        .description("Which items should be automatically duped")
        .defaultValue(Items.SHULKER_BOX)
        .build());

    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Delay in ticks")
        .defaultValue(10)
        .min(0)
        .sliderMax(200)
        .build());

    private final Setting<Boolean> throwExp = sgRenaming.add(new BoolSetting.Builder()
        .name("throw-xp-bottles")
        .description("Toggles XP thrower for the player.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> stringLength = sgRenaming.add(new IntSetting.Builder()
        .name("name-string-length")
        .description("Length of the random rename strings.")
        .defaultValue(10)
        .min(1)
        .max(50)
        .build());

    private final Setting<Boolean> onlyAscii = sgRenaming.add(new BoolSetting.Builder()
        .name("ascii-only")
        .description("Only uses the characters in the ASCII charset.")
        .defaultValue(true)
        .build());

    public AnvilDuper() {
        super(OneBAddon.CATEGORY, "AnvilDuper", "Automatically dupe items using the anvil rename dupe on 1b1t");
    }

    private int skippedTicks;
    private PrimitiveIterator.OfInt codePointGenerator;

    @Override
    public void onActivate() {
        codePointGenerator = new Random().ints(onlyAscii.get() ? 0x21 : 0x0800, onlyAscii.get() ? 0x7E : 0x10FFFF)
            .filter(codePoint ->
                !Character.isWhitespace(codePoint)
                && Character.isBmpCodePoint(codePoint) // Filter code points that can be represented using a single char
                && SharedConstants.isValidChar((char) codePoint) // Filter code points that can be used in an anvil
            )
            .iterator();
    }

    @Override
    public void onDeactivate() {
        codePointGenerator = null;
        skippedTicks = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        EXPThrower expThrower = throwExp.get() ? Modules.get().get(EXPThrower.class) : null;

        if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler anvilScreen)) {
            if (throwExp.get() && expThrower.isActive()) expThrower.toggle();
            return;
        }

        if (skippedTicks < tickDelay.get()) {
            // Tick cooldown, useful to avoid desync
            skippedTicks++;
            return;
        }

        skippedTicks = 0;

        if (anvilScreen.getCursorStack() != ItemStack.EMPTY && anvilScreen.getCursorStack().getItem() != Items.AIR) {
            // Since we have to use SlotActionType PICKUP to ensure the dupe is successful,
            // we have to put any item we took out back from the mouse cursor into our inventory
            for (int i = 3; i < anvilScreen.slots.size(); i++) {
                if (!anvilScreen.getSlot(i).hasStack()) {
                    mc.interactionManager.clickSlot(anvilScreen.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                    break;
                }
            }
            return;
        }

        if (!anvilScreen.getSlot(0).hasStack()) {
            // Move first configured item type that we can find into input slot
            for (int i = 3; i < anvilScreen.slots.size(); i++) {
                if (items.get().contains(anvilScreen.getSlot(i).getStack().getItem())) {
                    mc.interactionManager.clickSlot(anvilScreen.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    break;
                }
            }
            return;
        }

        if (!anvilScreen.getSlot(2).hasStack()) {
            // Create a new unique random name
            StringBuilder newNameBuilder = new StringBuilder(stringLength.get());
            for (int appendedChars = 0; appendedChars < stringLength.get(); appendedChars++)
                newNameBuilder.appendCodePoint(this.codePointGenerator.nextInt());
            mc.player.networkHandler.sendPacket(new RenameItemC2SPacket(newNameBuilder.toString()));
            return;
        }

        if (mc.player.experienceLevel >= anvilScreen.getLevelCost() && anvilScreen.getLevelCost() > 0) {
            // Take renamed item out of slot into mouse cursor by using SlotActionType PICKUP.
            // Any other type breaks the anvil but doesnt dupe
            mc.interactionManager.clickSlot(anvilScreen.syncId, 2, 0, SlotActionType.PICKUP, mc.player);
            if (throwExp.get() && expThrower.isActive())
                expThrower.toggle();
        } else {
            if (throwExp.get() && !expThrower.isActive())
                expThrower.toggle();
        }
    }
}
