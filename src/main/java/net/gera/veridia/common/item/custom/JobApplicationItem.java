package net.gera.veridia.common.item.custom;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.gera.veridia.common.villager.storage.BindVerdict;
import net.gera.veridia.common.villager.storage.StorageKind;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.gera.veridia.common.villager.storage.WorkerSlots;
import net.gera.veridia.common.villager.storage.WorkerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Этим предметом можно устроить жителя на работу. Страшно
 */
public class JobApplicationItem extends Item {

    /**
     * Привязка "игрок -> выбранный житель" - живёт в памяти сервера, привязана к игроку, а НЕ к
     * NBT стека. Раньше хранилась в самом предмете и переживала выкладывание из руки - отменить
     * привязку можно было только сменой профиля предмета (реролл/новый стек), а не убрав его из
     * руки, хотя игрок ожидал ровно этого ("чтобы отменить job_application надо убрать предмет
     * с руки"). Теперь JobApplicationEvents каждый тик проверяет, держит ли игрок предмет в
     * руке - как только нет, привязка сама снимается (см. cancelLink).
     */
    private static final Map<UUID, UUID> LINKS = new HashMap<>();

    private static final int SEARCH_RADIUS_H = 48;
    private static final int SEARCH_RADIUS_V = 16;
    private static final DustParticleOptions GREEN_BURST =
            new DustParticleOptions(Vec3.fromRGB24(0x33CC33).toVector3f(), 1.3F);

    public JobApplicationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        if (!StorageKind.anyAccepts(villager.getVillagerData().getProfession())) {
            player.displayClientMessage(Component.translatable("item.veridia.job_application.not_a_worker"), true);
            return InteractionResult.FAIL;
        }
        LINKS.put(player.getUUID(), villager.getUUID());

        // Разовая вспышка - подтверждение клика. Непрерывная линия+подсветка склада дальше
        // рисуется в JobApplicationEvents, пока привязка жива (то есть пока предмет в руке - см.
        // JobApplicationEvents.onServerTick/cancelLink) - не таймер на несколько секунд, как было
        // раньше (жалоба "частицы быстро пропадают").
        level.sendParticles(GREEN_BURST, villager.getX(), villager.getY() + villager.getBbHeight() * 0.5, villager.getZ(),
                25, 0.3, 0.4, 0.3, 0.0);

        if (computeTargets(villager, level).isEmpty()) {
            player.displayClientMessage(Component.translatable("item.veridia.job_application.no_storage_nearby"), true);
        }

        return InteractionResult.SUCCESS;
    }

    /** Житель, выбранный этим игроком последним кликом (interactLivingEntity), или null, если
     * привязки нет/она уже снята (см. cancelLink). */
    @Nullable
    public static UUID linkedVillager(UUID playerId) {
        return LINKS.get(playerId);
    }

    /** Снимает привязку игрока - вызывается из JobApplicationEvents, как только игрок перестал
     * держать предмет в руке (это и есть "отмена" job_application) или вышел с сервера. */
    public static void cancelLink(UUID playerId) {
        LINKS.remove(playerId);
    }

    static List<BlockPos> computeTargets(Villager villager, ServerLevel level) {
        List<BlockPos> targets = new ArrayList<>();
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (StorageKind.MATERIALS.accepts(profession)) {
            BlockPos materialsPos = previewTarget(villager, level, StorageKind.MATERIALS,
                    ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class);
            if (materialsPos != null) targets.add(materialsPos);
        }
        if (StorageKind.FOOD.accepts(profession)) {
            BlockPos foodPos = previewTarget(villager, level, StorageKind.FOOD,
                    ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class);
            if (foodPos != null) targets.add(foodPos);
        }
        return targets;
    }

    @Nullable
    private static <T extends BlockEntity & WorkerStorage> BlockPos previewTarget(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass) {
        BlockPos cached = VillagerStorageBinding.resolveCached(villager, level, kind, blockType, beClass);
        if (cached != null) return cached;
        return VillagerStorageBinding.previewNearest(villager, level, kind, blockType, beClass,
                villager.blockPosition(), SEARCH_RADIUS_H, SEARCH_RADIUS_V);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level rawLevel = context.getLevel();
        if (player == null || !player.isShiftKeyDown() || !(rawLevel instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        UUID villagerId = linkedVillager(player.getUUID());
        if (villagerId == null) {
            player.displayClientMessage(Component.translatable("item.veridia.job_application.no_target"), true);
            return InteractionResult.FAIL;
        }
        if (!(level.getEntity(villagerId) instanceof Villager villager)) {
            player.displayClientMessage(Component.translatable("item.veridia.job_application.target_gone"), true);
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.BUILDING_MATERIALS.get())) {
            return toggleBinding(player, villager, level, StorageKind.MATERIALS, pos, BuildingMaterialsBlockEntity.class);
        }
        if (state.is(ModBlocks.FOOD_STORAGE.get())) {
            return toggleBinding(player, villager, level, StorageKind.FOOD, pos, FoodStorageBlockEntity.class);
        }
        return InteractionResult.PASS;
    }

    private static <T extends BlockEntity & WorkerStorage> InteractionResult toggleBinding(
            Player player, Villager villager, ServerLevel level, StorageKind kind, BlockPos pos, Class<T> beClass) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!beClass.isInstance(be)) return InteractionResult.PASS;
        T storage = beClass.cast(be);
        VillagerStorageBinding.pruneGhosts(level, storage);

        if (storage.getWorkers().contains(villager.getUUID())) {
            VillagerStorageBinding.release(villager, level, kind, blockTypeOf(kind), beClass);
            player.displayClientMessage(Component.translatable("item.veridia.job_application.unbound"), true);
            return InteractionResult.SUCCESS;
        }
        // Сначала проверка, потом отвязка от прежнего склада: отказ не должен оставлять жителя вовсе без склада.
        BindVerdict verdict = VillagerStorageBinding.verdict(villager, kind, storage);
        if (verdict != BindVerdict.OK) {
            player.displayClientMessage(refusal(verdict), true);
            return InteractionResult.FAIL;
        }
        VillagerStorageBinding.release(villager, level, kind, blockTypeOf(kind), beClass);
        VillagerStorageBinding.bindDirect(villager, level, kind, pos, storage);
        player.displayClientMessage(Component.translatable("item.veridia.job_application.bound"), true);
        return InteractionResult.SUCCESS;
    }

    private static Component refusal(BindVerdict verdict) {
        return switch (verdict) {
            case WRONG_PROFESSION -> Component.translatable("item.veridia.job_application.wrong_profession");
            case PROFESSION_LIMIT -> Component.translatable("item.veridia.job_application.profession_limit", WorkerSlots.MAX_PER_PROFESSION);
            case FULL, OK -> Component.translatable("item.veridia.job_application.storage_full");
        };
    }

    private static Block blockTypeOf(StorageKind kind) {
        return kind == StorageKind.MATERIALS ? ModBlocks.BUILDING_MATERIALS.get() : ModBlocks.FOOD_STORAGE.get();
    }
}
