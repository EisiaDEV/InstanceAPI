package com.eisiadev.instanceapi.skript;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffClearPlayerInstances extends Effect {
    
    private Expression<OfflinePlayer> player;
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        player = (Expression<OfflinePlayer>) exprs[0];
        return true;
    }
    
    @Override
    protected void execute(Event event) {
        OfflinePlayer p = player.getSingle(event);
        
        if (p == null) {
            return;
        }
        
        InstanceAPIPlugin.getInstance().getInstanceManager().clearPlayerInstances(p.getUniqueId());
    }
    
    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "clear instances for " + player.toString(event, debug);
    }
}