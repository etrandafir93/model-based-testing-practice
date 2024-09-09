package com.etr.demo.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.etr.demo.utils.LogColors.*;

@Slf4j
@RequiredArgsConstructor
public class TestHttpClient {

	private final String clientName;
	private final String baseUrl;
	private final RestClient restClient = RestClient.create();

	ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public ApiResponse<EmployeeDto> get(String employeeNo) {
		log.info(YELLOW.paint("[%s] GET /api/employees/%s".formatted(clientName, employeeNo)));
		return restClient.get()
				.uri("%s/%s".formatted(baseUrl, employeeNo))
				.exchange((req, resp) -> new ApiResponse<>(
						resp.getStatusCode().value(),
						resp.getStatusCode().is2xxSuccessful()
								? objectMapper.readValue(resp.getBody(), EmployeeDto.class)
								: null));
	}

	public ApiResponse<Void> put(String employeeNo, String newName) {
		log.info(PURPLE.paint("[%s] PUT /api/employees%s?name=%s".formatted(clientName, employeeNo, newName)));
		return restClient.put()
				.uri("%s/%s?newName=%s".formatted(baseUrl, employeeNo, newName))
				.exchange((req, resp) -> new ApiResponse<>(
						resp.getStatusCode().value(), null));
	}

	public ApiResponse<List<EmployeeDto>> getByDepartment(String department) {
		log.info(BLUE.paint("[%s] GET /api/employees?department=%s".formatted(clientName, department)));
		return restClient.get()
				.uri("%s?department=%s".formatted(baseUrl, department))
				.exchange((req, resp) -> new ApiResponse<>(
						resp.getStatusCode().value(),
						resp.getStatusCode().is2xxSuccessful()
								? objectMapper.readValue(resp.getBody(), new TypeReference<List<EmployeeDto>>() {})
								: null));
	}

	public ApiResponse<EmployeeDto> post(String employeeNo, String name) {
		log.info(CYAN.paint("[%s] POST /api/employees { name=%s, empNo=%s }".formatted(clientName, name, employeeNo)));
		return restClient.post()
				.uri(baseUrl)
				.body(new EmployeeDto(employeeNo, name))
				.exchange((req, resp) -> new ApiResponse<>(
						resp.getStatusCode().value(),
						objectMapper.readValue(resp.getBody(), EmployeeDto.class)));
	}

	public record ApiResponse<T>(int statusCode, @Nullable T body) {
	}

	public record EmployeeDto(String employeeNo, String name) {
	}

}
