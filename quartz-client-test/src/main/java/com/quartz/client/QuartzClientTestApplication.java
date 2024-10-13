package com.quartz.client;

import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.context.annotation.Bean;

// SchedulingConfiguration
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class, EmbeddedWebServerFactoryCustomizerAutoConfiguration.class})
//@EnableScheduling
//@SpringBootApplication
public class QuartzClientTestApplication {
	public static void main(String[] args) {
		SpringApplication.run(QuartzClientTestApplication.class,args);
	}

	@Bean
	public Tomcat tomcat(){
		return new Tomcat();
	}
}
