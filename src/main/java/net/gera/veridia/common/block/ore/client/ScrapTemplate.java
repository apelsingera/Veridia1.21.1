package net.gera.veridia.common.block.ore.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Заготовка торчащего куска: грани предмета "молибденовый скрап" (спрайт с толщиной, как предмет в руке), собранные
 * из его запечённой модели, и форма спрайта. Заготовка лежит как обычный предмет: остриё внизу, грани в долях блока от 0 до 1.
 * {@link ScrapPlanner} задаёт каждому торчащему куску своё положение и поворот, а {@link DecorQuads} применяет их к граням.
 *
 * @param quads грани предмета
 * @param shape остриё и ось спрайта
 */
record ScrapTemplate(List<BakedQuad> quads, ScrapShape shape) {

    static ScrapTemplate from(BakedModel itemModel) {
        RandomSource random = RandomSource.create(0L);
        List<BakedQuad> quads = new ArrayList<>(itemModel.getQuads(null, null, random));
        for (Direction side : Direction.values()) {
            quads.addAll(itemModel.getQuads(null, side, random));
        }
        if (quads.isEmpty()) {
            throw new IllegalStateException("У модели скрапа нет граней");
        }
        ScrapShape shape = ScrapShape.of(quads.get(0).getSprite().contents());
        return new ScrapTemplate(List.copyOf(quads), shape);
    }
}
