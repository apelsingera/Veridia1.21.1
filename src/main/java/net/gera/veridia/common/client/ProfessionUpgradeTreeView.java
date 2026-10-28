package net.gera.veridia.common.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
/**
 * Обёртка над конкретным деревом улучшений (LumberjackUpgrades, FarmerUpgrades, ...) для
 * VillagerProfessionScreen - сам экран рендерит/кликает/показывает тултип ОДИНАКОВО для любой
 * профессии через этот интерфейс, зная только позиции/связи (общие, из VillagerUpgradeTree) и
 * "как нарисовать/описать узел" (тут, разное для каждой профессии). Новая профессия с деревом -
 * это реализация этого интерфейса (см. LumberjackTreeView/FarmerTreeView) + сам файл дерева
 * (LumberjackUpgrades/FarmerUpgrades) + добавление ветки в VillagerProfessionScreen.viewFor().
 */
public interface ProfessionUpgradeTreeView {
    int size();
    int x(int index);
    int y(int index);
    int prerequisiteIndex(int index);
    ResourceLocation icon(int index);
    @Nullable
    ItemStack overlayItem(int index);
    Component describe(int index);
    boolean isAvailable(Villager villager, int index);
    boolean isUnlocked(Villager villager, int index);
    ItemStack costDisplayStack(Player player, int index);
    /** true - узел сейчас не рисуется и не кликается (и линии к нему тоже). По умолчанию все узлы видны;
     * переопределяет только дерево, у которого набор узлов меняется (у nailer'а - level1/level2). */
    default boolean isHidden(Villager villager, int index) {
        return false;
    }
}
