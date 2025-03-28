package com.banalytics.box.module.usecase;

import com.banalytics.box.module.BoxEngine;
import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Getter
public abstract class AbstractUseCase<CONFIGURATION> implements UseCase<CONFIGURATION> {
    public final Class<CONFIGURATION> configClass = getType(0);

    public final CONFIGURATION configuration;

    protected final BoxEngine engine;

    public AbstractUseCase(BoxEngine engine) {
        this.engine = engine;

        try {
            configuration = configClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Class<T> getType(int index) {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof Class) {
            while (!(type instanceof ParameterizedType)) {
                type = ((Class) type).getGenericSuperclass();
            }
        }


        type = ((ParameterizedType) type).getActualTypeArguments()[index];
        if (type instanceof Class) {
            return (Class<T>) type;
        } else {
            return null;
        }
    }
}
