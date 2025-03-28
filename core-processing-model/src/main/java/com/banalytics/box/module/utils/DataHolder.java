package com.banalytics.box.module.utils;

import com.banalytics.box.api.integration.AbstractMessage;
import com.banalytics.box.api.integration.utils.CommonUtils;
import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.module.events.ClientEvent;
import com.banalytics.box.module.usecase.AbstractUseCase;
import com.banalytics.box.module.usecase.UseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.*;

@Slf4j
public final class DataHolder {
    private static final Map<String, Class<? extends AbstractEvent>> EVENT_TYPE_CODE_CLASS_MAP = new HashMap<>();
    private static final Set<Class<? extends AbstractUseCase>> USE_CASES = new HashSet<>();
    private static final Set<Class<? extends AbstractEvent>> CLIENT_EVENT_TYPES = new HashSet<>();


    public static void registerClass(Class<? extends AbstractEvent> eventTypeClass) {
        try {
            Constructor<? extends AbstractEvent> constructor = eventTypeClass.getDeclaredConstructor();
            AbstractEvent ae = constructor.newInstance();
            EVENT_TYPE_CODE_CLASS_MAP.put(ae.getType(), eventTypeClass);
            if(ae instanceof ClientEvent) {
                CLIENT_EVENT_TYPES.add(eventTypeClass);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public static Collection<Class<? extends AbstractEvent>> eventTypeClasses() {
        return EVENT_TYPE_CODE_CLASS_MAP.values();
    }

    public static Collection<Class<? extends AbstractEvent>> clientEventTypeClasses() {
        return CLIENT_EVENT_TYPES;
    }

    public static <T extends AbstractMessage> T constructEventOrMessageFrom(String jsonMsg) throws Exception {
        AbstractMessage request = AbstractMessage.from(jsonMsg);
        if (request == null) {
            request = DataHolder.constructEventFrom(jsonMsg);
            if(request == null) {
                log.error("Message not supporting: {}", jsonMsg);
            }
        }
        return (T) request;
    }
    public static <T extends AbstractEvent> T constructEventFrom(String json) throws Exception {
        try {
            ObjectMapper objectMapper = CommonUtils.DEFAULT_OBJECT_MAPPER;
            JsonNode tree = objectMapper.readTree(json);
            String type = tree.get("type").asText();
            Class<? extends AbstractEvent> clazz = EVENT_TYPE_CODE_CLASS_MAP.get(type);
            return (T) objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static void registerUseCase(Class<? extends AbstractUseCase> useCase) {
        USE_CASES.add(useCase);
    }

    public static Set<Class<? extends AbstractUseCase>> useCases() {
        return USE_CASES;
    }
}
