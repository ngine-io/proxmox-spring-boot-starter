package io.ngine.proxmox.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Executes requests against the Proxmox API: encodes parameters the way Proxmox expects
 * them and unwraps the response envelope.
 */
final class ApiSupport {

    private final RestClient restClient;

    ApiSupport(RestClient restClient) {
        this.restClient = restClient;
    }

    <T> T get(Class<T> type, String path, Object... uriVariables) {
        return exchange(HttpMethod.GET, path, Map.of(), null, ResolvableType.forClass(type), uriVariables);
    }

    <T> T get(Class<T> type, String path, Map<String, ?> query, Object... uriVariables) {
        return exchange(HttpMethod.GET, path, query, null, ResolvableType.forClass(type), uriVariables);
    }

    <T> List<T> getList(Class<T> elementType, String path, Object... uriVariables) {
        return getList(elementType, path, Map.of(), uriVariables);
    }

    <T> List<T> getList(Class<T> elementType, String path, Map<String, ?> query, Object... uriVariables) {
        List<T> list = exchange(HttpMethod.GET, path, query, null,
                ResolvableType.forClassWithGenerics(List.class, elementType), uriVariables);
        return list != null ? list : List.of();
    }

    <T> T post(Class<T> type, String path, Map<String, ?> params, Object... uriVariables) {
        return exchange(HttpMethod.POST, path, Map.of(), params, ResolvableType.forClass(type), uriVariables);
    }

    void put(String path, Map<String, ?> params, Object... uriVariables) {
        exchange(HttpMethod.PUT, path, Map.of(), params, ResolvableType.forClass(Object.class), uriVariables);
    }

    <T> T delete(Class<T> type, String path, Map<String, ?> query, Object... uriVariables) {
        return exchange(HttpMethod.DELETE, path, query, null, ResolvableType.forClass(type), uriVariables);
    }

    private <T> T exchange(HttpMethod method, String path, Map<String, ?> query, Map<String, ?> params,
            ResolvableType dataType, Object... uriVariables) {
        Function<UriBuilder, URI> uri = builder -> {
            builder.path(path);
            encode(query).forEach(builder::queryParam);
            return builder.build(uriVariables);
        };
        RestClient.RequestBodySpec request = this.restClient.method(method).uri(uri);
        if (params != null) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(encode(params));
        }
        ParameterizedTypeReference<PveResponse<T>> responseType = ParameterizedTypeReference
                .forType(ResolvableType.forClassWithGenerics(PveResponse.class, dataType).getType());
        PveResponse<T> response = request.retrieve().body(responseType);
        return response != null ? response.data() : null;
    }

    /**
     * Encodes parameters as Proxmox expects them: booleans as {@code 0}/{@code 1},
     * collections as comma separated lists, {@code null} values omitted.
     */
    static MultiValueMap<String, String> encode(Map<String, ?> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach((name, value) -> {
            if (value != null) {
                form.add(name, encodeValue(value));
            }
        });
        return form;
    }

    private static String encodeValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder joined = new StringBuilder();
            iterable.forEach(item -> joined.append(joined.isEmpty() ? "" : ",").append(encodeValue(item)));
            return joined.toString();
        }
        return value.toString();
    }
}
