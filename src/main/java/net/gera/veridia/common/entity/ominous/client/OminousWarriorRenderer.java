package net.gera.veridia.common.entity.ominous.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ominous.OminousWarrior;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Зловещий воин; высокий (Brutal) рисуется тем же классом со своей моделью и текстурами. */
public class OminousWarriorRenderer extends MobRenderer<OminousWarriorEntity, OminousWarrior> {
    private static final ResourceLocation TEXTURE = texture("ominous_warrior");
    private static final ResourceLocation RAGE_TEXTURE = texture("ominous_warrior_rage");
    private static final float SHADOW_RADIUS = 0.6F;

    private final ResourceLocation texture;
    private final ResourceLocation rageTexture;

    public OminousWarriorRenderer(EntityRendererProvider.Context context) {
        this(context, new OminousWarrior(context.bakeLayer(OminousWarrior.LAYER_LOCATION)), TEXTURE, RAGE_TEXTURE);
    }

    protected OminousWarriorRenderer(EntityRendererProvider.Context context, OminousWarrior model,
                                     ResourceLocation texture, ResourceLocation rageTexture) {
        super(context, model, SHADOW_RADIUS);
        this.texture = texture;
        this.rageTexture = rageTexture;
        this.addLayer(new OminousWarriorHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    protected static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/ominous/" + name + ".png");
    }

    // Падение мёртвого моба рисует сама анимация death, ванильный поворот набок не нужен
    @Override
    protected float getFlipDegrees(OminousWarriorEntity entity) {
        return 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(OminousWarriorEntity entity) {
        return entity.isRaging() ? rageTexture : texture;
    }
}
