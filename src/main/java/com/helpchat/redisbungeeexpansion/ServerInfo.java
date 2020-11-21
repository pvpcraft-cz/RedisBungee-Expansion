package com.helpchat.redisbungeeexpansion;

import lombok.Getter;
import lombok.Setter;

public class ServerInfo {

    @Getter
    private final String server;

    @Getter
    @Setter
    private int playerCount;
    @Getter
    @Setter
    private boolean online;

    public ServerInfo(String server) {
        this.server = server;
    }
}
