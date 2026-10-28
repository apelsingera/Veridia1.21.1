package net.gera.veridia.common.entity.ominous;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Анимации, разобранные по костям. В Blockbench анимация - целая (ноги, руки, тело), а в игре её части нужно смешивать
 * по-разному:
 * <ul>
 *   <li>ярость: ноги и левая рука из rage ведёт шаг (как walk), поэтому переход между бегом и ударом на бегу не
 *       дёргается; правая рука из rage (поднята) - если в руке оружие; правая рука из rageattack - удар на бегу; без
 *       оружия правая рука не поднимается, а качается зеркально левой;</li>
 *   <li>приседание (Brutal): ноги из duckdown/ducking/duckdownend ведёт шаг, а наклон тела и головы идёт по времени,
 *       поэтому пригнувшийся воин, стоя на месте, не перебирает ногами.</li>
 * </ul>
 */
final class OminousWarriorAnimationParts {
    static final String RIGHT_HAND = "right_hand";
    static final String LEFT_HAND = "left_hand";
    static final String RIGHT_LEG = "right_leg";
    static final String LEFT_LEG = "left_leg";

    private OminousWarriorAnimationParts() {
    }

    /** Анимация без этих костей. */
    static AnimationDefinition without(AnimationDefinition source, String... bones) {
        Map<String, List<AnimationChannel>> kept = new HashMap<>(source.boneAnimations());
        kept.keySet().removeAll(Set.of(bones));
        return new AnimationDefinition(source.lengthInSeconds(), source.looping(), kept);
    }

    /** Только эти кости. */
    static AnimationDefinition only(AnimationDefinition source, boolean looping, String... bones) {
        Map<String, List<AnimationChannel>> kept = new HashMap<>(source.boneAnimations());
        kept.keySet().retainAll(Set.of(bones));
        return new AnimationDefinition(source.lengthInSeconds(), looping, kept);
    }

    /** Кость target двигается так же, как from, только в обратную сторону (руки качаются навстречу друг другу). */
    static AnimationDefinition mirrored(AnimationDefinition source, String from, String target) {
        List<AnimationChannel> mirroredChannels = source.boneAnimations().get(from).stream()
                .map(OminousWarriorAnimationParts::negate)
                .toList();
        return new AnimationDefinition(source.lengthInSeconds(), source.looping(), Map.of(target, mirroredChannels));
    }

    private static AnimationChannel negate(AnimationChannel channel) {
        Keyframe[] keyframes = new Keyframe[channel.keyframes().length];
        for (int i = 0; i < keyframes.length; i++) {
            Keyframe keyframe = channel.keyframes()[i];
            keyframes[i] = new Keyframe(keyframe.timestamp(), new Vector3f(keyframe.target()).negate(), keyframe.interpolation());
        }
        return new AnimationChannel(channel.target(), keyframes);
    }
}
