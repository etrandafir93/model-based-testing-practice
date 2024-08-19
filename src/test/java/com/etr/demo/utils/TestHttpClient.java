package com.etr.demo.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.etr.demo.utils.LogColors.*;

@Slf4j
@RequiredArgsConstructor
public class TestHttpClient {

	private final String clientName;
	private final String baseUrl;
	private final RestTemplate restTemplate = new RestTemplateBuilder().build();

	public EmployeeResponse create(String empNo, String name) {
		EmployeeRequest request = new EmployeeRequest(empNo, name);
		log.info(CYAN.paint("[%s] POST { name=%s, empNo=%s }".formatted(clientName, name, empNo)));
		return restTemplate.postForObject(baseUrl, request, EmployeeResponse.class);
	}

	public Optional<EmployeeResponse> get(String empNo) {
		log.info(YELLOW.paint("[%s] GET /%s".formatted(clientName, empNo)));
		String url = baseUrl + "/" + empNo;
		try {
			return Optional.of(restTemplate.getForObject(url, EmployeeResponse.class));
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		}
	}

	public void update(String employeeNo, String newName) {
		String url = "%s/%s?name=%s".formatted(baseUrl, employeeNo, newName);
		log.info(PURPLE.paint("[%s] PUT %s?name=%s".formatted(clientName, employeeNo, newName)));
		restTemplate.put(url, null);
	}

	public List<EmployeeResponse> getAll() {
		log.info(BLUE.paint("[%s] GET all".formatted(clientName)));
		EmployeeResponse[] employees = restTemplate.getForObject(baseUrl, EmployeeResponse[].class);
		return Arrays.asList(employees);
	}

	public record EmployeeResponse(String name, Long id, String employeeNo) {
	}

	public record EmployeeRequest(String employeeNo, String name) {
	}
}
