package net.gera.veridia.common.villager;

import net.gera.veridia.common.aibuff.villager.farmer.FarmerUpgrades;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackUpgrades;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorUpgrades;
import net.gera.veridia.common.block.entity.screen.MenuTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VillagerProfessionMenu extends AbstractContainerMenu {
    private final VillagerProfession profession;
    private final Player player;
    /** Конкретный житель, чьё дерево улучшений показывает это окно - "1 апгрейд жителя на одного
     * жителя": разблокировка узла (clickMenuButton) и чтение состояния (VillagerProfessionScreen)
     * идут именно по нему, а не по игроку/профессии вообще. null, если сущность не нашлась (не
     * должно происходить в обычном потоке - открытие идёт от VillagerNetworking с уже проверенным
     * id) - тогда окно просто не даёт разблокировать ничего, см. clickMenuButton. */
    @Nullable
    private final Villager villager;

    public VillagerProfessionMenu(int containerId, Inventory inventory, VillagerProfession profession, @Nullable Villager villager) {
        super(MenuTypes.VILLAGER_PROFESSION_MENU.get(), containerId);
        this.profession = profession;
        this.player = inventory.player;
        this.villager = villager;
    }

    public VillagerProfessionMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, readProfession(buf), readVillager(inventory, buf));
    }

    private static VillagerProfession readProfession(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.get(id);
        return profession != null ? profession : VillagerProfession.NONE;
    }

    @Nullable
    private static Villager readVillager(Inventory inventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Entity entity = inventory.player.level().getEntity(entityId);
        return entity instanceof Villager villager ? villager : null;
    }

    public VillagerProfession profession() {
        return profession;
    }

    @Nullable
    public Villager villager() {
        return villager;
    }

    /** id - индекс узла в дереве улучшений текущей профессии (LumberjackUpgrades.NODES /
     * FarmerUpgrades.NODES). Профессия перепроверяется здесь же - клиент шлёт клики только для
     * профессий со своим деревом, но серверу своему клиенту доверять нельзя. Без жителя (villager
     * не нашёлся при открытии) разблокировать нечего. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (villager == null) {
            return super.clickMenuButton(player, id);
        }
        if (profession == ModVillagers.LUMBERJACK.value()) {
            return LumberjackUpgrades.tryUnlock(player, villager, id);
        }
        if (profession == VillagerProfession.FARMER) {
            return FarmerUpgrades.tryUnlock(player, villager, id);
        }
        if (profession == ModVillagers.WARRIOR.value()) {
            return WarriorUpgrades.tryUnlock(player, villager, id);
        }
        if (profession == ModVillagers.NAILER.value()) {
            return NailerUpgrades.tryUnlock(player, villager, id);
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive() && !player.isRemoved();
    }
}
