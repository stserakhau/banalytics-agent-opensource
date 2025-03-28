package com.banalytics.box.module.usecase;

import com.banalytics.box.module.BoxEngine;

public interface UseCase<CONFIG> {
    void create() throws Exception;

    default String groupCode() {
        return "GENERAL";
    }

    static AbstractUseCase<?> blankOf(Class<? extends AbstractUseCase> useCaseClazz, BoxEngine engine) throws Exception {
        return useCaseClazz.getDeclaredConstructor(new Class[]{BoxEngine.class}).newInstance(engine);
    }
}
