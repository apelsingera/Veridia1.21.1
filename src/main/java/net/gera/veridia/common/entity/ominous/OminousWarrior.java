package net.gera.veridia.common.entity.ominous;

import com.mojang.blaze3d.vertex.PoseStack;
import net.gera.veridia.Veridia;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Модель зловещего воина ростом с игрока (Blockbench). От экспорта отличается только тем, что стоит ровно по центру
 * хитбокса: в Blockbench вся модель была сдвинута на 2 пикселя по X (part all стоял в x = 0, а тело симметрично
 * вокруг x = -2). Высокий воин - {@link BrutalOminousWarrior}, анимируется так же.
 */
public class OminousWarrior extends HierarchicalModel<OminousWarriorEntity> implements ArmedModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior"), "main");

	// Сколько раз за пройденный путь проигрывается шаг: бег - та же анимация walk, только быстрее
	private static final float WALK_ANIMATION_SPEED = 2.0F;
	private static final float RUN_ANIMATION_SPEED = 2.6F;
	private static final float RAGE_ANIMATION_SPEED = 2.6F;
	protected static final float WALK_AMPLITUDE_SCALE = 2.5F;

	// Нижний блок руки стоит на оси вращения, а у ванильной руки он сдвинут на пиксель наружу; ванильный вывод
	// предмета этот пиксель учитывает, здесь его возвращаем, чтобы предмет был по центру кисти
	private static final float HAND_CENTER_CORRECTION_PIXELS = 1.0F;
	// Ванильный вывод предмета ставит его на 10 пикселей ниже плеча; у этой модели кисть кончается на 11
	private static final float HAND_LENGTH_CORRECTION_PIXELS = 1.0F;

	private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0F;

	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart upper_body;
	private final ModelPart head;
	private final ModelPart left_hand;
	private final ModelPart right_hand;
	private final OminousWarriorAnimationSet animations;
	private final float handLengthCorrectionPixels;

	public OminousWarrior(ModelPart root) {
		this(root, OminousWarriorAnimationSet.OMINOUS, HAND_LENGTH_CORRECTION_PIXELS);
	}

	protected OminousWarrior(ModelPart root, OminousWarriorAnimationSet animations, float handLengthCorrectionPixels) {
		this.root = root;
		this.all = root.getChild("all");
		this.upper_body = this.all.getChild("upper_body");
		this.head = this.upper_body.getChild("head");
		this.left_hand = this.upper_body.getChild("left_hand");
		this.right_hand = this.upper_body.getChild("right_hand");
		this.animations = animations;
		this.handLengthCorrectionPixels = handLengthCorrectionPixels;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(2.0F, 24.0F, 0.0F));

		PartDefinition right_leg = all.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 35).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -14.0F, 0.0F));

		PartDefinition left_leg = all.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 0.0F));

		PartDefinition upper_body = all.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-2.0F, -12.0F, 0.0F));

		PartDefinition center = upper_body.addOrReplaceChild("center", CubeListBuilder.create().texOffs(0, 17).addBox(-6.0F, -11.0F, -2.0F, 8.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 44).addBox(-5.0F, -11.0F, -3.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(30, 46).addBox(-5.0F, -11.0F, 2.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 0.0F, 0.0F));

		PartDefinition head = upper_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, 0.0F));

		PartDefinition left_hand = upper_body.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(16, 34).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 19).addBox(-2.5F, 3.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(40, 38).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -10.0F, 0.0F));

		PartDefinition right_hand = upper_body.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -0.9991F, -3.0436F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 10).addBox(-2.5F, 3.0009F, -2.5436F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(40, 29).addBox(-2.0F, 7.0009F, -2.0436F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -10.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(OminousWarriorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		if (entity.deathAnimationState.isStarted()) {
			this.animate(entity.deathAnimationState, animations.death(), ageInTicks);
			return;
		}

		this.head.yRot = netHeadYaw * DEGREES_TO_RADIANS;
		this.head.xRot = headPitch * DEGREES_TO_RADIANS;

		float stepSpeed = entity.isRaging() ? RAGE_ANIMATION_SPEED
				: entity.isRunning() ? RUN_ANIMATION_SPEED : WALK_ANIMATION_SPEED;
		boolean ducking = animateDucking(entity, limbSwing, limbSwingAmount, ageInTicks, stepSpeed);
		if (entity.isRaging()) {
			setupRage(entity, limbSwing, limbSwingAmount, ageInTicks, ducking);
		} else {
			// Шаг идёт всегда (сила зависит от скорости), удары и молитва накладываются сверху
			if (!ducking) {
				this.animateWalk(animations.walk(), limbSwing, limbSwingAmount, stepSpeed, WALK_AMPLITUDE_SCALE);
			}
			this.animate(entity.attack1AnimationState, animations.attack1(), ageInTicks);
			this.animate(entity.attack2AnimationState, animations.attack2(), ageInTicks);
			this.animate(entity.prayAnimationState, animations.pray(), ageInTicks);
		}
		this.animate(entity.blockAnimationState, animations.block(), ageInTicks);
	}

	/**
	 * Приседание (есть только у {@link BrutalOminousWarrior}): если воин пригнулся или выпрямляется, сам ведёт ноги
	 * и наклон и возвращает true - тогда обычный шаг и качание рук не играют.
	 */
	protected boolean animateDucking(OminousWarriorEntity entity, float limbSwing, float limbSwingAmount,
									 float ageInTicks, float stepSpeed) {
		return false;
	}

	// Ярость: ноги и левую руку ведёт шаг (rage), правая рука с оружием поднята (сначала ragehandup, потом держится,
	// на ударе - rageattack), без оружия качается как левая и бьёт обычными ударами. Пригнувшись, ноги ведёт
	// приседание, а руки не качаются
	private void setupRage(OminousWarriorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
						   boolean ducking) {
		if (!ducking) {
			this.animateWalk(animations.rageBody(), limbSwing, limbSwingAmount, RAGE_ANIMATION_SPEED, WALK_AMPLITUDE_SCALE);
		}
		if (!entity.isArmed()) {
			if (!ducking) {
				this.animateWalk(animations.rageArmSwing(), limbSwing, limbSwingAmount, RAGE_ANIMATION_SPEED, WALK_AMPLITUDE_SCALE);
			}
			this.animate(entity.attack1AnimationState, animations.attack1(), ageInTicks);
			this.animate(entity.attack2AnimationState, animations.attack2(), ageInTicks);
		} else if (entity.rageHandUpAnimationState.isStarted()) {
			this.animate(entity.rageHandUpAnimationState, animations.rageHandUp(), ageInTicks);
		} else if (entity.rageAttackAnimationState.isStarted()) {
			this.animate(entity.rageAttackAnimationState, animations.rageAttackArm(), ageInTicks);
		} else {
			this.applyStatic(animations.rageArmRaised());
		}
	}

	@Override
	public ModelPart root() {
		return this.root;
	}

	/**
	 * Ставит систему координат в руку так, как её ждёт ванильный вывод предмета ({@code ItemInHandLayer}): начало в
	 * плече, вниз - плюс Y, вперёд - минус Z. Группы модели не повёрнуты, поэтому хватает пройти по цепочке частей.
	 */
	@Override
	public void translateToHand(HumanoidArm side, PoseStack poseStack) {
		boolean left = side == HumanoidArm.LEFT;
		this.all.translateAndRotate(poseStack);
		this.upper_body.translateAndRotate(poseStack);
		(left ? this.left_hand : this.right_hand).translateAndRotate(poseStack);
		poseStack.translate((left ? -HAND_CENTER_CORRECTION_PIXELS : HAND_CENTER_CORRECTION_PIXELS) / 16.0F,
				handLengthCorrectionPixels / 16.0F, 0.0F);
	}
}
