package com.eisiadev.instanceapi.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InstanceData {
    
    private final String worldName;
    private final String template;
    private final UUID owner;
    private final long createdAt;
    private final Map<String, Object> metadata;
    
    public InstanceData(String worldName, String template, UUID owner) {
        this.worldName = worldName;
        this.template = template;
        this.owner = owner;
        this.createdAt = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }
    
    // Getters
    public String getWorldName() {
        return worldName;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    // Serialization
    public void save(File file) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        }
    }
    
    public static InstanceData load(File file) throws IOException {
        Gson gson = new Gson();
        
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, InstanceData.class);
        }
    }
}