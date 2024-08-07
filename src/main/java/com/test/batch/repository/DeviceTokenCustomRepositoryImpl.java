package com.test.batch.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.batch.domain.QDeviceToken;
import com.test.batch.domain.QUser;
import com.test.batch.dto.DeviceTokenOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceTokenCustomRepositoryImpl implements DeviceTokenCustomRepository {
    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;
    private final QDeviceToken deviceToken = QDeviceToken.deviceToken;

    @Override
    public Page<DeviceTokenOwner> findActivatedDeviceTokenOwners(Pageable pageable) {
        List<DeviceTokenOwner> content = queryFactory
                .select(
                        Projections.constructor(
                                DeviceTokenOwner.class,
                                user.id,
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

        JPAQuery<Long> count = queryFactory
                .select(deviceToken.id.count())
                .from(deviceToken)
                .leftJoin(user).on(deviceToken.user.id.eq(user.id))
                .where(deviceToken.activated.isTrue().and(user.notifySetting.accountBookNotify.isTrue()));

        return PageableExecutionUtils.getPage(content, pageable, () -> count.fetch().size());
    }
}
