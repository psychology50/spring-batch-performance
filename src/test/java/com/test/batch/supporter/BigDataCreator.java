package com.test.batch.supporter;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BigDataCreator {
    private static final String INSERT_USER = "INSERT INTO user (id, name, username, account_book_notify, chat_notify, feed_notify, created_at, updated_at, deleted_at) "
            + "VALUES (NULL, ?, ?, true, true, true, NOW(), NOW(), NULL);";
    private static final String INSERT_DEVICE_TOKEN = "INSERT INTO device_token (id, token, activated, created_at, updated_at, user_id) "
            + "VALUES (NULL, ?, true, NOW(), NOW(), ?);";
    private static final int BATCH_SIZE = 50000;

    private final JdbcTemplate jdbcTemplate;

    public BigDataCreator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertUserData(int dataSize) {
        int i, batchCount = 0;

//        for (i = 0; i < dataSize; ++i) {
//
//            if ((i + 1) % BATCH_SIZE == 0)
//                batchCount = batchInsertUser(batchCount);
//        }
//
//        if ((i + 1) % BATCH_SIZE != 0)
//            batchInsertUser(batchCount);

        for (i = 0; i < dataSize * 10; ++i) {
            if ((i + 1) % BATCH_SIZE == 0)
                batchCount = batchInsertDeviceToken(batchCount);
        }

        if ((i + 1) % BATCH_SIZE != 0)
            batchInsertDeviceToken(batchCount);
    }

    private int batchInsertUser(int batchCount) {
        jdbcTemplate.batchUpdate(INSERT_USER, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, "user_" + i);
                ps.setString(2, UUID.randomUUID().toString());
            }

            @Override
            public int getBatchSize() {
                return BATCH_SIZE;
            }
        });

        return ++batchCount;
    }

    private int batchInsertDeviceToken(int batchCount) {
        jdbcTemplate.batchUpdate(INSERT_DEVICE_TOKEN, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, "token_" + i);
                ps.setLong(2, ThreadLocalRandom.current().nextLong(1, 100100000));
            }

            @Override
            public int getBatchSize() {
                return BATCH_SIZE;
            }
        });

        return ++batchCount;
    }
}
