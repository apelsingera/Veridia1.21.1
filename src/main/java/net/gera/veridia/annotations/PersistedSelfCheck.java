package net.gera.veridia.annotations;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class PersistedSelfCheck {

    private static final Logger LOG = LogUtils.getLogger();

    private PersistedSelfCheck() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PersistedSelfCheck::run);
    }

    private static void run() {
        Set<String> classNames = new TreeSet<>();
        for (var scanData : ModList.get().getAllScanData()) {
            scanData.getAnnotatedBy(Persisted.class, ElementType.FIELD)
                    .forEach(annotation -> classNames.add(annotation.clazz().getClassName()));
        }

        ClassLoader loader = PersistedSelfCheck.class.getClassLoader();
        int problemCount = 0;

        for (String className : classNames) {
            Class<?> type;
            try {
                type = Class.forName(className, false, loader);
            } catch (Throwable t) {
                LOG.error("[persisted] self-check could not load {}", className, t);
                problemCount++;
                continue;
            }

            List<String> problems = PersistedFields.validate(type);
            for (String problem : problems) {
                LOG.error("[persisted] {}", problem);
            }
            problemCount += problems.size();
        }

        if (problemCount == 0) {
            LOG.info("[persisted] self-check: {} class(es) OK", classNames.size());
        } else {
            LOG.error("[persisted] self-check: {} problem(s) across {} class(es) — see above",
                    problemCount, classNames.size());
        }
    }
}