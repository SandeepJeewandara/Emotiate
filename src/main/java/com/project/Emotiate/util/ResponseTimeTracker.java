package com.project.Emotiate.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResponseTimeTracker {

    // Tracks dispatch timestamp per session
    private final ConcurrentHashMap<String, Long> pendingTimestamps = new ConcurrentHashMap<>();


    /** Called when a guest message is dispatched to the agent. */
    public void recordMessageDispatched(String sessionId) {
        pendingTimestamps.put(sessionId, System.currentTimeMillis());
    }


    /** Returns elapsed ms since dispatch and removes the entry. Returns null if no entry exists. */
    public Long computeAndClear(String sessionId) {
        Long startTime = pendingTimestamps.remove(sessionId);
        return startTime == null ? null : System.currentTimeMillis() - startTime;
    }
}