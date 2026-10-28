package net.gera.veridia.common.worldgen.biome.mixin;

import net.gera.veridia.common.worldgen.biome.AshLands;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import terrablender.worldgen.IExtendedParameterList;

import java.util.Map;

/**
 * Пепельные земли в выборе биома (см. AshLands): внутри земель наземные биомы заменяются пепельными, а вокруг них, в
 * жарком поясе, климат смещается к жаркому и сухому, чтобы рядом не лежал снег. TerraBlender ставит свой результат в
 * самом начале метода и тут же возвращает его, поэтому "на выходе" вставиться нельзя: наш обработчик идёт раньше
 * TerraBlender (меньший priority), сам выбирает обычный биом и решает, подменять ли его. Вложенный вызов проходит мимо
 * нас (флаг IN_REPLACEMENT на поток), иначе вышла бы бесконечная рекурсия.
 * <p>
 * Работает только у источников, где есть пепельные биомы (то есть у Верхнего мира, а не Нижнего). Вне земель и пояса
 * (почти весь мир) стоит только быстрая проверка координат.
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 500)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique
    private static final ThreadLocal<Boolean> IN_REPLACEMENT = ThreadLocal.withInitial(() -> false);

    // Пепельные биомы этого источника; ищутся при первом обращении (у Нижнего мира их нет)
    @Unique
    private volatile Map<ResourceKey<Biome>, Holder<Biome>> veridia$ashHolders;

    @Shadow
    public abstract Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler);

    @Shadow
    public abstract Climate.ParameterList<Holder<Biome>> parameters();

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true)
    private void veridia$ashLands(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
                                  CallbackInfoReturnable<Holder<Biome>> callback) {
        // Центр квадрата биома 4x4
        int x = QuartPos.toBlock(quartX) + 2;
        int z = QuartPos.toBlock(quartZ) + 2;
        if (IN_REPLACEMENT.get()) {
            return;
        }
        double heat = AshLands.heat(x, z);
        if (heat <= 0.0D || veridia$ashHolders().isEmpty()) {
            return;
        }
        IN_REPLACEMENT.set(true);
        try {
            Holder<Biome> original = veridia$climateBiome(quartX, quartY, quartZ, sampler, heat);
            callback.setReturnValue(veridia$replace(original, x, z));
        } finally {
            IN_REPLACEMENT.set(false);
        }
    }

    // Обычный биом точки, но по климату, смещённому к жаркому на долю heat (так же, как это делает TerraBlender)
    @Unique
    @SuppressWarnings("unchecked")
    private Holder<Biome> veridia$climateBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler, double heat) {
        Climate.TargetPoint heated = AshLands.heatedClimate(sampler.sample(quartX, quartY, quartZ), heat);
        return ((IExtendedParameterList<Holder<Biome>>) parameters()).findValuePositional(heated, quartX, quartY, quartZ);
    }

    // Пепельный биом вместо исходного или сам исходный, если заменять не надо
    @Unique
    private Holder<Biome> veridia$replace(Holder<Biome> original, int x, int z) {
        ResourceKey<Biome> ashBiome = AshLands.replacement(original, x, z);
        Holder<Biome> holder = ashBiome == null ? null : veridia$ashHolders().get(ashBiome);
        return holder == null ? original : holder;
    }

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> veridia$ashHolders() {
        Map<ResourceKey<Biome>, Holder<Biome>> holders = veridia$ashHolders;
        if (holders == null) {
            holders = AshLands.findAshHolders(((BiomeSource) (Object) this).possibleBiomes());
            veridia$ashHolders = holders;
        }
        return holders;
    }
}
