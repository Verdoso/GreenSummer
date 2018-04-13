package org.greeneyed.demo.controller;

import java.util.Arrays;

import org.greeneyed.demo.model.App;
import org.greeneyed.demo.model.MyPojo;
import org.greeneyed.summer.config.XsltConfiguration.XsltModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private ConfigurableEnvironment env;

    @RequestMapping(value = "/test")
    public ModelAndView testInterface() {
        App app = new App(Arrays.asList(new MyPojo("anId", "aName"), new MyPojo("anotherId", "anotherName")));
        log.debug("Testing");
        return new XsltModelAndView("pojo_process", app, HttpStatus.OK);
    }

    @RequestMapping(value = "/param_test")
    public ResponseEntity<String> testParameters(@RequestParam(name = "name_x") String nameString, @RequestParam(name = "name_int") int nameInteger,
        @RequestParam(name = "name_array") String[] theValues) {
        //throw new RuntimeException("Probando, probando");
        return new ResponseEntity<>("pojo_process", HttpStatus.BAD_REQUEST);
    }
}
