package net.gera.veridia.common.entity.ominous;

import net.minecraft.client.animation.AnimationDefinition;

import static net.gera.veridia.common.entity.ominous.OminousWarriorAnimationParts.LEFT_HAND;
import static net.gera.veridia.common.entity.ominous.OminousWarriorAnimationParts.RIGHT_HAND;

/**
 * Набор анимаций одного вида воина. Обычный и Brutal анимируются одинаково, но анимации у каждого из своего экспорта
 * Blockbench ({@link OminousWarriorAnimation} и {@link BrutalOminousWarriorAnimation}): так их можно менять по отдельности.
 * Части ярости разбираются из rage и rageattack (см. {@link OminousWarriorAnimationParts}).
 */
record OminousWarriorAnimationSet(
        AnimationDefinition walk,
        AnimationDefinition attack1,
        AnimationDefinition attack2,
        AnimationDefinition block,
        AnimationDefinition pray,
        AnimationDefinition death,
        AnimationDefinition rageHandUp,
        // Ярость: ноги и левая рука (их ведёт шаг), поднятая правая рука, удар правой на бегу, качание правой без оружия
        AnimationDefinition rageBody,
        AnimationDefinition rageArmRaised,
        AnimationDefinition rageAttackArm,
        AnimationDefinition rageArmSwing) {

    static final OminousWarriorAnimationSet OMINOUS = of(OminousWarriorAnimation.walk,
            OminousWarriorAnimation.attack_1, OminousWarriorAnimation.attack_2, OminousWarriorAnimation.block,
            OminousWarriorAnimation.pray, OminousWarriorAnimation.death, OminousWarriorAnimation.ragehandup,
            OminousWarriorAnimation.rage, OminousWarriorAnimation.rageattack);

    static final OminousWarriorAnimationSet BRUTAL = of(BrutalOminousWarriorAnimation.walk,
            BrutalOminousWarriorAnimation.attack_1, BrutalOminousWarriorAnimation.attack_2,
            BrutalOminousWarriorAnimation.block, BrutalOminousWarriorAnimation.pray, BrutalOminousWarriorAnimation.death,
            BrutalOminousWarriorAnimation.ragehandup, BrutalOminousWarriorAnimation.rage,
            BrutalOminousWarriorAnimation.rageattack);

    private static OminousWarriorAnimationSet of(AnimationDefinition walk, AnimationDefinition attack1,
                                                 AnimationDefinition attack2, AnimationDefinition block,
                                                 AnimationDefinition pray, AnimationDefinition death,
                                                 AnimationDefinition rageHandUp, AnimationDefinition rage,
                                                 AnimationDefinition rageAttack) {
        return new OminousWarriorAnimationSet(walk, attack1, attack2, block, pray, death, rageHandUp,
                OminousWarriorAnimationParts.without(rage, RIGHT_HAND),
                OminousWarriorAnimationParts.only(rage, true, RIGHT_HAND),
                OminousWarriorAnimationParts.only(rageAttack, false, RIGHT_HAND),
                OminousWarriorAnimationParts.mirrored(rage, LEFT_HAND, RIGHT_HAND));
    }
}
