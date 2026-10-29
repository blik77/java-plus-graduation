package ru.practicum.service;

import com.google.protobuf.Empty;
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

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionServiceImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получено действие пользователя: user={}, event={}, action={}",
                request.getUserId(), request.getEventId(), request.getActionType());

        ActionTypeAvro avroActionType = mapToAvroActionType(request.getActionType());

        long timestampMs = request.getTimestamp().getSeconds() * 1000 + request.getTimestamp().getNanos() / 1_000_000;

        UserActionAvro avroMessage = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(avroActionType)
                .setTimestamp(Instant.ofEpochMilli(timestampMs))
                .build();

        kafkaTemplate.send("stats.user-actions.v1", request.getUserId(), avroMessage);
        log.info("Сообщение успешно отправлено в Kafka");

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
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