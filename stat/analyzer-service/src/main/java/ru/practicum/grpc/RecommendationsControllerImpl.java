package ru.practicum.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.service.RecommendationService;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    /**
     * gRPC-метод: вернуть рекомендации для пользователя (поток событий).
     */
    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("Запрос рекомендаций для пользователя {}, maxResults={}", userId, maxResults);

        List<Map.Entry<Long, Double>> recommendations =
                recommendationService.getRecommendationsForUser(userId, maxResults);

        for (Map.Entry<Long, Double> entry : recommendations) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(entry.getKey())
                    .setScore(entry.getValue())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Отправлено {} рекомендаций для пользователя {}", recommendations.size(), userId);
    }

    /**
     * gRPC-метод: вернуть похожие мероприятия (поток событий).
     */
    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("Запрос похожих событий для event={}, user={}, maxResults={}", eventId, userId, maxResults);

        List<Map.Entry<Long, Double>> similarEvents =
                recommendationService.getSimilarEvents(eventId, userId, maxResults);

        for (Map.Entry<Long, Double> entry : similarEvents) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(entry.getKey())
                    .setScore(entry.getValue())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Отправлено {} похожих событий для event={}", similarEvents.size(), eventId);
    }

    /**
     * gRPC-метод: вернуть сумму весов взаимодействий для списка мероприятий.
     */
    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        List<Long> eventIds = request.getEventIdList();

        log.info("Запрос количества взаимодействий для {} мероприятий", eventIds.size());

        List<Map.Entry<Long, Double>> interactions =
                recommendationService.getInteractionsCount(eventIds);

        for (Map.Entry<Long, Double> entry : interactions) {
            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(entry.getKey())
                    .setScore(entry.getValue())
                    .build();
            responseObserver.onNext(proto);
        }

        responseObserver.onCompleted();
        log.info("Отправлены данные о взаимодействиях для {} мероприятий", interactions.size());
    }

}