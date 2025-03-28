package com.banalytics.box.module;

import com.banalytics.box.module.events.AbstractEvent;

import java.util.Set;

public interface EventProducer {
    Set<Class<? extends AbstractEvent>> produceEvents();
}
