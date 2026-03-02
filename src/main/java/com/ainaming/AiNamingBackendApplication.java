package com.ainaming;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ainaming.mapper")   // 扫描 Mapper 接口
public class AiNamingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiNamingBackendApplication.class, args);
		System.out.println("✅ AI智能取名后端启动成功！");
		System.out.println("📖 API文档: http://localhost:8000");
	}

}
