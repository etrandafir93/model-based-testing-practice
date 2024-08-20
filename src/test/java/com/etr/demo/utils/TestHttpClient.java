package com.etr.demo.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.etr.demo.utils.LogColors.*;

@Slf4j
@RequiredArgsConstructor
public class TestHttpClient {

	private final String clientName;
	private final String baseUrl;
	private final RestTemplate restTemplate = new RestTemplateBuilder().build();

	public EmployeeDto create(String empNo, String name) {
		CreateEmployeeRequest request = new CreateEmployeeRequest(empNo, name);
		log.info(CYAN.paint("[%s] POST /api/employees { name=%s, empNo=%s }".formatted(clientName, name, empNo)));
		return restTemplate.postForObject(baseUrl, request, EmployeeDto.class);
	}

	public Optional<EmployeeDto> get(String empNo) {
		log.info(YELLOW.paint("[%s] GET /api/employees/%s".formatted(clientName, empNo)));
		String url = baseUrl + "/" + empNo;
		try {
			return Optional.of(restTemplate.getForObject(url, EmployeeDto.class));
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		}
	}

	public void update(String employeeNo, String newName) {
		String url = "%s/%s?name=%s".formatted(baseUrl, employeeNo, newName);
		log.info(PURPLE.paint("[%s] PUT /api/employees%s?name=%s".formatted(clientName, employeeNo, newName)));
		restTemplate.put(url, null);
	}

	public List<EmployeeDto> getAll() {
		log.info(BLUE.paint("[%s] GET /api/employees".formatted(clientName)));
		EmployeeDto[] employees = restTemplate.getForObject(baseUrl, EmployeeDto[].class);
		return Arrays.asList(employees);
	}

	public record EmployeeDto(String name, String employeeNo) {
	}

	public record CreateEmployeeRequest(String employeeNo, String name) {
	}
}
