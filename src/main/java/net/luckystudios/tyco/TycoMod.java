package net.luckystudios.tyco;

import net.luckystudios.tyco.block.entity.ModBlockEntities;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.luckystudios.tyco.recipe.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TycoMod.MODID)
public class TycoMod {
    public static final String MODID = "tyco";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public TycoMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(net.luckystudios.tyco.client.TycoBlockColors::register);
        modEventBus.addListener(net.luckystudios.tyco.network.ModNetworking::register);

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, net.luckystudios.tyco.config.TycoConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var recipeManager = event.getServer().getRecipeManager();
        var allGenerating = recipeManager.getAllRecipesFor(ModRecipes.GENERATING_TYPE.get());
        LOGGER.info("[Tyco] Loaded {} generating recipes: {}", allGenerating.size(),
                allGenerating.stream().map(r -> r.id().toString()).toList());

        var itemRegistry = event.getServer().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
        var tagOpt = itemRegistry.getTag(net.luckystudios.tyco.ModTags.Items.COINS);
        if (tagOpt.isPresent()) {
            LOGGER.info("[Tyco] coins tag FOUND with {} entries: {}", tagOpt.get().size(),
                    tagOpt.get().stream().map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("?")).toList());
        } else {
            LOGGER.info("[Tyco] coins tag NOT FOUND / empty");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        net.luckystudios.tyco.command.ShopCommands.register(event);
    }
}