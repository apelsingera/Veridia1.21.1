package net.gera.veridia.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class ReplaceItemModifier extends LootModifier {
    public static final MapCodec<ReplaceItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(inst.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("from").forGetter(m -> m.from),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(m -> m.to)
            )).apply(inst, ReplaceItemModifier::new));

    private final Item from;
    private final Item to;

    public ReplaceItemModifier(LootItemCondition[] conditionsIn, Item from, Item to) {
        super(conditionsIn);
        this.from = from;
        this.to = to;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(from)) {
                generatedLoot.set(i, new ItemStack(to, stack.getCount()));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}