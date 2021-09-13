package com.futlabs.letsencrypt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(args = {
		"--domain=jobprojectbidding.com",
		"--dnsProvider=alibaba",
		"--accessKey=<accessKey>",
		"--accessSecret=<accessSecret>"
})
class WildcardApplicationTests {

	@Test
	void testApplication() {
		// Just run this test case, the ApplicationRunner will automatically run ...
	}

}
