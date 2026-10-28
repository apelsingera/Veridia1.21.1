package net.gera.veridia.common.recipe.mythicaltable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record SlotIngredient(Ingredient ingredient, boolean required, int count, int durability) {

    public static final Codec<SlotIngredient> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC.fieldOf("item").forGetter(SlotIngredient::ingredient),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(SlotIngredient::required),
            Codec.INT.optionalFieldOf("count", 1).forGetter(SlotIngredient::count),
            Codec.INT.optionalFieldOf("durability", 0).forGetter(SlotIngredient::durability)
    ).apply(inst, SlotIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SlotIngredient::ingredient,
            ByteBufCodecs.BOOL, SlotIngredient::required,
            ByteBufCodecs.VAR_INT, SlotIngredient::count,
            ByteBufCodecs.VAR_INT, SlotIngredient::durability,
            SlotIngredient::new
    );

    /**
     * durability=0 (по умолчанию, не указано в json) - слот ведёт себя как раньше, предмет
     * потребляется по count (см. MythicalTableBlockEntity.consumeSlot). durability>0 - предмет НЕ
     * потребляется по count, а вместо этого теряет столько прочности за крафт; если оставшейся
     * прочности не хватает - matches() возвращает false, и рецепт просто не подходит (как при
     * нехватке любого другого ингредиента), крафт не идёт.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return !required;
        }
        if (!ingredient.test(stack) || stack.getCount() < count) {
            return false;
        }
        if (durability > 0) {
            return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() >= durability;
        }
        return true;
    }
}