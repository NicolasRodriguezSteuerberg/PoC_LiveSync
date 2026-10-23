package com.nsteuerberg.sse;

public record Tablero (
    int counter,
    String message,
    long timestamp
){
}
