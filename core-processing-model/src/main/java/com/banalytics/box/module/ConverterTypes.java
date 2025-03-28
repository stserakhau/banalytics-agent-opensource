package com.banalytics.box.module;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ConverterTypes {
    TypeReference<List<String>> TYPE_LIST_STRING = new TypeReference<>() {
    };
    TypeReference<Set<String>> TYPE_SET_STRING = new TypeReference<>() {
    };
    TypeReference<Set<UUID>> TYPE_SET_UUID = new TypeReference<>() {
    };

    TypeReference<Map<String, Object>> TYPE_NODE_CONFIGURATION = new TypeReference<>() {
    };

    TypeReference<Map<String, Map<String, String>>> TYPE_MAP_STR_MAP_STR_STR = new TypeReference<>() {
    };
}
