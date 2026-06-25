package com.ceos.voteservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.jwt.secret=test-secret-key-for-context-load")
class VoteServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
