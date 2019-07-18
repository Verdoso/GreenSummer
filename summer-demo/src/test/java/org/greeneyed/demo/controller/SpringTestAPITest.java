package org.greeneyed.demo.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.greeneyed.demo.Application;
import org.greeneyed.demo.controller.SpringTestAPITest.Config;
import org.greeneyed.demo.model.App;
import org.greeneyed.summer.config.XsltConfiguration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import lombok.Data;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { Application.class, Config.class })
@TestPropertySource({ "classpath:application-test.properties" })
@ActiveProfiles({ "test" })
@Data
public class SpringTestAPITest {

	private static final PodamFactory PODAM = new PodamFactoryImpl();

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mvc;

	@Before
	public void before() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Configuration
	@Profile("test")
	protected static class Config {
//		@Bean
//		public XService xService() {
//			final XService xService = Mockito.mock(XService.class);
//			//@formatter:off
//			//@formatter:on
//			return xService;
//		}
	}

	@Test
	public void basicXSLTTestIsProcessedCorrectly() throws Exception {

		// Obtaining response and basic tests
		MvcResult response = this.mvc
				//
				.perform(get("/test"))
				//
				//.andDo(print())
				//
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("Test label")))
				//
				.andReturn()
				//
				;
		// Check the model
		final Object model = response.getModelAndView().getModel().get(XsltConfiguration.XML_SOURCE_TAG);
		assertNotNull("Model object returned is not null", model);
		assertThat("Model object is of the appropriate class", model, instanceOf(App.class));
		App app = (App) model;
		// Further App checking...

		// Check the response
		Document html = Jsoup.parse(response.getResponse().getContentAsString());

		Element headerElement = html.selectFirst("h1");
		assertNotNull("We have a title", headerElement);
		assertThat("We have a title", "TEST", equalTo(headerElement.text()));
	}

	@Test
	public void showXMLSourceWorksOnBasicTest() throws Exception {

		// Obtaining response and basic tests
		ResultActions resultActions = this.mvc
				//
				.perform(get("/test?showXMLSource=true"))
				//
				.andDo(print())
				//
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
				//
				// .andReturn()
				//
				;
		// Check the XML
		resultActions.andExpect(xpath("/app/pojos/pojo").exists());
	}
}