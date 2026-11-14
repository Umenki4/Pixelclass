package com.pixelclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.player.ClassActions;
import com.pixelclasses.player.ClassApi;
import com.pixelclasses.player.ClassData;
import com.pixelclasses.player.ClassEffects;
import com.pixelclasses.skill.SkillCaster;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * /class                                     open the class selection window (like Origins)
 * /pixelclasses (alias /pc):
 *   menu | skills | list                     anyone: open class window / skill window / list everything
 *   set <players> <class>                    operator: force a class
 *   clear <players>                          operator: remove the class
 *   equip <players> <slot 1-4> <skill>       operator: equip a skill
 *   cast <players> <skill>                   operator: cast ANY skill for free (test the VFX)
 *   refill <players>                         operator: full mana + reset all cooldowns
 */
public final class ClassCommand {
    private static final SimpleCommandExceptionType UNKNOWN_CLASS =
            new SimpleCommandExceptionType(Component.literal("Unknown class. Use /pixelclasses list"));
    private static final SimpleCommandExceptionType UNKNOWN_SKILL =
            new SimpleCommandExceptionType(Component.literal("Unknown skill. Use /pixelclasses list"));

    private ClassCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("class").executes(ctx -> open(ctx, 0)));

        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("pixelclasses")
                .then(Commands.literal("menu").executes(ctx -> open(ctx, 0)))
                .then(Commands.literal("skills").executes(ctx -> open(ctx, 1)))
                .then(Commands.literal("list").executes(ClassCommand::list))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(idArgument("class", true).executes(ClassCommand::setClass))))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ClassCommand::clear)))
                .then(Commands.literal("equip").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                                        .then(idArgument("skill", false).executes(ClassCommand::equip)))))
                .then(Commands.literal("cast").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(idArgument("skill", false).executes(ClassCommand::cast))))
                .then(Commands.literal("refill").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ClassCommand::refill))));

        dispatcher.register(Commands.literal("pc").redirect(root));
    }

    /** Free-text id argument (so ids with ':' work) with tab-completion. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> idArgument(String name, boolean classes) {
        return Commands.argument(name, StringArgumentType.greedyString()).suggests((ctx, builder) -> {
            Stream<String> ids = (classes ? Definitions.SERVER.classIds() : Definitions.SERVER.skillIds()).stream()
                    .map(ResourceLocation::toString);
            return SharedSuggestionProvider.suggest(ids, builder);
        });
    }

    private static ResourceLocation resolve(String text) {
        String s = text.trim();
        return ResourceLocation.tryParse(s.contains(":") ? s : "pixelclasses:" + s);
    }

    private static int open(CommandContext<CommandSourceStack> ctx, int kind) throws CommandSyntaxException {
        ModNetwork.openScreen(ctx.getSource().getPlayerOrException(), kind);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        for (ClassDef cls : Definitions.SERVER.classes()) {
            StringBuilder skills = new StringBuilder();
            for (SkillDef s : Definitions.SERVER.skillsOf(cls.id())) {
                if (skills.length() > 0) {
                    skills.append(", ");
                }
                skills.append(s.id());
            }
            String line = cls.id() + " (" + cls.name().english() + "): " + skills;
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int setClass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ClassDef cls = Definitions.SERVER.classDef(resolve(StringArgumentType.getString(ctx, "class")));
        if (cls == null) {
            throw UNKNOWN_CLASS.create();
        }
        for (ServerPlayer p : targets) {
            ClassActions.setClass(p, cls);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Set class " + cls.id() + " for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            ClassActions.clearClass(p);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Removed the class of " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int equip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
        SkillDef skill = Definitions.SERVER.skillDef(resolve(StringArgumentType.getString(ctx, "skill")));
        if (skill == null) {
            throw UNKNOWN_SKILL.create();
        }
        for (ServerPlayer p : targets) {
            ClassActions.equip(p, slot, skill.id().toString());
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Equipped " + skill.id() + " in slot " + (slot + 1)
                + " (it must belong to the player's class)."), true);
        return targets.size();
    }

    private static int cast(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        SkillDef skill = Definitions.SERVER.skillDef(resolve(StringArgumentType.getString(ctx, "skill")));
        if (skill == null) {
            throw UNKNOWN_SKILL.create();
        }
        for (ServerPlayer p : targets) {
            SkillCaster.cast(p, skill, true);
        }
        return targets.size();
    }

    private static int refill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            ClassData data = ClassApi.get(p);
            ClassDef cls = ClassEffects.current(p);
            data.setMana(cls == null ? 0.0F : (float) cls.maxMana());
            data.clearCooldowns();
            ModNetwork.sendPlayer(p);
            ModNetwork.sendCooldown(p, "*", 0);   // "*" tells the client to clear all cooldown timers
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Refilled mana and cleared cooldowns of " + targets.size() + " player(s)."), true);
        return targets.size();
    }
}
