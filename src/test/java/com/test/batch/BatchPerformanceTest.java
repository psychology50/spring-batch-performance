package com.test.batch;

import com.test.batch.repository.UserRepository;
import com.test.batch.supporter.DataCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StopWatch;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@SpringBatchTest
public class BatchPerformanceTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    private DataCreator dataCreator;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        dataCreator = new DataCreator(jdbcTemplate);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000, 100000})
    void testJobPerformance1(int dataSize) throws Exception {
        insertData(dataSize);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        stopWatch.stop();

        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        log.info("Job completed in {} ms", stopWatch.getTotalTimeMillis());
        log.info("Read count: {}", jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum());
        log.info("Write count: {}", jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum());
    }

    @AfterEach
    void tearDown() {
        dataCreator.bulkDeleteDeviceToken();
        dataCreator.bulkDeleteNotifications();
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
