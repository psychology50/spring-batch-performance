package com.test.batch;

import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.reader.ActiveDeviceTokenReader;
import com.test.batch.repository.DeviceTokenRepository;
import com.test.batch.repository.UserRepository;
import com.test.batch.supporter.DataCreator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

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
	private DeviceTokenRepository deviceTokenRepository;

	private DataCreator dataCreator;

	@BeforeEach
	void setUp() {
		dataCreator = new DataCreator(jdbcTemplate);
	}

	@Test
	@Transactional
	void testReaderPerformance() {
		int userCount = 10, deviceTokenCount = userCount * 10;
		dataCreator.bulkInsertUser(userCount);
		List<Long> userIds = userRepository.findAllIds();
		dataCreator.bulkInsertDeviceToken(deviceTokenCount, userIds);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		RepositoryItemReader<DeviceTokenOwner> itemReader = reader.execute();
		int count = 0;
		DeviceTokenOwner item;
		try {
			while ((item = itemReader.read()) != null) {
				log.info("Read item: {}", item);
				count++;
			}
		} catch (Exception e) {
			fail("Error reading items", e);
		}

		stopWatch.stop();
		log.info("Reader processed {} items in {} ms", count, stopWatch.getTotalTimeMillis());
	}
}
