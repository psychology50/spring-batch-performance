package com.test.batch;

import com.test.batch.dto.DeviceTokenOwner;
import com.test.batch.reader.ActiveDeviceTokenReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
@SpringBatchTest
class BatchApplicationTests {
	@Autowired
	private ActiveDeviceTokenReader reader;

	@Test
	void contextLoads() throws Exception {


		RepositoryItemReader<DeviceTokenOwner> executor = reader.execute();
		executor.afterPropertiesSet();

		DeviceTokenOwner dto = executor.read();
		while (dto != null) {
			log.info("{}", dto);
			dto = executor.read();
		}
		log.info("총 개수: {}", executor.getCurrentItemCount());
	}

}
