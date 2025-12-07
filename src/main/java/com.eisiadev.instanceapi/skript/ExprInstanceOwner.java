package com.eisiadev.instanceapi.skript;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.data.InstanceData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExprInstanceOwner extends SimplePropertyExpression<World, OfflinePlayer> {
    
    @Override
    @Nullable
    public OfflinePlayer convert(World world) {
        InstanceData data = InstanceAPIPlugin.getInstance().getInstanceManager()
                .getInstanceData(world.getName());
        
        return data != null ? Bukkit.getOfflinePlayer(data.getOwner()) : null;
    }
    
    @Override
    public @NotNull Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }
    
    @Override
    protected @NotNull String getPropertyName() {
        return "instance owner";
    }
}