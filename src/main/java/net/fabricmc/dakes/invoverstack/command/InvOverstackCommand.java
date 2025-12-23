package net.fabricmc.dakes.invoverstack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.dakes.invoverstack.config.ConfigManager;
import net.fabricmc.dakes.invoverstack.config.ModConfig;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Handles the /invoverstack command and all its subcommands.
 */
public class InvOverstackCommand {

    /**
     * Registers the /invoverstack command with all subcommands.
     *
     * @param dispatcher The command dispatcher to register to
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("invoverstack")
                // TODO: Add permission check once we figure out the correct method name in Yarn for 1.21.11
                // Should be: .requires(source -> source.hasPermissionLevel(2))
                .then(literal("reload")
                        .executes(InvOverstackCommand::executeReload))
                .then(literal("set")
                        .then(argument("item", IdentifierArgumentType.identifier())
                                .then(argument("size", IntegerArgumentType.integer(1, 32767))
                                        .executes(InvOverstackCommand::executeSet))))
                .then(literal("info")
                        .then(argument("item", IdentifierArgumentType.identifier())
                                .executes(InvOverstackCommand::executeInfo)))
                .executes(InvOverstackCommand::executeHelp));
    }

    /**
     * Executes /invoverstack (shows help)
     */
    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("§6=== InvOverstack Commands ==="), false);
        source.sendFeedback(() -> Text.literal("§e/invoverstack reload §7- Reload configuration from disk"), false);
        source.sendFeedback(() -> Text.literal("§e/invoverstack set <item> <size> §7- Set custom stack size for an item"), false);
        source.sendFeedback(() -> Text.literal("§e/invoverstack info <item> §7- Show current stack size for an item"), false);

        return 1;
    }

    /**
     * Executes /invoverstack reload
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("§eReloading InvOverstack configuration..."), false);

        boolean success = ConfigManager.reloadConfig();

        if (success) {
            ModConfig config = ConfigManager.getConfig();
            source.sendFeedback(() -> Text.literal(
                    String.format("§aConfiguration reloaded successfully! Default: %d, Max: %d",
                            config.defaultMaxStackSize, config.maxAllowedStackSize)), false);
            return 1;
        } else {
            source.sendError(Text.literal("§cFailed to reload configuration. Check server logs for details."));
            return 0;
        }
    }

    /**
     * Executes /invoverstack set <item> <size>
     */
    private static int executeSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");
        int size = IntegerArgumentType.getInteger(context, "size");

        // Verify item exists
        if (!Registries.ITEM.containsId(itemId)) {
            source.sendError(Text.literal("§cItem not found: " + itemId));
            return 0;
        }

        ModConfig config = ConfigManager.getConfig();

        // Check if size exceeds max allowed
        if (size > config.maxAllowedStackSize) {
            source.sendError(Text.literal(
                    String.format("§cStack size %d exceeds maximum allowed (%d)", size, config.maxAllowedStackSize)));
            return 0;
        }

        // Set the override
        config.perItemOverrides.put(itemId.toString(), size);
        ConfigManager.saveConfig();

        source.sendFeedback(() -> Text.literal(
                String.format("§aSet stack size for §e%s §ato §e%d", itemId, size)), true);

        InvOverstackMod.LOGGER.info("Stack size for {} set to {} by {}", itemId, size, source.getName());

        return 1;
    }

    /**
     * Executes /invoverstack info <item>
     */
    private static int executeInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");

        // Verify item exists and get item
        if (!Registries.ITEM.containsId(itemId)) {
            source.sendError(Text.literal("§cItem not found: " + itemId));
            return 0;
        }

        Item item = Registries.ITEM.get(itemId);
        ModConfig config = ConfigManager.getConfig();

        String itemIdStr = itemId.toString();
        int vanillaMax = item.getMaxCount();
        int configuredSize = config.getStackSizeForItem(itemIdStr);

        source.sendFeedback(() -> Text.literal("§6=== Stack Info: §e" + itemId + " §6==="), false);
        source.sendFeedback(() -> Text.literal("§7Vanilla max stack: §f" + vanillaMax), false);

        if (configuredSize == -1) {
            source.sendFeedback(() -> Text.literal("§cThis item is BLACKLISTED (no modification)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("§7Configured stack size: §a" + configuredSize), false);

            if (config.perItemOverrides.containsKey(itemIdStr)) {
                source.sendFeedback(() -> Text.literal("§7Source: §ePer-item override"), false);
            } else {
                source.sendFeedback(() -> Text.literal("§7Source: §eDefault configuration"), false);
            }
        }

        return 1;
    }
}
