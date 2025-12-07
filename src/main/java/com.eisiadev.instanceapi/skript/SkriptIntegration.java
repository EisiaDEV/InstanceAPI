package com.eisiadev.instanceapi.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import com.eisiadev.instanceapi.event.InstanceWorldCreatedEvent;
import com.eisiadev.instanceapi.event.InstanceWorldDeletedEvent;
import org.bukkit.World;

public class SkriptIntegration {

    public static void register() {
        // Register events
        Skript.registerEvent("Instance World Created", SimpleEvent.class, InstanceWorldCreatedEvent.class,
                        "[instance[ |-]]world creat(e|ed)",
                        "instance creat(e|ed)")
                .description("Called when an instance world is created")
                .examples(
                        "on instance world created:",
                        "\tbroadcast \"Instance %event-world% was created!\""
                )
                .since("1.0");

        Skript.registerEvent("Instance World Deleted", SimpleEvent.class, InstanceWorldDeletedEvent.class,
                        "[instance[ |-]]world delet(e|ed)",
                        "instance delet(e|ed)")
                .description("Called when an instance world is deleted")
                .examples(
                        "on instance world deleted:",
                        "\tbroadcast \"Instance %event-string% was deleted!\""
                )
                .since("1.0");

        // Register event values for InstanceWorldCreatedEvent
        EventValues.registerEventValue(InstanceWorldCreatedEvent.class, World.class, new Getter<World, InstanceWorldCreatedEvent>() {
            @Override
            public World get(InstanceWorldCreatedEvent event) {
                return event.getWorld();
            }
        }, 0);

        EventValues.registerEventValue(InstanceWorldCreatedEvent.class, String.class, new Getter<String, InstanceWorldCreatedEvent>() {
            @Override
            public String get(InstanceWorldCreatedEvent event) {
                return event.getWorld().getName();
            }
        }, 0);

        // Register event values for InstanceWorldDeletedEvent
        EventValues.registerEventValue(InstanceWorldDeletedEvent.class, String.class, new Getter<String, InstanceWorldDeletedEvent>() {
            @Override
            public String get(InstanceWorldDeletedEvent event) {
                return event.getWorldName();
            }
        }, 0);

        // Register expressions and effects
        Skript.registerEffect(EffCreateInstance.class,
                "(create|make) [a[n]] instance [world] (from|of|with) template %string% for %player%",
                "(create|make) instance %string% for %player%");

        Skript.registerEffect(EffDeleteInstance.class,
                "(delete|remove) instance [world] %string%");

        Skript.registerEffect(EffClearPlayerInstances.class,
                "clear [all] instances (for|of) %offlineplayer%",
                "force clear instances (for|of) %offlineplayer%");

        Skript.registerExpression(ExprInstanceTemplate.class, String.class,
                ch.njol.skript.lang.ExpressionType.PROPERTY,
                "[the] [instance] template of %world%",
                "%world%'s [instance] template");

        Skript.registerExpression(ExprInstanceOwner.class, org.bukkit.OfflinePlayer.class,
                ch.njol.skript.lang.ExpressionType.PROPERTY,
                "[the] [instance] owner of %world%",
                "%world%'s [instance] owner");

        Skript.registerCondition(CondIsInstance.class,
                "%world% is [an] instance [world]",
                "%world% is(n't| not) [an] instance [world]");
    }
}