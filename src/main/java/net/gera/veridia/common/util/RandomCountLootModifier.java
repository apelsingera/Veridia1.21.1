package net.gera.veridia.common.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.minecraft.core.registries.BuiltInRegistries;

public class RandomCountLootModifier extends LootModifier {

    public static final MapCodec<RandomCountLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            LootModifier.codecStart(instance).and(
                    instance.group(
                            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
                            Codec.INT.fieldOf("min").forGetter(m -> m.min),
                            Codec.INT.fieldOf("max").forGetter(m -> m.max)
                    )
            ).apply(instance, RandomCountLootModifier::new)
    );

    private final Item item;
    private final int min;
    private final int max;

    public RandomCountLootModifier(LootItemCondition[] conditionsIn, Item item, int min, int max) {
        super(conditionsIn);
        this.item = item;
        this.min = min;
        this.max = max;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int count = context.getRandom().nextInt(this.max - this.min + 1) + this.min;
        generatedLoot.add(new ItemStack(this.item, count));
        return generatedLoot;
    }

    @Override
    public   MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}