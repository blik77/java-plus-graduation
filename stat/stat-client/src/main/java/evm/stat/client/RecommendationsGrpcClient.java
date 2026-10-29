package evm.stat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.dashboard.*;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Spliterators;
import java.util.Spliterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsGrpcClient {

    @GrpcClient("analyzer-service")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> responseIterator = analyzerStub.getInteractionsCount(request);
            return iteratorToList(responseIterator);
        } catch (Exception e) {
            log.warn("Не удалось получить количество взаимодействий: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Получает похожие мероприятия для указанного события.
     */
    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> responseIterator = analyzerStub.getSimilarEvents(request);
            return iteratorToList(responseIterator);
        } catch (Exception e) {
            log.warn("Не удалось получить похожие события: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Получает персональные рекомендации для пользователя.
     */
    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> responseIterator = analyzerStub.getRecommendationsForUser(request);
            return iteratorToList(responseIterator);
        } catch (Exception e) {
            log.warn("Не удалось получить рекомендации: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RecommendedEventProto> iteratorToList(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).collect(Collectors.toList());
    }
}