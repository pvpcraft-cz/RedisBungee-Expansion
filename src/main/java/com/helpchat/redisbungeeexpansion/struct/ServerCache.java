package com.helpchat.redisbungeeexpansion.struct;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServerCache {

    private final Set<ServerInfo> values = new HashSet<>();

    public void clear() {
        values.clear();
    }

    public void remove(String name) {
        values.remove(new ServerInfo(name));
    }

    public ServerInfo get(String name) {
        return values.stream()
                .filter(i -> i.getName().equals(name))
                .findAny().orElse(null);
    }

    public ServerInfo getOrCreate(String name) {
        ServerInfo info = get(name);
        return info == null ? create(name) : info;
    }

    public ServerInfo add(ServerInfo info) {
        values.add(info);
        return info;
    }

    public ServerInfo create(String name) {
        ServerInfo info = new ServerInfo(name);
        values.add(info);
        return info;
    }

    public boolean has(String name) {
        return values.stream().anyMatch(i -> i.getName().equals(name));
    }

    public Set<ServerInfo> getValues() {
        return Collections.unmodifiableSet(values);
    }
}
