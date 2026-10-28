package net.gera.veridia.common.compat.iris;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Iris без жёсткой зависимости: включён ли сейчас шейдерпак. С шейдерпаком кадр собирает сам шейдерпак (обычно со
 * своим свечением), и наши пост-эффекты поверх него только мешают. Sodium без Iris ничего не меняет.
 */
public final class IrisCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "iris";
    private static final String API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";

    @Nullable
    private static Object api;
    @Nullable
    private static Method isShaderPackInUse;
    private static boolean initialized;

    private IrisCompat() {
    }

    public static boolean isShaderPackInUse() {
        if (!initialized) {
            init();
        }
        if (api == null || isShaderPackInUse == null) {
            return false;
        }
        try {
            return (boolean) isShaderPackInUse.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static void init() {
        initialized = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            api = apiClass.getMethod("getInstance").invoke(null);
            isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Iris is loaded but its API was not found; assuming no shader pack", e);
        }
    }
}
