package com.nsteuerberg.websocket;

public record Request (
    boolean increment,
    int value
){
}
