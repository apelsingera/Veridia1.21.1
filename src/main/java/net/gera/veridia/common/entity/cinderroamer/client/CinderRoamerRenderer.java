package net.gera.veridia.common.entity.cinderroamer.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamer;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Пеплоход. Сёдла нарисованы на текстуре: без седла, с одним (переднее), с двумя; с рюкзаками - своя текстура (на ней
 * оба седла и сами рюкзаки).
 */
public class CinderRoamerRenderer extends MobRenderer<CinderRoamerEntity, CinderRoamer> {
    private static final ResourceLocation TEXTURE = texture("cinderroamer.png");
    private static final ResourceLocation ONE_SADDLE_TEXTURE = texture("cinderroamer_1saddle.png");
    private static final ResourceLocation TWO_SADDLES_TEXTURE = texture("cinderroamer_2saddles.png");
    private static final ResourceLocation BACKPACKS_TEXTURE = texture("cinderroamer_backpacks.png");
    private static final float SHADOW_RADIUS = 1.2F;

    public CinderRoamerRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderRoamer(context.bakeLayer(CinderRoamer.LAYER_LOCATION)), SHADOW_RADIUS);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/cinderroamer/" + name);
    }

    @Override
    public ResourceLocation getTextureLocation(CinderRoamerEntity entity) {
        if (entity.getBackpacks() > 0) {
            return BACKPACKS_TEXTURE;
        }
        return switch (entity.getSaddles()) {
            case 0 -> TEXTURE;
            case 1 -> ONE_SADDLE_TEXTURE;
            default -> TWO_SADDLES_TEXTURE;
        };
    }
}
