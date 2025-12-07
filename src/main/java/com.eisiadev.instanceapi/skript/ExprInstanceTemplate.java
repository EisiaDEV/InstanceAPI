package com.eisiadev.instanceapi.skript;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.data.InstanceData;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExprInstanceTemplate extends SimplePropertyExpression<World, String> {
    
    @Override
    @Nullable
    public String convert(World world) {
        InstanceData data = InstanceAPIPlugin.getInstance().getInstanceManager()
                .getInstanceData(world.getName());
        
        return data != null ? data.getTemplate() : null;
    }
    
    @Override
    public @NotNull Class<? extends String> getReturnType() {
        return String.class;
    }
    
    @Override
    protected @NotNull String getPropertyName() {
        return "instance template";
    }
}