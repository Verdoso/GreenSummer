package org.greeneyed.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoClient {

	public static void main(String[] args) throws InterruptedException {
		log.info("Initiating test");

		RestTemplate restTemplate = new RestTemplate();
		ExecutorService executor = Executors.newFixedThreadPool(10);
		List<Callable<HttpStatus>> callableTasks = new ArrayList<>();

		for (int i = 0; i < 200; i++) {
			callableTasks.add(new Callable<HttpStatus>() {
				@Override
				public HttpStatus call() throws Exception {
					ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9090/test/", String.class);
					log.info("Response: {} ", response.getStatusCode());
					return response.getStatusCode();
				}
			});
		}

		executor.invokeAll(callableTasks);
		executor.shutdownNow();
		executor.awaitTermination(1, TimeUnit.MINUTES);

		log.info("Finished test");

	}

}
