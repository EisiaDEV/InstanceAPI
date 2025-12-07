package com.eisiadev.instanceapi.event;

import com.eisiadev.instanceapi.data.InstanceData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InstanceWorldDeletedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String worldName;
    private final InstanceData instanceData;
    
    public InstanceWorldDeletedEvent(@NotNull String worldName, @NotNull InstanceData instanceData) {
        this.worldName = worldName;
        this.instanceData = instanceData;
    }
    
    @NotNull
    public String getWorldName() {
        return worldName;
    }
    
    @NotNull
    public InstanceData getInstanceData() {
        return instanceData;
    }
    
    @NotNull
    public String getTemplate() {
        return instanceData.getTemplate();
    }
    
    @NotNull
    public UUID getOwner() {
        return instanceData.getOwner();
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}