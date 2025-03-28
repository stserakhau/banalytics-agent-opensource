package com.banalytics.box.module.events.model;

import com.banalytics.box.module.events.AbstractEvent;
import com.banalytics.box.filter.FilterNode;
import com.banalytics.box.filter.FilterTreeBuilder;
import com.cronutils.model.CompositeCron;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.logging.log4j.util.Strings.isNotEmpty;

/**
 * Condition:
 * <p>
 * eventSourceThingUuid in (eventSourceThingsUUIDs)
 * and
 * eventSourceClassName in (eventSourcesClassNames)
 * and
 * eventTypeClassName in (eventTypesClassNames)
 * and
 * one from cronExpressions returns true for the time
 * <p>
 * Trigger is fired
 * <p>
 * Note: if property is empty it not affect the condition result.
 * Default condition is fdlse;
 */
@Slf4j
public class Trigger {
    private static final String PARAM_NATIVE_RULE_CONFIGURATION = "nativeRule";

    private static final ZoneId LOCAL_TIME_ZONE;

    static {
        String localTz = System.getProperty("user.timezone");
        if (isEmpty(localTz)) {
            LOCAL_TIME_ZONE = ZoneId.systemDefault();
        } else {
            LOCAL_TIME_ZONE = ZoneId.of(localTz);
        }
    }

    private static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
    private static final CronParser CRON_PARSER = new CronParser(CRON_DEFINITION);

    private final Set<UUID> eventSourceNodeUUIDs = new HashSet<>();

    private final Set<String> eventSourcesClassNames = new HashSet<>();

    private final List<EventTypeConfig> eventTypesConfigs = new ArrayList<>();

    private final List<String> cronExpressions = new ArrayList<>();

    private transient ExecutionTime executionTime;

    public void reset() {
        executionTime = null;
    }

    public boolean triggered(AbstractEvent event) {
        boolean isRuleDefined = false;
        isRuleDefined |= !eventSourceNodeUUIDs.isEmpty();
        if (!eventSourceNodeUUIDs.isEmpty()) {
            boolean valid = eventSourceNodeUUIDs.contains(event.getNodeUuid());
            if (!valid) {
                return false;
            }
        }

        isRuleDefined |= !eventSourcesClassNames.isEmpty();
        if (!eventSourcesClassNames.isEmpty()) {
            boolean valid = eventSourcesClassNames.contains(event.getNodeClassName());
            if (!valid) {
                return false;
            }
        }

        isRuleDefined |= !eventTypesConfigs.isEmpty();
        if (!eventTypesConfigs.isEmpty()) {
            boolean valid = false;
            for (EventTypeConfig etc : eventTypesConfigs) {
                try {
                    if (etc.check(event)) {
                        valid = true;
                        break;
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (!valid) {
                return false;
            }
        }

        isRuleDefined |= !cronExpressions.isEmpty();
        if (!cronExpressions.isEmpty()) {
            if (executionTime == null) {
                List<Cron> crons = new ArrayList<>(cronExpressions.size());
                for (String cronExpression : cronExpressions) {
                    Cron cron = CRON_PARSER.parse(cronExpression);
                    crons.add(cron);
                }
                CompositeCron cron = new CompositeCron(crons);
                executionTime = ExecutionTime.forCron(cron);
            }

            ZonedDateTime now = ZonedDateTime.now(LOCAL_TIME_ZONE);
            boolean valid = executionTime.isMatch(now);
            if (!valid) {
                return false;
            }
        }

        return isRuleDefined;
    }

    public Set<UUID> getEventSourceNodeUUIDs() {
        return eventSourceNodeUUIDs;
    }

    public Set<String> getEventSourcesClassNames() {
        return eventSourcesClassNames;
    }

    public List<EventTypeConfig> getEventTypesConfigs() {
        return eventTypesConfigs;
    }

    public List<String> getCronExpressions() {
        return cronExpressions;
    }

    public static class EventTypeConfig {
        private String className;
        private Map<String, Object> configuration;

        private FilterNode filterNode;

        public EventTypeConfig() {
        }

        public EventTypeConfig(String className, Map<String, Object> configuration) {
            this.className = className;
            this.configuration = configuration;
            init();
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
            init();
        }

        private void init() {
            if (configuration == null || configuration.isEmpty()) {
                filterNode = null;
            } else {
                String nativeRule = (String) configuration.remove(PARAM_NATIVE_RULE_CONFIGURATION); // remove nativeRule and put below

                StringBuilder expression = new StringBuilder(100);
                for (Map.Entry<String, Object> entry : configuration.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    if (val == null || (val instanceof String s && s.isEmpty())) {
                        continue;
                    }
                    boolean isCollection = Collection.class.isAssignableFrom(val.getClass())
                            || (val instanceof String && ((String) val).startsWith("["));

                    if (isCollection && ("[]".equals(val) || (val instanceof Collection c && c.size() == 0))) {
                        continue;
                    }

                    if (expression.length() > 0) {
                        expression.append(" and ");
                    }

                    expression
                            .append('(')
                            .append(key).append(' ')
                            .append(isCollection ? " in " : " eq ")
                            .append(val)
                            .append(')');
                }

                configuration.put(PARAM_NATIVE_RULE_CONFIGURATION, nativeRule);                     // return nativeRule

                if (isNotEmpty(nativeRule)) {
                    boolean emptyExpr = expression.isEmpty();
                    if (!emptyExpr) {
                        expression.append(" and (");
                    }
                    expression.append(nativeRule);
                    if (!emptyExpr) {
                        expression.append(")");
                    }
                }

                try {
                    filterNode = expression.isEmpty() ? null : FilterTreeBuilder.parse(expression.toString(), Class.forName(className));
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        public boolean check(AbstractEvent event) throws Exception {
            if (!className.equals(event.getClass().getName())) {
                return false;
            }

            if (filterNode != null) {
                return filterNode.applyFilter(event);
            }

            return true;
        }
    }
}
