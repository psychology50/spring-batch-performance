package com.test.batch.common.converter;

import com.test.batch.common.type.Announcement;
import jakarta.persistence.Converter;

@Converter
public class AnnouncementConverter extends AbstractLegacyEnumAttributeConverter<Announcement> {
    private static final String ENUM_NAME = "공지 타입";

    public AnnouncementConverter() {
        super(Announcement.class, false, ENUM_NAME);
    }
}
