package com.eisiadev.instanceapi.skript;

import ch.njol.skript.conditions.base.PropertyCondition;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class CondIsInstance extends PropertyCondition<World> {
    
    @Override
    public boolean check(@NotNull World world) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().isInstanceWorld(world);
    }
    
    @Override
    protected @NotNull String getPropertyName() {
        return "instance world";
    }
}