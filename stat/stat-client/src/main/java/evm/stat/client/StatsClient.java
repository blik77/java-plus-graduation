package evm.stat.client;

import evm.stat.dto.HitDto;
import evm.stat.dto.ViewStatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class StatsClient extends BaseClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatsClient(DiscoveryClient discoveryClient,
                       RetryTemplate retryTemplate,
                       String statsServiceId) {
        super("http://localhost");
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statsServiceId = statsServiceId;
    }

    @Async
    public void hit(HitDto hitDto) {
        try {
            log.debug("Отправляем hit: app={}, uri={}, ip={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp());
            URI uri = makeUri("/hit");
            restClient.post()
                .uri(uri)
                .body(hitDto)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Не удалось отправить hit в сервис статистики: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .path("/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                uris.forEach(uri -> builder.queryParam("uris", uri));
            }

            String path = builder.build().encode().toUriString();
            URI uri = makeUri(path);

            ViewStatsDto[] result = restClient.get()
                .uri(uri)
                .retrieve()
                .body(ViewStatsDto[].class);
            return result != null ? List.of(result) : List.of();
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            return List.of();
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException(
                "Сервис статистики с id '" + statsServiceId + "' не найден в Eureka");
        }
        return instances.getFirst();
    }
}