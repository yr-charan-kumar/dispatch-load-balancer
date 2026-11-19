package com.freightfox.dispatch.domain;

/** Delivery priority. Lower rank = served first. */
public enum Priority {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
