package evm.event.mapper;


import evm.event.dto.LocationDto;
import evm.event.model.Location;

public class LocationMapper {

    private LocationMapper() {
    }

    public static Location toEntity(LocationDto dto) {
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static LocationDto toDto(Location location) {
        if (location == null) return null;
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}