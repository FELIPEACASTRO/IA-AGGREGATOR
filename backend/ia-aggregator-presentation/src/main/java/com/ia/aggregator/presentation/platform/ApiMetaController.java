package com.ia.aggregator.presentation.platform;

import com.ia.aggregator.presentation.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/meta")
public class ApiMetaController {

    private final RequestMappingHandlerMapping handlerMapping;

    public ApiMetaController(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public record EndpointDto(String methods, String path) {}

    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<List<EndpointDto>>> getEndpoints() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        List<EndpointDto> endpoints = handlerMethods.entrySet().stream()
                .filter(entry -> {
                    String pkg = entry.getValue().getBeanType().getPackageName();
                    return pkg.startsWith("com.ia.aggregator.presentation");
                })
                .flatMap(entry -> {
                    RequestMappingInfo info = entry.getKey();
                    String methods = info.getMethodsCondition().getMethods().stream()
                            .map(Enum::name)
                            .sorted()
                            .collect(Collectors.joining("/"));
                    if (methods.isEmpty()) methods = "GET";

                    return info.getPatternValues().stream()
                            .map(pattern -> new EndpointDto(methods, pattern));
                })
                .sorted(Comparator.comparing(EndpointDto::path).thenComparing(EndpointDto::methods))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(endpoints));
    }
}
