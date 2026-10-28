package net.gera.veridia.common.client.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
@SuppressWarnings("deprecation")
@Mixin(SimpleBakedModel.class)
public class SimpleBakedModelMixin {
    @Inject(
            method = "getRenderTypes(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void veridia$replaceChunkItemRenderTypes(ItemStack stack, boolean fabulous,
                                                          CallbackInfoReturnable<List<RenderType>> cir) {
        List<RenderType> types = cir.getReturnValue();
        if (types == null || types.isEmpty()) return;

        List<RenderType> fixed = null;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).format() != DefaultVertexFormat.BLOCK) continue;

            if (fixed == null) {
                fixed = new ArrayList<>(types);
            }
            fixed.set(i, ItemBlockRenderTypes.getRenderType(stack, fabulous));
        }

        if (fixed != null) {
            cir.setReturnValue(fixed);
        }
    }
}