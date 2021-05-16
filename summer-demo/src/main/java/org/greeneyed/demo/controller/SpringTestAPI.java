package org.greeneyed.demo.controller;

import java.util.Arrays;

import org.greeneyed.demo.model.App;
import org.greeneyed.demo.model.MyPojo;
import org.greeneyed.summer.config.XsltConfiguration.XsltModelAndView;
import org.greeneyed.summer.monitoring.Logable;
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

@RestController
@Data
public class SpringTestAPI {
  public static final String TEST_XSLT_SOURCE = "pojo_process";

  public static enum TestEnum {
    TEST_ENUM_A, TEST_ENUM_B
  }

  @Autowired
  private ConfigurableEnvironment env;

  @Data
  public static class TestClass implements Logable {
    private int code;
    private String name;

    @Override
    public String formatted() {
      final String toString = toString();
      return toString.substring(toString.indexOf("("));
    }
  }

  @RequestMapping(value = "/test")
  public ModelAndView testInterface() {
    return new XsltModelAndView(TEST_XSLT_SOURCE, generateAppObject(), HttpStatus.OK);
  }

  // http://localhost:9090/param_test?name_x=x&name_int=1&name_array=name1,name2,name3&int_array=1,2,3&enum_array=TEST_ENUM_A,TEST_ENUM_B
  @RequestMapping(value = "/param_test")
  public ResponseEntity<String> testParameters(
      @RequestParam(name = "name_x") String nameString,
      @RequestParam(name = "name_int") int nameInteger,
      @RequestParam(name = "name_array") String[] theValues,
      @RequestParam(name = "int_array") int[] intValues,
      @RequestParam(name = "enum_array") TestEnum[] enumValues

  ) {
    return new ResponseEntity<>(TEST_XSLT_SOURCE, HttpStatus.BAD_REQUEST);
  }

  // http://localhost:9090/param_class_test?code=1&&name=aName
  @RequestMapping(value = "/param_class_test")
  public ResponseEntity<String> testParameters(
      TestClass testValue) {
    return new ResponseEntity<>(TEST_XSLT_SOURCE, HttpStatus.OK);
  }

  @RequestMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
  public App testJSON() {
    return generateAppObject();
  }

  private App generateAppObject() {
    App app = new App(Arrays.asList(new MyPojo("anId", "aName"), new MyPojo("anotherId", "anotherName")));
    return app;
  }
}
