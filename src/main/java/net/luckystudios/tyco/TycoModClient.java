package net.luckystudios.tyco;

import net.luckystudios.tyco.client.TycoBlockColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TycoMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TycoMod.MODID, value = Dist.CLIENT)
public class TycoModClient {
    public TycoModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(TycoBlockColors::register);
        modEventBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(net.luckystudios.tyco.block.entity.ModBlockEntities.GENERATOR_BE.get(),
                    net.luckystudios.tyco.client.GeneratorScreenRenderer::new);
        });
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }


}