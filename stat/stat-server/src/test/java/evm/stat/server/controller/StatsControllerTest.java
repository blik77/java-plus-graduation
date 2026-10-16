package evm.stat.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import evm.stat.dto.HitDto;
import evm.stat.server.repository.StatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StatsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

// -----------------------------------------------------------------------
// Тесты POST /hit
// -----------------------------------------------------------------------

    @Test
    void hit_shouldReturn201_whenValidRequest() throws Exception {
        HitDto hit = HitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());
    }

    @Test
    void hit_shouldReturn400_whenAppIsBlank() throws Exception {
        HitDto hit = HitDto.builder()
                .app("")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isBadRequest());
    }

// -----------------------------------------------------------------------
// Тесты GET /stats
// -----------------------------------------------------------------------

    @Test
    void getStats_shouldReturnHits_whenHitsExist() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(1));
    }

    @Test
    void getStats_shouldReturnEmptyList_whenNoHits() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStats_shouldCountUniqueIps_whenUniqueIsTrue() throws Exception {
        // три хита на /events/1 — два с одного ip, один с другого
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/1", "192.168.0.1"); // повтор
        saveHit("ewm-main-service", "/events/1", "192.168.0.2"); // другой ip

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                // уникальных ip = 2
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    @Test
    void getStats_shouldCountAllHits_whenUniqueIsFalse() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/1", "192.168.0.2");

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                // всего три хита
                .andExpect(jsonPath("$[0].hits").value(3));
    }

    @Test
    void getStats_shouldFilterByUri() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/2", "192.168.0.1");

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00")
                        .param("uris", "/events/1")) // фильтруем только /events/1
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].uri").value("/events/1"));
    }

    @Test
    void getStats_shouldFilterByMultipleUris() throws Exception {
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/2", "192.168.0.1");
        saveHit("ewm-main-service", "/events/3", "192.168.0.1");

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00")
                        .param("uris", "/events/1", "/events/2")) // два uri
                .andExpect(status().isOk())
                // /events/3 не попал в фильтр
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getStats_shouldSortByHitsDesc() throws Exception {
        // /events/2 получит больше хитов — должен быть первым
        saveHit("ewm-main-service", "/events/1", "192.168.0.1");
        saveHit("ewm-main-service", "/events/2", "192.168.0.1");
        saveHit("ewm-main-service", "/events/2", "192.168.0.2");

        mockMvc.perform(get("/stats")
                        .param("start", "2020-01-01 00:00:00")
                        .param("end", "2030-01-01 00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uri").value("/events/2")) // первый — самый популярный
                .andExpect(jsonPath("$[0].hits").value(2))
                .andExpect(jsonPath("$[1].uri").value("/events/1"))
                .andExpect(jsonPath("$[1].hits").value(1));
    }

    @Test
    void getStats_shouldReturn400_whenStartAfterEnd() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2030-01-01 00:00:00")
                        .param("end", "2020-01-01 00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    //сохраняет хит через HTTP
    private void saveHit(String app, String uri, String ip) throws Exception {
        HitDto hit = HitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hit)));
    }
}