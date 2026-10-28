package net.gera.veridia.common.entity.ominous.client;

import net.gera.veridia.common.entity.ominous.BrutalOminousWarrior;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Высокий зловещий воин: своя модель и свои текстуры (обычная и в ярости). */
public class BrutalOminousWarriorRenderer extends OminousWarriorRenderer {
    public BrutalOminousWarriorRenderer(EntityRendererProvider.Context context) {
        super(context, new BrutalOminousWarrior(context.bakeLayer(BrutalOminousWarrior.LAYER_LOCATION)),
                texture("brutal_ominous_warrior"), texture("brutal_ominous_warrior_rage"));
    }
}
