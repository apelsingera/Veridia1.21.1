package net.gera.veridia.common.entity.cinderroamer;

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
import net.minecraft.util.Mth;

/**
 * Модель пеплохода (экспорт из Blockbench). Единственная правка экспорта - группа all сдвинута на 2 px по x: модель
 * была отцентрована по x = -2.
 * <p>
 * Шипы на спине sspike1..5 прячутся по одному по мере спиливания (CinderRoamerEntity#getSawnSpikes), спереди назад.
 * Рюкзаки backpack1..3 (из экспорта CinderRoamerBackpacks) появляются по одному (CinderRoamerEntity#getBackpacks).
 * Шаг и бег - одна анимация walk, бег просто играет её быстрее, переход плавный. Лёжа (сон или «лежать» по команде хозяина) -
 * sleepstart, потом sleeping, при подъёме sleepend.
 */
public class CinderRoamer extends HierarchicalModel<CinderRoamerEntity> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "cinder_roamer"), "main");

	// Насколько сильно играет шаг в зависимости от того, как быстро моб идёт: на ходу - в полную силу, при остановке
	// плавно гаснет (время анимации и скорость её проигрывания - в CinderRoamerEntity#getWalkAnimationTicks)
	private static final float WALK_AMPLITUDE_SCALE = 2.5F;
	// Насколько голова поворачивается за взглядом (доля от поворота головы сущности)
	private static final float HEAD_LOOK_SCALE = 0.6F;

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart[] backSpikes;
	private final ModelPart[] backpacks;

	public CinderRoamer(ModelPart root) {
		this.root = root;
		ModelPart all = root.getChild("all");
		this.head = all.getChild("head");
		ModelPart core = all.getChild("core");
		this.backSpikes = numberedParts(core.getChild("spikes"), "sspike", CinderRoamerEntity.BACK_SPIKES);
		this.backpacks = numberedParts(core.getChild("backpacks"), "backpack", CinderRoamerEntity.MAX_BACKPACKS);
	}

	// Части prefix1..prefixN
	private static ModelPart[] numberedParts(ModelPart group, String prefix, int count) {
		ModelPart[] parts = new ModelPart[count];
		for (int i = 0; i < count; i++) {
			parts[i] = group.getChild(prefix + (i + 1));
		}
		return parts;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(-0.5F, 5.0F, 0.0F));

		PartDefinition dontmove = all.addOrReplaceChild("dontmove", CubeListBuilder.create().texOffs(46, 154).addBox(-6.4F, -4.8F, -4.0F, 14.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(41, 109).addBox(-0.5F, 0.0F, 45.0F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -22.0F));

		PartDefinition head = all.addOrReplaceChild("head", CubeListBuilder.create().texOffs(39, 121).addBox(-6.9F, -10.0F, -11.0F, 15.0F, 12.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(5, 89).addBox(8.1F, 0.0F, -5.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(4, 96).addBox(8.2F, 0.0F, -5.0F, 0.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(5, 93).addBox(-6.9F, 0.0F, -5.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(3, 95).addBox(-7.0F, 0.0F, -5.0F, 0.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, -20.0F));

		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(-0.9F, 4.0F, 2.25F));

		PartDefinition cube_r1 = jaw.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 148).addBox(-6.0F, -3.3486F, -12.9848F, 15.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition spikeshead = head.addOrReplaceChild("spikeshead", CubeListBuilder.create(), PartPose.offset(0.6F, -3.5F, -12.0F));

		PartDefinition cube_r2 = spikeshead.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(98, 36).addBox(-1.0F, -2.0F, -14.0F, 2.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.9F, -2.75F, 8.0F, -0.2256F, -0.2555F, 0.0579F));

		PartDefinition cube_r3 = spikeshead.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(76, 91).addBox(-1.0F, -2.0F, -14.0F, 2.0F, 2.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1F, -2.75F, 8.0F, -0.2256F, 0.2555F, -0.0579F));

		PartDefinition cube_r4 = spikeshead.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(79, 94).addBox(-1.0F, -3.0F, -7.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition core = all.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 36).addBox(0.0F, -10.0F, -42.0F, 28.0F, 14.0F, 21.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-1.0F, -9.0F, -21.0F, 30.0F, 13.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offset(-13.5F, 5.0F, 21.0F));

		PartDefinition spikes = core.addOrReplaceChild("spikes", CubeListBuilder.create(), PartPose.offset(14.0F, -4.9F, -20.0F));

		PartDefinition sspike3 = spikes.addOrReplaceChild("sspike3", CubeListBuilder.create().texOffs(106, 28).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(92, 104).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition sspike2 = spikes.addOrReplaceChild("sspike2", CubeListBuilder.create().texOffs(54, 109).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 111).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -8.0F));

		PartDefinition sspike1 = spikes.addOrReplaceChild("sspike1", CubeListBuilder.create().texOffs(0, 111).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(84, 114).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -16.0F));

		PartDefinition sspike4 = spikes.addOrReplaceChild("sspike4", CubeListBuilder.create().texOffs(16, 111).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(108, 115).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.3F, 8.0F));

		PartDefinition sspike5 = spikes.addOrReplaceChild("sspike5", CubeListBuilder.create().texOffs(92, 111).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(51, 103).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.3F, 16.0F));

		PartDefinition sidespikes = core.addOrReplaceChild("sidespikes", CubeListBuilder.create().texOffs(98, 66).addBox(-5.0F, -2.0F, -1.0F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(114, 66).addBox(-4.0F, -2.0F, 3.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(108, 111).addBox(27.1F, -2.0F, -1.0F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(70, 114).addBox(27.2F, -2.0F, 3.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, -37.0F));

		// Рюкзаки из экспорта CinderRoamerBackpacks; там backpack1 лежал прямо в группе backpacks - здесь у каждого
		// своя часть, чтобы их можно было показывать по одному. Текстура - cinderroamer_backpacks.png
		PartDefinition backpacks = core.addOrReplaceChild("backpacks", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		backpacks.addOrReplaceChild("backpack1", CubeListBuilder.create().texOffs(23, 79).addBox(28.0F, -8.0F, -32.0F, 4.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		backpacks.addOrReplaceChild("backpack2", CubeListBuilder.create().texOffs(23, 79).addBox(-1.0F, -5.0F, -3.0F, 4.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.0F, -2.0F, 0.0F, 3.1416F, 0.0F));

		backpacks.addOrReplaceChild("backpack3", CubeListBuilder.create().texOffs(23, 79).addBox(-1.0F, -5.0F, -3.0F, 4.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -10.0F, -16.0F, 1.5708F, 0.0F, -1.5708F));

		PartDefinition legs = all.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(9.5F, 19.0F, 17.0F));

		PartDefinition front_legs = legs.addOrReplaceChild("front_legs", CubeListBuilder.create(), PartPose.offset(-17.0F, -12.0F, -32.0F));

		PartDefinition frontrightleg = front_legs.addOrReplaceChild("frontrightleg", CubeListBuilder.create().texOffs(126, 134).addBox(-5.0F, 0.0F, -5.0F, 8.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition frontleftleg = front_legs.addOrReplaceChild("frontleftleg", CubeListBuilder.create().texOffs(90, 134).addBox(-3.0F, 0.0F, -5.0F, 8.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(16.0F, 0.0F, 0.0F));

		PartDefinition backlegs = legs.addOrReplaceChild("backlegs", CubeListBuilder.create(), PartPose.offset(-18.0F, -12.0F, 0.0F));

		PartDefinition backrightleg = backlegs.addOrReplaceChild("backrightleg", CubeListBuilder.create().texOffs(126, 134).addBox(-4.0F, 0.0F, -5.0F, 8.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition backleftleg = backlegs.addOrReplaceChild("backleftleg", CubeListBuilder.create().texOffs(90, 134).addBox(-4.0F, 0.0F, -5.0F, 8.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(18.0F, 0.0F, 0.0F));

		PartDefinition tail = all.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.5F, 3.0F, 25.4F));

		PartDefinition tailsubs = tail.addOrReplaceChild("tailsubs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -1.4F));

		PartDefinition starttoend = tailsubs.addOrReplaceChild("starttoend", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition tailstart = starttoend.addOrReplaceChild("tailstart", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tail1 = tailstart.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(106, 0).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tail2 = tailstart.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(76, 104).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 4.0F));

		PartDefinition tail3 = tailstart.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 7.4F));

		PartDefinition tail3_r1 = tail3.addOrReplaceChild("tail3_r1", CubeListBuilder.create().texOffs(102, 91).addBox(-2.0F, -6.0F, -0.5F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition tailend = starttoend.addOrReplaceChild("tailend", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 7.4F));

		PartDefinition tail4 = tailend.addOrReplaceChild("tail4", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tail4_r1 = tail4.addOrReplaceChild("tail4_r1", CubeListBuilder.create().texOffs(102, 101).addBox(-2.0F, -5.7172F, 3.4667F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition tail5 = tailend.addOrReplaceChild("tail5", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tail5_r1 = tail5.addOrReplaceChild("tail5_r1", CubeListBuilder.create().texOffs(106, 10).addBox(-2.0F, -4.5652F, 7.3994F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition tail6 = tailend.addOrReplaceChild("tail6", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tail6_r1 = tail6.addOrReplaceChild("tail6_r1", CubeListBuilder.create().texOffs(106, 19).addBox(-4.0F, -9.4384F, 47.3456F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 14.0F, -33.4F, 0.2618F, 0.0F, 0.0F));

		PartDefinition tailcore = tailend.addOrReplaceChild("tailcore", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition core_r1 = tailcore.addOrReplaceChild("core_r1", CubeListBuilder.create().texOffs(98, 49).addBox(-3.0F, -5.5605F, 14.9127F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition spike_r1 = tailcore.addOrReplaceChild("spike_r1", CubeListBuilder.create().texOffs(98, 62).addBox(-7.0F, -1.5045F, -0.8582F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -6.6F, 16.8F, 0.0F, 1.2654F, 1.5708F));

		PartDefinition spike_r2 = tailcore.addOrReplaceChild("spike_r2", CubeListBuilder.create().texOffs(98, 62).addBox(-7.0F, -1.5045F, -0.8582F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -6.6F, 16.8F, 0.0F, -0.2618F, 1.5708F));

		PartDefinition spike_r3 = tailcore.addOrReplaceChild("spike_r3", CubeListBuilder.create().texOffs(98, 62).addBox(-7.0F, -1.5045F, -0.8582F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.1F, 16.8F, 0.2618F, 0.0F, 0.0F));

		PartDefinition origins = tailsubs.addOrReplaceChild("origins", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition origin = origins.addOrReplaceChild("origin", CubeListBuilder.create().texOffs(40, 109).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition origin2 = origins.addOrReplaceChild("origin2", CubeListBuilder.create().texOffs(40, 109).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));

		PartDefinition origin3 = origins.addOrReplaceChild("origin3", CubeListBuilder.create().texOffs(40, 109).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -6.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(CinderRoamerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		for (int i = 0; i < backSpikes.length; i++) {
			backSpikes[i].visible = i >= entity.getSawnSpikes();
		}
		for (int i = 0; i < backpacks.length; i++) {
			backpacks[i].visible = i < entity.getBackpacks();
		}

		if (entity.sleepStartAnimationState.isStarted() || entity.sleepingAnimationState.isStarted()) {
			this.animate(entity.sleepStartAnimationState, CinderRoamerAnimations.sleepstart, ageInTicks);
			this.animate(entity.sleepingAnimationState, CinderRoamerAnimations.sleeping, ageInTicks);
			return;
		}
		this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * HEAD_LOOK_SCALE;
		this.head.xRot = headPitch * Mth.DEG_TO_RAD * HEAD_LOOK_SCALE;

		// Время клипа идёт с его родной скоростью (на бегу быстрее), а не от пройденного пути
		float partialTick = ageInTicks - entity.tickCount;
		this.animateWalk(CinderRoamerAnimations.walk, entity.getWalkAnimationTicks(partialTick), limbSwingAmount,
				1.0F, WALK_AMPLITUDE_SCALE);
		this.animate(entity.sleepEndAnimationState, CinderRoamerAnimations.sleepend, ageInTicks);
		this.animate(entity.biteAnimationState, CinderRoamerAnimations.attack_bite, ageInTicks);
		this.animate(entity.spikeHitAnimationState, CinderRoamerAnimations.attackspikehit, ageInTicks);
		this.animate(entity.tailSmashAnimationState, CinderRoamerAnimations.attack_tail_smash, ageInTicks);
		this.animate(entity.tailSidesAnimationState, CinderRoamerAnimations.attack_tail_sides, ageInTicks);
	}

	@Override
	public ModelPart root() {
		return this.root;
	}
}
