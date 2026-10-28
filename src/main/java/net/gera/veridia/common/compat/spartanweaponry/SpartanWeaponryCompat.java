package net.gera.veridia.common.compat.spartanweaponry;

import net.gera.veridia.common.util.Ids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Оружие Spartan Weaponry по id (через {@link Ids}, без классов самого мода): без Spartan Weaponry игра не падает,
 * вместо оружия просто ничего.
 */
public final class SpartanWeaponryCompat {
    private static final String MOD_ID = "spartan_weaponry_unofficial";

    public static final ResourceLocation COPPER_BATTLEAXE = id("copper_battleaxe");
    public static final ResourceLocation COPPER_PIKE = id("copper_pike");
    public static final ResourceLocation IRON_PIKE = id("iron_pike");
    public static final ResourceLocation DIAMOND_PIKE = id("diamond_pike");
    public static final ResourceLocation IRON_BATTLE_HAMMER = id("iron_battle_hammer");
    public static final ResourceLocation COPPER_WARHAMMER = id("copper_warhammer");
    public static final ResourceLocation IRON_WARHAMMER = id("iron_warhammer");
    public static final ResourceLocation DIAMOND_WARHAMMER = id("diamond_warhammer");
    public static final ResourceLocation COPPER_RAPIER = id("copper_rapier");
    public static final ResourceLocation IRON_RAPIER = id("iron_rapier");
    public static final ResourceLocation NETHERITE_RAPIER = id("netherite_rapier");
    public static final ResourceLocation DIAMOND_GREATSWORD = id("diamond_greatsword");
    public static final ResourceLocation DIAMOND_SCYTHE = id("diamond_scythe");

    private SpartanWeaponryCompat() {
    }

    public static Item copperBattleaxe() {
        return Ids.item(COPPER_BATTLEAXE.toString());
    }

    public static Item ironWarhammer() {
        return Ids.item(IRON_WARHAMMER.toString());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
