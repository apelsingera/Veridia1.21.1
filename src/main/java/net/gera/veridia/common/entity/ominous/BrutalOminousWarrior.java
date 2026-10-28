package net.gera.veridia.common.entity.ominous;

import net.gera.veridia.Veridia;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import static net.gera.veridia.common.entity.ominous.OminousWarriorAnimationParts.LEFT_LEG;
import static net.gera.veridia.common.entity.ominous.OminousWarriorAnimationParts.RIGHT_LEG;

/**
 * Модель высокого зловещего воина (Blockbench, бывшая модель обычного). Как и у {@link OminousWarrior}, единственная
 * правка экспорта - центровка (part all x 0 -> 2). Всё анимируется так же, плюс приседание в низких проходах:
 * duckdown (пригибается) -> ducking (идёт пригнувшись) -> duckdownend (выпрямляется). Ноги в этих анимациях - тот же шаг,
 * его ведёт пройденный путь, а наклон тела и головы идёт по времени.
 */
public class BrutalOminousWarrior extends OminousWarrior {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "brutal_ominous_warrior"), "main");

	private static final AnimationDefinition DUCK_LEGS =
			OminousWarriorAnimationParts.only(BrutalOminousWarriorAnimation.ducking, true, LEFT_LEG, RIGHT_LEG);
	private static final AnimationDefinition DUCK_DOWN_POSE =
			OminousWarriorAnimationParts.without(BrutalOminousWarriorAnimation.duckdown, LEFT_LEG, RIGHT_LEG);
	private static final AnimationDefinition DUCKING_POSE =
			OminousWarriorAnimationParts.without(BrutalOminousWarriorAnimation.ducking, LEFT_LEG, RIGHT_LEG);
	private static final AnimationDefinition DUCK_UP_POSE =
			OminousWarriorAnimationParts.without(BrutalOminousWarriorAnimation.duckdownend, LEFT_LEG, RIGHT_LEG);

	// Кисть этой модели кончается на 10 пикселях ниже плеча, как у ванильной руки
	private static final float HAND_LENGTH_CORRECTION_PIXELS = 0.0F;

	public BrutalOminousWarrior(ModelPart root) {
		super(root, OminousWarriorAnimationSet.BRUTAL, HAND_LENGTH_CORRECTION_PIXELS);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(2.0F, 24.0F, 0.0F));

		PartDefinition right_leg = all.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -14.0F, 0.0F));

		PartDefinition left_leg = all.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 0.0F));

		PartDefinition upper_body = all.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-2.0F, -14.0F, 0.0F));

		PartDefinition center = upper_body.addOrReplaceChild("center", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, -13.0F, -2.0F, 8.0F, 13.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 44).addBox(-5.0F, -13.0F, -3.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(30, 46).addBox(-5.0F, -13.0F, 2.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 0.0F, 0.0F));

		PartDefinition head = upper_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, 0.0F));

		PartDefinition left_hand = upper_body.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(16, 34).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 19).addBox(-2.5F, 1.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(40, 37).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -10.0F, 0.0F));

		PartDefinition right_hand = upper_body.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -2.9991F, -3.0436F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 10).addBox(-2.5F, 1.0009F, -2.5436F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(40, 28).addBox(-2.0F, 5.0009F, -2.0436F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -10.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	// Пригнулся: сначала duckdown, после его конца держится поза ducking. Выпрямляется: duckdownend до конца анимации.
	// Ноги всё это время идут шагом из этих же анимаций
	@Override
	protected boolean animateDucking(OminousWarriorEntity entity, float limbSwing, float limbSwingAmount,
									 float ageInTicks, float stepSpeed) {
		if (!(entity instanceof BrutalOminousWarriorEntity brutal)) {
			return false;
		}
		if (brutal.isDucking()) {
			if (brutal.duckDownAnimationState.isStarted()) {
				this.animate(brutal.duckDownAnimationState, DUCK_DOWN_POSE, ageInTicks);
			} else {
				this.applyStatic(DUCKING_POSE);
			}
		} else if (brutal.duckUpAnimationState.isStarted()) {
			this.animate(brutal.duckUpAnimationState, DUCK_UP_POSE, ageInTicks);
		} else {
			return false;
		}
		this.animateWalk(DUCK_LEGS, limbSwing, limbSwingAmount, stepSpeed, WALK_AMPLITUDE_SCALE);
		return true;
	}
}
