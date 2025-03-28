package com.banalytics.box.module.standard;

import com.banalytics.box.module.events.AbstractEvent;

import java.util.Set;
import java.util.UUID;

public interface EventConsumer {
    void consume(Recipient target, AbstractEvent event);

    UUID getUuid();

    String getTitle();

    String getSelfClassName();

    default Set<String> accountNames(Set<String> accountIds) {
        return Set.of();
    }

    public static record Recipient(Set<String> accounts) {
        public boolean isAllowed(String account) {
            if (accounts == null) {
                return true;
            }
            return accounts != null && accounts.contains(account);
        }

        public boolean isEmpty() {
            return accounts == null || accounts.isEmpty();
        }
    }
}
