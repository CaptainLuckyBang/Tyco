package net.luckystudios.tyco.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.luckystudios.tyco.shop.UnlockedCategoriesData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class ShopCommands {
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("tyco")
                .then(Commands.literal("resetshoplocks")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                // No category specified - reset everything for this player
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    ServerLevel level = target.serverLevel();
                                    UnlockedCategoriesData.get(level).resetAll(target.getUUID());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Reset all shop tab unlocks for " + target.getName().getString()
                                    ), true);
                                    return 1;
                                })
                                // Category specified - reset just that one
                                .then(Commands.argument("category", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            String category = StringArgumentType.getString(context, "category");
                                            ServerLevel level = target.serverLevel();
                                            UnlockedCategoriesData.get(level).resetCategory(target.getUUID(), category);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Reset shop tab '" + category + "' for " + target.getName().getString()
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}