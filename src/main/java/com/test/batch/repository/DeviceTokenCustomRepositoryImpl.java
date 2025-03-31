package com.test.batch.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.batch.domain.QDeviceToken;
import com.test.batch.domain.QUser;
import com.test.batch.dto.DeviceTokenOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceTokenCustomRepositoryImpl implements DeviceTokenCustomRepository {
    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;
    private final QDeviceToken deviceToken = QDeviceToken.deviceToken;

    public static <T> Slice<T> toSlice(List<T> contents, Pageable pageable) {
        boolean hasNext = isContentSizeGreaterThanPageSize(contents, pageable);
        return new SliceImpl<>(hasNext ? subListLastContent(contents, pageable) : contents, pageable, hasNext);
    }

    private static <T> boolean isContentSizeGreaterThanPageSize(List<T> content, Pageable pageable) {
        return pageable.isPaged() && content.size() > pageable.getPageSize();
    }

    private static <T> List<T> subListLastContent(List<T> content, Pageable pageable) {
        return content.subList(0, pageable.getPageSize());
    }

    @Override
    public Slice<DeviceTokenOwner> findActivatedDeviceTokenOwners(Pageable pageable) {
        List<DeviceTokenOwner> content = queryFactory
                .select(
                        Projections.constructor(
                                DeviceTokenOwner.class,
                                user.id,
                                deviceToken.id,
                                user.name,
                                deviceToken.token
                        )
                )
                .from(deviceToken)
                .leftJoin(user).on(deviceToken.user.id.eq(user.id))
                .where(deviceToken.activated.isTrue().and(user.notifySetting.accountBookNotify.isTrue()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(user.id.asc())
                .fetch();

        return toSlice(content, pageable);
    }
}
