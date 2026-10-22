package evm.event.port;

import java.util.List;
import java.util.Map;

public interface RequestStatsPort {

    long countConfirmed(Long eventId);

    Map<Long, Long> countConfirmedByEventIds(List<Long> eventIds);
}