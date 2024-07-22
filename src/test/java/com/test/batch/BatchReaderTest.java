package com.test.batch;

import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.reader.ActiveDeviceTokenReader;
import com.test.batch.repository.UserRepository;
import com.test.batch.supporter.DataCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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

    @Test
    @DisplayName("JdbcCursorItemReader 테스트")
    void testJdbcCursorItemReader() throws Exception {
        int userCount = 100, deviceTokenCount = userCount * 10;
        dataCreator.bulkInsertUser(userCount);
        List<Long> userIds = userRepository.findAllIds();
        dataCreator.bulkInsertDeviceToken(deviceTokenCount, userIds);

        JdbcCursorItemReader<DeviceTokenOwner> itemReader = reader.jdbcCursorItemReader(dataSource);
        itemReader.afterPropertiesSet();

        itemReader.open(new ExecutionContext());

        testItemReader(itemReader, "JdbcCursorItemReader");
    }

    @Test
    @Disabled
    @Transactional
    @DisplayName("RepositoryItemReader 테스트")
    void testRepositoryItemReader() {
        int userCount = 100, deviceTokenCount = userCount * 10;
        dataCreator.bulkInsertUser(userCount);
        List<Long> userIds = userRepository.findAllIds();
        dataCreator.bulkInsertDeviceToken(deviceTokenCount, userIds);

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
                log.info("{}", item);
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
}
