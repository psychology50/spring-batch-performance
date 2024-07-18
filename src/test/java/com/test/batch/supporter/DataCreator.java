package com.test.batch.supporter;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DataCreator {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DataCreator(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String INSERT_USER = "INSERT INTO user (account_book_notify, chat_notify, feed_notify, created_at, updated_at, name, username) VALUES (:accountBookNotify, :chatNotify, :feedNotify, NOW(), NOW(), :name, :username);";
    private static final String INSERT_DEVICE_TOKEN = "INSERT INTO device_token (activated, created_at, updated_at, user_id, token) VALUES (:activated, NOW(), NOW(), :userId, :token);";

    public void bulkInsertUser(int count) {
        SqlParameterSource[] params = new SqlParameterSource[count];
        for (int i = 0; i < count; i++) {
            params[i] = new MapSqlParameterSource()
                    .addValue("accountBookNotify", true)
                    .addValue("chatNotify", true)
                    .addValue("feedNotify", true)
                    .addValue("name", "user_" + i)
                    .addValue("username", "username_" + i);
        }

        jdbcTemplate.batchUpdate(INSERT_USER, params);
    }

    public void bulkInsertDeviceToken(int count, List<Long> userIds) {
        SqlParameterSource[] params = new SqlParameterSource[count];
        for (int i = 0; i < count; i++) {
            params[i] = new MapSqlParameterSource()
                    .addValue("activated", true)
                    .addValue("userId", ThreadLocalRandom.current().nextLong(userIds.get(0), userIds.get(userIds.size() - 1) + 1))
                    .addValue("token", "token_" + i);
        }

        jdbcTemplate.batchUpdate(INSERT_DEVICE_TOKEN, params);
    }
}
