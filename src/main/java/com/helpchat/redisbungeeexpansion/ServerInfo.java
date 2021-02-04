package com.helpchat.redisbungeeexpansion;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class ServerInfo {

    @Getter
    private final String name;

    @Getter
    @Setter
    private int playerCount;
    @Getter
    @Setter
    private boolean online;

    @Getter
    @Setter
    private boolean tracked = false;

    public ServerInfo(String name) {
        this.name = name;
    }

    public ServerInfo(String name, boolean tracked) {
        this.name = name;
        this.tracked = tracked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerInfo)) return false;
        ServerInfo that = (ServerInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
