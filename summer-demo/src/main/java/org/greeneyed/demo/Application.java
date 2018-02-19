package org.greeneyed.demo;

import org.greeneyed.summer.config.enablers.EnableSummer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({ "org.greeneyed.demo" })
@EnableSummer(xslt_view = true, xml_view_pooling = true, log4j = false, logback = false, slf4j_filter = false)
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
