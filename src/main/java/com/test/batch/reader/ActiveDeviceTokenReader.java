package com.test.batch.reader;

import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.repository.DeviceTokenRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveDeviceTokenReader {
    private final DeviceTokenRepository deviceTokenRepository;

    @Bean
    public RepositoryItemReader<DeviceTokenOwner> execute() {
        return new RepositoryItemReaderBuilder<DeviceTokenOwner>()
                .name("execute")
                .repository(deviceTokenRepository)
                .methodName("findActivatedDeviceTokenOwners")
                .pageSize(100)
                .sorts(new HashMap<>() {{
                    put("id", Sort.Direction.ASC);
                }})
                .build();
    }

    @Bean
    public JdbcPagingItemReader<DeviceTokenOwner> jdbcPagingItemReader(DataSource dataSource) {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSelectClause("SELECT u.id, u.name, dt.token");
        factoryBean.setFromClause("FROM device_token dt LEFT JOIN user u ON dt.user_id = u.id");
        factoryBean.setWhereClause("WHERE dt.activated = true AND u.account_book_notify = true");
        factoryBean.setSortKey("u.id");

        try {
            return new JdbcPagingItemReaderBuilder<DeviceTokenOwner>()
                    .name("jdbcPagingItemReader")
                    .dataSource(dataSource)
                    .fetchSize(100)
                    .rowMapper(new BeanPropertyRowMapper<>(DeviceTokenOwner.class))
                    .queryProvider(factoryBean.getObject())
                    .pageSize(100)
                    .build();
        } catch (Exception e) {
            log.error("Error creating jdbcPagingItemReader", e);
            return null;
        }
    }

    @Bean
    public JpaPagingItemReader<DeviceTokenOwner> jpaPagingItemReader(EntityManagerFactory em) {
        return new JpaPagingItemReaderBuilder<DeviceTokenOwner>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(em)
                .queryString("SELECT new com.test.batch.dto.DeviceTokenOwner(u.id, u.name, dt.token) " +
                        "FROM DeviceToken dt LEFT JOIN User u " +
                        "ON dt.user.id = u.id WHERE dt.activated = true AND u.accountBookNotify = true" +
                        "ORDER BY u.id AXC")
                .pageSize(100)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<DeviceTokenOwner> jdbcCursorItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<DeviceTokenOwner>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("SELECT u.id, u.name, dt.token FROM device_token dt " +
                        "LEFT JOIN user u ON dt.user_id = u.id " +
                        "WHERE dt.activated = true AND u.account_book_notify = true " +
                        "ORDER BY u.id ASC")
                .rowMapper(new BeanPropertyRowMapper<>(DeviceTokenOwner.class))
                .fetchSize(100)
                .build();
    }
}
