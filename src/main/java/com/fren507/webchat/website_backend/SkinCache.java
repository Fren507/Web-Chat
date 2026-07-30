package com.fren507.webchat.website_backend;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SkinCache {
    private static final long EXPIRATION_TIME = TimeUnit.HOURS.toMillis(2); // Cache for 2 hours
    // Cache holding UUID -> JSON String, capped at 100 entries to prevent memory leaks
    private final Map<UUID, CacheEntry> cache = new LinkedHashMap<UUID, CacheEntry>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, CacheEntry> eldest) {
            return size() > 100; // Max 100 cached profiles
        }
    };

    public synchronized String get(UUID uuid) {
        CacheEntry entry = cache.get(uuid);
        if (entry != null && !entry.isExpired()) {
            return entry.data;
        }
        cache.remove(uuid);
        return null;
    }

    public synchronized void put(UUID uuid, String data) {
        cache.put(uuid, new CacheEntry(data, System.currentTimeMillis() + EXPIRATION_TIME));
    }

    private static class CacheEntry {
        String data;
        long expiresAt;

        CacheEntry(String data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}