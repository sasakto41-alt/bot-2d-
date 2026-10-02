package com.example.deepdigger.models;

import java.util.UUID;

/**
 * Pending invitation between two players. Held in-memory only.
 */
public class Invite {
    private final String id;
    private final UUID fromUuid;
    private final UUID toUuid;
    private final String fromName;
    private final String toName;
    private final long createdAt;

    public Invite(String id, UUID fromUuid, UUID toUuid, String fromName, String toName, long createdAt) {
        this.id = id;
        this.fromUuid = fromUuid;
        this.toUuid = toUuid;
        this.fromName = fromName;
        this.toName = toName;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public UUID getFromUuid() { return fromUuid; }
    public UUID getToUuid() { return toUuid; }
    public String getFromName() { return fromName; }
    public String getToName() { return toName; }
    public long getCreatedAt() { return createdAt; }
}
