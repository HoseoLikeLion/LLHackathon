package com.hackathon.skinroutine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local") // H2 인메모리 — 외부 서비스 없이 컨텍스트 부팅 확인
class SkinRoutineApplicationTests {

	@Test
	void contextLoads() {
	}

}
