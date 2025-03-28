package com.banalytics.box.module.events.jpa;

import com.banalytics.box.api.integration.AbstractMessage;
import com.banalytics.box.jpa.types.AbstractJsonUserType;
import com.banalytics.box.module.utils.DataHolder;
import lombok.extern.slf4j.Slf4j;

import java.sql.Types;

@Slf4j
public class AbstractMessageJsonUserType extends AbstractJsonUserType {
    @Override
    protected Object createObject(String jsonMsg) throws Exception {
        AbstractMessage request = DataHolder.constructEventOrMessageFrom(jsonMsg);
        if (request == null) {
            request = new AbstractMessage("NOT_SUPPORTED") {
            };
            log.warn("Event type not supported:\n{}", jsonMsg);
        }

        return request;
    }

    @Override
    protected int getDataType() {
        return Types.CLOB;
    }

    @Override
    public Class returnedClass() {
        return AbstractMessage.class;
    }
}
