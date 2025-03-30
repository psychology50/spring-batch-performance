package com.test.batch.reader;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.batch.common.item.QuerydslNoOffsetPagingItemReader;
import com.test.batch.common.item.expression.Expression;
import com.test.batch.common.item.options.QuerydslNoOffsetNumberOptions;
import com.test.batch.common.item.options.QuerydslNoOffsetOptions;
import com.test.batch.domain.QDeviceToken;
import com.test.batch.domain.QUser;
import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.repository.DeviceTokenRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveDeviceTokenReader {
    private final DeviceTokenRepository deviceTokenRepository;
    private final JPAQueryFactory queryFactory;

    private final EntityManagerFactory emf;

    private final QUser user = QUser.user;
    private final QDeviceToken deviceToken = QDeviceToken.deviceToken;

    @Bean
    @StepScope
    public RepositoryItemReader<DeviceTokenOwner> execute() {
        return new RepositoryItemReaderBuilder<DeviceTokenOwner>()
                .name("execute")
                .repository(deviceTokenRepository)
                .methodName("findActivatedDeviceTokenOwners")
                .pageSize(1000)
                .sorts(new HashMap<>() {{
                    put("id", Sort.Direction.ASC);
                }})
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemTestReader<DeviceTokenOwner> executeTestVersion() {
        RepositoryItemTestReader<DeviceTokenOwner> reader = new RepositoryItemTestReader<>();
        reader.setRepository(deviceTokenRepository);
        reader.setMethodName("findActivatedDeviceTokenOwners");
        reader.setPageSize(1000);
        reader.setSort(new HashMap<>() {{
            put("id", Sort.Direction.ASC);
        }});
        return reader;
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<DeviceTokenOwner> jdbcPagingItemReader(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSelectClause("SELECT u.id, u.name, dt.token, dt.id AS deviceTokenId");
        factoryBean.setFromClause("FROM device_token dt LEFT JOIN user u ON dt.user_id = u.id");
        factoryBean.setWhereClause("WHERE dt.activated = true AND u.account_book_notify = true");
        factoryBean.setSortKey("u.id");

        try {
            return new JdbcPagingItemReaderBuilder<DeviceTokenOwner>()
                    .name("jdbcPagingItemReader")
                    .dataSource(dataSource)
                    .fetchSize(100)
                    .rowMapper((rs, rowNum) -> new DeviceTokenOwner(
                            rs.getLong("id"),
                            rs.getLong("deviceTokenId"),
                            rs.getString("name"),
                            rs.getString("token")
                    ))
                    .queryProvider(factoryBean.getObject())
                    .pageSize(100)
                    .build();
        } catch (Exception e) {
            log.error("Error creating jdbcPagingItemReader", e);
            return null;
        }
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DeviceTokenOwner> jpaPagingItemReader(EntityManagerFactory em) {
        return new JpaPagingItemReaderBuilder<DeviceTokenOwner>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(em)
                .queryString("SELECT new com.test.batch.dto.DeviceTokenOwner(u.id, u.name, dt.token) " +
                        "FROM DeviceToken dt LEFT JOIN User u " +
                        "ON dt.user.id = u.id WHERE dt.activated = true AND u.notifySetting.accountBookNotify = true " +
                        "ORDER BY u.id ASC")
//                .queryProvider(new JpaQueryDslProvider<>(
//                        queryFactory
//                                .select(
//                                        Projections.constructor(
//                                                DeviceTokenOwner.class,
//                                                user.id,
//                                                user.name,
//                                                deviceToken.token
//                                        )
//                                )
//                                .from(deviceToken)
//                                .leftJoin(user).on(deviceToken.user.id.eq(user.id))
//                                .where(deviceToken.activated.isTrue().and(user.notifySetting.accountBookNotify.isTrue()))
//                                .orderBy(user.id.asc())
//                ))
                .pageSize(3000)
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<DeviceTokenOwner> jdbcCursorItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<DeviceTokenOwner>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("SELECT u.id, u.name, dt.token FROM device_token dt " +
                        "LEFT JOIN user u ON dt.user_id = u.id " +
                        "WHERE dt.activated = true AND u.account_book_notify = true " +
                        "ORDER BY u.id ASC")
//                .rowMapper(new BeanPropertyRowMapper<>(DeviceTokenOwner.class))
//                .beanRowMapper(DeviceTokenOwner.class)
                .rowMapper((rs, rowNum) -> new DeviceTokenOwner(
                        rs.getLong("id"),
                        rs.getLong("deviceTokenId"),
                        rs.getString("name"),
                        rs.getString("token")
                ))
                .fetchSize(1000)
                .build();
    }

    @Bean
    @StepScope
    public QuerydslNoOffsetPagingItemReader<DeviceTokenOwner> querydslNoOffsetPagingItemReader() {
        QuerydslNoOffsetOptions<DeviceTokenOwner> options = new QuerydslNoOffsetNumberOptions<>(deviceToken.id, Expression.ASC, "deviceTokenId");
        options.setIdSelectQuery(queryFactory.select(createConstructorExpression()).from(deviceToken));

        return new QuerydslNoOffsetPagingItemReader<>(emf, 1000, options, queryFactory -> queryFactory
                .select(createConstructorExpression())
                .from(deviceToken)
                .innerJoin(user).on(deviceToken.user.id.eq(user.id))
                .where(deviceToken.activated.isTrue().and(user.notifySetting.accountBookNotify.isTrue()))
        );
    }

    private ConstructorExpression<DeviceTokenOwner> createConstructorExpression() {
        return Projections.constructor(
                DeviceTokenOwner.class,
                user.id,
                deviceToken.id,
                user.name,
                deviceToken.token
        );
    }
}
