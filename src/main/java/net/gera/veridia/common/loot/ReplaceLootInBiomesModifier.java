package net.gera.veridia.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Optional;

/**
 * Если лут добыт в одном из биомов (по точке ORIGIN: у рыбалки это поплавок), весь лут заменяется результатом
 * другой таблицы. Обычные условия модификатора (например, id исходной таблицы) проверяются до этого.
 */
public class ReplaceLootInBiomesModifier extends LootModifier {
    public static final MapCodec<ReplaceLootInBiomesModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(inst.group(
                    ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(e -> e.biomes),
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(e -> e.lootTable)
            )).apply(inst, ReplaceLootInBiomesModifier::new));

    private final List<ResourceKey<Biome>> biomes;
    private final ResourceKey<LootTable> lootTable;

    public ReplaceLootInBiomesModifier(LootItemCondition[] conditions, List<ResourceKey<Biome>> biomes,
                                       ResourceKey<LootTable> lootTable) {
        super(conditions);
        this.biomes = biomes;
        this.lootTable = lootTable;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!isInListedBiome(context)) {
            return generatedLoot;
        }
        Optional<Holder.Reference<LootTable>> replacement = context.getResolver().get(Registries.LOOT_TABLE, lootTable);
        if (replacement.isEmpty()) {
            return generatedLoot;
        }
        ObjectArrayList<ItemStack> replaced = new ObjectArrayList<>();
        // Raw: вариант getRandomItems сам применяет глобальные модификаторы, и этот снова сработал бы на том же контексте
        // (id исходной таблицы в нём прежний) - бесконечная рекурсия и StackOverflowError
        replacement.get().value().getRandomItemsRaw(context, replaced::add);
        return replaced;
    }

    private boolean isInListedBiome(LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            return false;
        }
        Holder<Biome> biome = context.getLevel().getBiome(BlockPos.containing(origin));
        return biomes.stream().anyMatch(biome::is);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
