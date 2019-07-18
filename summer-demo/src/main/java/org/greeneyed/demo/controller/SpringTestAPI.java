package org.greeneyed.demo.controller;

import java.util.Arrays;

import org.greeneyed.demo.model.App;
import org.greeneyed.demo.model.MyPojo;
import org.greeneyed.summer.config.JoltViewConfiguration.JoltModelAndView;
import org.greeneyed.summer.config.XsltConfiguration.XsltModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@Data
public class SpringTestAPI {

	public static final String TEST_XSLT_SOURCE = "pojo_process";
	@Autowired
	private ConfigurableEnvironment env;

	@RequestMapping(value = "/test")
	public ModelAndView testInterface() {
		return new XsltModelAndView(TEST_XSLT_SOURCE, generateAppObject(), HttpStatus.OK);
	}

	@RequestMapping(value = "/param_test")
	public ResponseEntity<String> testParameters(@RequestParam(name = "name_x") String nameString,
			@RequestParam(name = "name_int") int nameInteger, @RequestParam(name = "name_array") String[] theValues) {
		// throw new RuntimeException("Probando, probando");
		return new ResponseEntity<>(TEST_XSLT_SOURCE, HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
	public App testJSON() {
		return generateAppObject();
	}

	@RequestMapping(value = "/jolt")
	public ModelAndView testJOLT() {
		log.debug("Testing");
		return new JoltModelAndView("jolt-test", generateAppObject(), HttpStatus.OK);
	}
	
	private App generateAppObject() {
		App app = new App(Arrays.asList(new MyPojo("anId", "aName"), new MyPojo("anotherId", "anotherName")));
		return app;
	}
}
