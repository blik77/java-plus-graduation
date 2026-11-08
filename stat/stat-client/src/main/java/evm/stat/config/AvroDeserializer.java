package evm.stat.config;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final Map<String, Class<?>> typeMapping = new HashMap<>();

    public AvroDeserializer() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (isKey) {
            return;
        }

        typeMapping.put("stats.user-actions.v1", ru.practicum.ewm.stats.avro.UserActionAvro.class);
        typeMapping.put("stats.events-similarity.v1", ru.practicum.ewm.stats.avro.EventSimilarityAvro.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        Class<?> targetType = typeMapping.get(topic);

        if (targetType == null) {
            throw new SerializationException(
                    "Неизвестный топик или не настроен маппинг для: '" + topic + "'. " +
                            "Доступные топики: " + typeMapping.keySet()
            );
        }

        try {

            SpecificRecordBase instance = (SpecificRecordBase) targetType.getDeclaredConstructor().newInstance();

            SpecificDatumReader<T> reader = new SpecificDatumReader<>(instance.getSchema());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);

            return reader.read(null, decoder);
        } catch (IOException | ReflectiveOperationException e) {
            throw new SerializationException("Ошибка десериализации Avro сообщения для топика: " + topic, e);
        }
    }

    @Override
    public void close() {
        // Ресурсы не требуют освобождения
    }
}