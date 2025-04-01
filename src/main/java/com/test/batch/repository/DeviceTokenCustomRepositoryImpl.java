package com.test.batch.repository;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceTokenCustomRepositoryImpl implements DeviceTokenCustomRepository {
    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;
    private final QDeviceToken deviceToken = QDeviceToken.deviceToken;
    private final AtomicLong lastProcessedId = new AtomicLong(0);

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
        ConstructorExpression<DeviceTokenOwner> constructorExpression = Projections.constructor(
                DeviceTokenOwner.class,
                user.id,
                deviceToken.id,
                user.name,
                deviceToken.token
        );

        BooleanExpression whereCondition = deviceToken.activated.isTrue()
                .and(user.notifySetting.accountBookNotify.isTrue());

        if (lastProcessedId.get() > 0) {
            log.info("lastProcessedId: {}", lastProcessedId.get());
            whereCondition = whereCondition.and(deviceToken.id.gt(lastProcessedId.get()));
        }

        List<DeviceTokenOwner> content = queryFactory
                .select(constructorExpression)
                .from(deviceToken)
                .innerJoin(user).on(deviceToken.user.id.eq(user.id))
                .where(whereCondition)
                .limit(pageable.getPageSize())
                .orderBy(deviceToken.id.asc())
                .fetch();

        if (!content.isEmpty()) {
            lastProcessedId.set(content.get(content.size() - 1).deviceTokenId());
            log.info("updated lastProcessedId: {}", lastProcessedId.get());
        }

        return toSlice(content, pageable);
    }

    public void resetLastProcessedId() {
        log.info("Resetting last processed ID from {} to 0", lastProcessedId);
        lastProcessedId.set(0);
    }
}
