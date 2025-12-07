package com.eisiadev.instanceapi.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffDeleteInstance extends Effect {
    
    private Expression<String> worldName;
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, SkriptParser.@NotNull ParseResult parseResult) {
        worldName = (Expression<String>) exprs[0];
        return true;
    }
    
    @Override
    protected void execute(@NotNull Event event) {
        String world = worldName.getSingle(event);
        
        if (world == null) {
            return;
        }
        
        InstanceAPIPlugin.getInstance().getInstanceManager().deleteInstance(world);
    }
    
    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "delete instance " + worldName.toString(event, debug);
    }
}