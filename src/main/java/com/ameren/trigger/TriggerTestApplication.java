package com.ameren.trigger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class TriggerTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TriggerTestApplication.class, args);
//		SpringApplication application = new SpringApplication(TriggerTestApplication.class);
//		Properties properties = new Properties();
//		properties.setProperty("spring.main.banner-mode", "log");
//		LocalDateTime dateTime = LocalDateTime.now(); 
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
//		System.out.println(dateTime.format(formatter));
//		String logFileName = "LoadTest-" + dateTime.format(formatter) + ".log";
//		properties.setProperty("logging.file",logFileName);
//		properties.setProperty("logging.pattern.console", "");
//		application.setDefaultProperties(properties);
//		application.run(args);
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

//	@Bean
//	JedisConnectionFactory jedisConnectionFactory() {
//	    JedisConnectionFactory jedisConFactory
//	      = new JedisConnectionFactory();
//	    jedisConFactory.setHostName("localhost");
//	    jedisConFactory.setPort(6379);
//	    return jedisConFactory;
//	}
//
//	@Bean
//	public RedisTemplate<String, Object> redisTemplate() {
//	    RedisTemplate<String, Object> template = new RedisTemplate<>();
//	    template.setConnectionFactory(jedisConnectionFactory());
//	    return template;
//	}
}
