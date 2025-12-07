package com.eisiadev.instanceapi.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffCreateInstance extends Effect {
    
    private Expression<String> template;
    private Expression<Player> player;
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, SkriptParser.@NotNull ParseResult parseResult) {
        template = (Expression<String>) exprs[0];
        player = (Expression<Player>) exprs[1];
        return true;
    }
    
    @Override
    protected void execute(@NotNull Event event) {
        String templateName = template.getSingle(event);
        Player p = player.getSingle(event);
        
        if (templateName == null || p == null) {
            return;
        }
        
        InstanceAPIPlugin.getInstance().getInstanceManager().createInstance(p, templateName);
    }
    
    @Override
    public @NotNull String toString(@Nullable Event event, boolean debug) {
        return "create instance " + template.toString(event, debug) + " for " + player.toString(event, debug);
    }
}