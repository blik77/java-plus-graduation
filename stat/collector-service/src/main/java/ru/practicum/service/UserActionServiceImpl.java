package ru.practicum.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;
import java.util.concurrent.CompletionException;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionServiceImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private static final String TOPIC = "stats.user-actions.v1";

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        final UserActionAvro avroMessage;

        try {
            ActionTypeAvro avroActionType = mapToAvroActionType(request.getActionType());

            long timestampMs = request.getTimestamp().getSeconds() * 1_000
                    + request.getTimestamp().getNanos() / 1_000_000;

            avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(avroActionType)
                    .setTimestamp(Instant.ofEpochMilli(timestampMs))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный тип действия: {}", request.getActionType(), e);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
            return;
        } catch (RuntimeException e) {
            log.error("Не удалось обработать действие пользователя", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Не удалось обработать действие пользователя")
                    .withCause(e)
                    .asRuntimeException());
            return;
        }

        log.info("Получено действие пользователя: user={}, event={}, action={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        try {
            kafkaTemplate.send(TOPIC, request.getUserId(), avroMessage)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            Throwable cause = error instanceof CompletionException && error.getCause() != null
                                    ? error.getCause()
                                    : error;
                            log.error("Не удалось отправить действие пользователя в Kafka", cause);
                            responseObserver.onError(Status.UNAVAILABLE
                                    .withDescription("Kafka временно недоступна")
                                    .withCause(cause)
                                    .asRuntimeException());
                            return;
                        }

                        log.info("Сообщение успешно подтверждено Kafka");
                        responseObserver.onNext(Empty.getDefaultInstance());
                        responseObserver.onCompleted();
                    });
        } catch (RuntimeException e) {
            log.error("Ошибка при отправке действия пользователя в Kafka", e);
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Не удалось отправить сообщение в Kafka")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private ActionTypeAvro mapToAvroActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Неизвестный тип действия");
        };
    }
}