package com.test.batch.repository;

import com.querydsl.core.Tuple;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
        // 1. join 없이 deviceToken만 조회
        List<Tuple> deviceTokens = queryFactory
                .select(deviceToken.id, deviceToken.token, deviceToken.user.id)
                .from(deviceToken)
                .where(deviceToken.activated.isTrue().and(deviceToken.id.gt(lastProcessedId.get())))
                .limit(pageable.getPageSize())
                .orderBy(deviceToken.id.asc())
                .fetch();

        // 2. deviceToken이 없으면 빈 결과 반환
        if (deviceTokens.isEmpty()) {
            return toSlice(List.of(), pageable);
        }

        // 3. 마지막 조회된 deviceToken의 ID를 업데이트
        lastProcessedId.set(deviceTokens.get(deviceTokens.size() - 1).get(deviceToken.id));
        log.info("updated lastProcessedId: {}", lastProcessedId.get());

        // 4. deviceToken에 대한 user ID 목록 추출
        List<Long> userIds = deviceTokens.stream()
                .map(tuple -> tuple.get(deviceToken.user.id))
                .distinct()
                .collect(Collectors.toList());

        // 5. user ID 목록에 해당하는 user 조회
        Map<Long, String> users = queryFactory
                .select(user.id, user.name)
                .from(user)
                .where(user.id.in(userIds).and(user.notifySetting.accountBookNotify.isTrue()))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(user.id),
                        tuple -> tuple.get(user.name),
                        (existing, replacement) -> existing
                ));

        // 6. deviceTokenOwner 목록 생성
        List<DeviceTokenOwner> result = deviceTokens.stream()
                .filter(token -> users.containsKey(token.get(deviceToken.user.id)))
                .map(token -> new DeviceTokenOwner(
                        token.get(deviceToken.user.id),
                        token.get(deviceToken.id),
                        users.get(token.get(deviceToken.user.id)),
                        token.get(deviceToken.token)
                ))
                .toList();

        return toSlice(result, pageable);
    }

//    @Override
//    public Slice<DeviceTokenOwner> findActivatedDeviceTokenOwners(Pageable pageable) {
//        ConstructorExpression<DeviceTokenOwner> constructorExpression = Projections.constructor(
//                DeviceTokenOwner.class,
//                user.id,
//                deviceToken.id,
//                user.name,
//                deviceToken.token
//        );
//
//        BooleanExpression whereCondition = deviceToken.activated.isTrue()
//                .and(user.notifySetting.accountBookNotify.isTrue());
//
//        if (lastProcessedId.get() > 0) {
//            log.info("lastProcessedId: {}", lastProcessedId.get());
//            whereCondition = whereCondition.and(deviceToken.id.gt(lastProcessedId.get()));
//        }
//
//        List<DeviceTokenOwner> content = queryFactory
//                .select(constructorExpression)
//                .from(deviceToken)
//                .innerJoin(user).on(deviceToken.user.id.eq(user.id))
//                .where(whereCondition)
//                .limit(pageable.getPageSize())
//                .orderBy(deviceToken.id.asc())
//                .fetch();
//
//        if (!content.isEmpty()) {
//            lastProcessedId.set(content.get(content.size() - 1).deviceTokenId());
//            log.info("updated lastProcessedId: {}", lastProcessedId.get());
//        }
//
//        return toSlice(content, pageable);
//    }

    public void resetLastProcessedId() {
        log.info("Resetting last processed ID from {} to 0", lastProcessedId);
        lastProcessedId.set(0);
    }
}
