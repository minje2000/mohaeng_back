package org.poolpool.mohaeng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan // @ConfigurationProperties가 붙은 클래스 자동 스캔, 애플리케이션 시작 시 UploadProperties 자동 등록,
								// 별도 Config 클래스 불필요
public class MohaengApplication {

	public static void main(String[] args) {
		SpringApplication.run(MohaengApplication.class, args);
	}

}
