package net.gera.veridia.common.entity.mutant;

import net.gera.veridia.Veridia;
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

public class Mutant extends HierarchicalModel<MutantEntity> {

	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "mutant"), "main");
	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart body;
	private final ModelPart heads;
	private final ModelPart head1;
	private final ModelPart head2;
	private final ModelPart torso_n_neck;
	private final ModelPart arms;
	private final ModelPart left_arm;
	private final ModelPart left_arm_cube;
	private final ModelPart left_arm2;
	private final ModelPart left_arm2_cube;
	private final ModelPart right_arm;
	private final ModelPart legs;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public Mutant(ModelPart root) {
		this.root = root;
		this.all = root.getChild("all");
		this.body = this.all.getChild("body");
		this.heads = this.body.getChild("heads");
		this.head1 = this.heads.getChild("head1");
		this.head2 = this.heads.getChild("head2");
		this.torso_n_neck = this.body.getChild("torso_n_neck");
		this.arms = this.body.getChild("arms");
		this.left_arm = this.arms.getChild("left_arm");
		this.left_arm_cube = this.left_arm.getChild("cube_r4");
		this.left_arm2 = this.arms.getChild("left_arm2");
		this.left_arm2_cube = this.left_arm2.getChild("cube_r5");
		this.right_arm = this.arms.getChild("right_arm");
		this.legs = this.all.getChild("legs");
		this.left_leg = this.legs.getChild("left_leg");
		this.right_leg = this.legs.getChild("right_leg");
	}

	public ModelPart getAll() {
		return this.all;
	}

	public ModelPart getBody() {
		return this.body;
	}

	public ModelPart getArms() {
		return this.arms;
	}

	public ModelPart getLeftArm() {
		return this.left_arm;
	}

	public ModelPart getLeftArmCube() {
		return this.left_arm_cube;
	}

	public ModelPart getLeftArm2() {
		return this.left_arm2;
	}

	public ModelPart getLeftArm2Cube() {
		return this.left_arm2_cube;
	}

	public ModelPart getLeftLeg() {
		return this.left_leg;
	}

	public ModelPart getRightLeg() {
		return this.right_leg;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(1.0F, 24.0F, -4.0F));

		PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-2.0F, -12.0F, 0.0F));

		PartDefinition heads = body.addOrReplaceChild("heads", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition head1 = heads.addOrReplaceChild("head1", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.5F, -1.5F));

		PartDefinition cube_r1 = head1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 8).addBox(-6.0F, -33.75F, -2.5F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 24.75F, 8.5F, 0.3491F, 0.0F, 0.0F));

		PartDefinition head2 = heads.addOrReplaceChild("head2", CubeListBuilder.create(), PartPose.offset(2.0F, -1.5F, 8.0F));

		PartDefinition cube_r2 = head2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, -33.5F, 2.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 21.75F, -15.0F, -0.3491F, 0.0F, 0.0F));

		PartDefinition torso_n_neck = body.addOrReplaceChild("torso_n_neck", CubeListBuilder.create().texOffs(32, 8).addBox(2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -12.0F, 2.0F));

		PartDefinition cube_r3 = torso_n_neck.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(32, 28).addBox(-3.0F, -6.0F, -7.0F, 3.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.0F, 4.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition arms = body.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(-2.0F, -8.0F, 8.0F));

		PartDefinition left_arm = arms.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, 0.0F));

		PartDefinition cube_r4 = left_arm.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(48, 39).addBox(-4.0F, -12.0F, 0.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition left_arm2 = arms.addOrReplaceChild("left_arm2", CubeListBuilder.create(), PartPose.offset(-1.0F, -4.0F, 6.0F));

		PartDefinition cube_r5 = left_arm2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -4.0F, -3.0F, 17.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.0F, 4.0F, 2.25F, 0.0F, -0.4363F, 0.0F));

		PartDefinition right_arm = arms.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, -8.0F));

		PartDefinition cube_r6 = right_arm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(16, 40).addBox(-4.0F, -12.0F, 0.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 2.0F, -4.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition legs = all.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_leg = legs.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -11.0F, 8.0F));

		PartDefinition right_leg = legs.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 39).addBox(-2.0F, -1.0F, -8.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -11.0F, 8.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(MutantEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.heads.resetPose();
		this.head1.resetPose();
		this.head2.resetPose();
		this.left_arm.resetPose();
		this.left_arm2.resetPose();
		this.right_arm.resetPose();
		this.left_leg.resetPose();
		this.right_leg.resetPose();
		this.body.resetPose();
		this.all.resetPose();

		if (entity.deathAnimationState.isStarted()) {
			this.animate(entity.deathAnimationState, MutantAnimations.death, ageInTicks);
		} else if (entity.healAnimationState.isStarted()) {
			this.animate(entity.healAnimationState, MutantAnimations.heal, ageInTicks);
		} else if (entity.attack1AnimationState.isStarted()) {
			this.animate(entity.attack1AnimationState, MutantAnimations.attack1, ageInTicks);
		} else if (entity.attack2AnimationState.isStarted()) {
			this.animate(entity.attack2AnimationState, MutantAnimations.attack2, ageInTicks);
		} else if (entity.chargeStartAnimationState.isStarted()) {
			this.animate(entity.chargeStartAnimationState, MutantAnimations.attack3headArun, ageInTicks);
		} else if (entity.chargeImpactAnimationState.isStarted()) {
			this.animate(entity.chargeImpactAnimationState, MutantAnimations.attack3headB, ageInTicks);
		} else if (entity.stealAnimationState.isStarted()) {
			this.animate(entity.stealAnimationState, MutantAnimations.attack4, ageInTicks);
		} else if (entity.passItemAnimationState.isStarted()) {
			this.animate(entity.passItemAnimationState, MutantAnimations.passitem, ageInTicks);
		} else if (entity.hitWithItemAnimationState.isStarted()) {
			this.animate(entity.hitWithItemAnimationState, MutantAnimations.attack5, ageInTicks);
		} else if (entity.eatAnimationState.isStarted()) {
			this.animate(entity.eatAnimationState, MutantAnimations.eat, ageInTicks);
		} else if (entity.walkAnimationState.isStarted()) {
			this.animate(entity.walkAnimationState, MutantAnimations.walk1, ageInTicks);
		} else if (entity.idleAnimationState.isStarted()) {
			this.animate(entity.idleAnimationState, entity.idleVariant == 0 ? MutantAnimations.idle_1 : MutantAnimations.idle_2, ageInTicks);
		}
	}

	@Override
	public ModelPart root() {
		return this.root;
	}
}