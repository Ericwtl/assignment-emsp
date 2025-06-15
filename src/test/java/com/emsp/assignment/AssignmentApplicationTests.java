package com.emsp.assignment;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AssignmentApplicationTests {

	// 1. 修复容器声明
	@Container
	public static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:15")
					.withDatabaseName("testdb")
					.withUsername("testuser")
					.withPassword("testpass")
					.withReuse(true); // 允许容器重用

	// 2. 动态注入数据库配置
	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		// 强制使用标准PostgreSQL驱动
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		// 禁用测试环境下的Flyway
		registry.add("spring.flyway.enabled", () -> "false");

		executeFlywayMigration();
	}

	private static void executeFlywayMigration() {
		Flyway flyway = Flyway.configure()
				.dataSource(postgres.getJdbcUrl(),
						postgres.getUsername(),
						postgres.getPassword())
				.locations("classpath:db/migration") // 指向您的迁移脚本目录
				.baselineOnMigrate(true)
				.load();

		flyway.migrate();
	}

	// 3. 注入依赖
	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
		// 测试Spring上下文加载
	}

	@Test
	void testDatabaseConnection() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		assertEquals(1, result, "Database connecting test failed.");
	}

	@Test
	void testDatabaseInitialization() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		// 验证表是否存在
		List<String> tables = jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class
		);
		tables.stream().forEach(table ->{
			System.out.println(table);
		});
		assertTrue(tables.contains("account"));
	}
}