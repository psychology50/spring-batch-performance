package com.test.batch.common.converter;

import com.test.batch.common.type.NoticeType;
import jakarta.persistence.Converter;

@Converter
public class NoticeTypeConverter extends AbstractLegacyEnumAttributeConverter<NoticeType> {
    private static final String ENUM_NAME = "알림 타입";

    public NoticeTypeConverter() {
        super(NoticeType.class, false, ENUM_NAME);
    }
}
