package net.gera.veridia.common.effect.onentity;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import java.util.List;
public final class TagEffects {
    public static final List<Tag> TAGS = List.of(
            tag("speed",
                    effect(MobEffects.MOVEMENT_SPEED, 2),
                    effect(MobEffects.DOLPHINS_GRACE, 3)
            ),
            tag("speed2",
                    effect(MobEffects.MOVEMENT_SPEED, 3
                    ),
                    effect(MobEffects.DOLPHINS_GRACE, 3)
            ),
            tag("tanky",
                    effect(MobEffects.DAMAGE_RESISTANCE, 2),
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 1)
            ),
            tag("strength1",
                    effect(MobEffects.DAMAGE_BOOST, 1)
            ),
            tag("selfslow0",
                    effect(MobEffects.MOVEMENT_SLOWDOWN, 1)
            )
    );
    public record Effect(Holder<MobEffect> effect, int amplifier) {}
    public record Tag(String name, List<Effect> effects) {}
    private static Tag tag(String name, Effect... effects) {
        return new Tag(name, List.of(effects));
    }
    private static Effect effect(Holder<MobEffect> effect, int level) {
        return new Effect(effect, level - 1);
    }
    private TagEffects() {}
}