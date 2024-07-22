package com.test.batch;

import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.reader.ActiveDeviceTokenReader;
import com.test.batch.repository.UserRepository;
import com.test.batch.supporter.DataCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StopWatch;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;

@Slf4j
@SpringBootTest
@SpringBatchTest
class BatchReaderTest {
    @Autowired
    private ActiveDeviceTokenReader reader;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    private DataCreator dataCreator;

    @BeforeEach
    void setUp() {
        dataCreator = new DataCreator(jdbcTemplate);
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000, 100000})
    @DisplayName("JdbcCursorItemReader 테스트")
    void testJdbcCursorItemReader(int dataSize) throws Exception {
        insertData(dataSize);

        JdbcCursorItemReader<DeviceTokenOwner> itemReader = reader.jdbcCursorItemReader(dataSource);
        itemReader.afterPropertiesSet();

        itemReader.open(new ExecutionContext());

        testItemReader(itemReader, "JdbcCursorItemReader");
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000, 100000})
    @DisplayName("JdbcPagingItemReader 테스트")
    void testJdbcPagingItemReader(int dataSize) throws Exception {
        insertData(dataSize);

        JdbcCursorItemReader<DeviceTokenOwner> itemReader = reader.jdbcCursorItemReader(dataSource);
        itemReader.afterPropertiesSet();

        itemReader.open(new ExecutionContext());

        testItemReader(itemReader, "JdbcPagingItemReader");
    }

    @Test
    @Disabled
    @DisplayName("RepositoryItemReader 테스트")
    void testRepositoryItemReader() {
        RepositoryItemReader<DeviceTokenOwner> itemReader = reader.execute();

        testItemReader(itemReader, "RepositoryItemReader");
    }

    private void testItemReader(ItemReader<DeviceTokenOwner> itemReader, String readerName) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int count = 0;
        DeviceTokenOwner item;
        try {
            while ((item = itemReader.read()) != null) {
                count++;
            }
        } catch (Exception e) {
            fail("Error reading items", e);
        }

        stopWatch.stop();
        log.info("{} processed {} items in {} ms", readerName, count, stopWatch.getTotalTimeMillis());
    }

    @AfterEach
    void tearDown() {
        dataCreator.bulkDeleteDeviceToken();
        dataCreator.bulkDeleteUser();
    }

    private void insertData(int dataSize) {
        int userCount = dataSize, deviceTokenCount = userCount * 10; // 사용자 수, 디바이스 토큰 수(사용자수 10배)
        dataCreator.bulkInsertUser(userCount);
        List<Long> userIds = userRepository.findAllIds();
        dataCreator.bulkInsertDeviceToken(deviceTokenCount, userIds); // 사용자에게 랜덤으로 디바이스 토큰을 부여 (중복 허용)
        dataCreator = new DataCreator(jdbcTemplate);
    }
}
