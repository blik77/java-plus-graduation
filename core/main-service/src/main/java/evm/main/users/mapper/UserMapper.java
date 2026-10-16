package evm.main.users.mapper;

import evm.main.users.dto.NewRequestUserDto;
import evm.main.users.dto.UserResponseDto;
import evm.main.users.dto.UserShortDto;
import evm.main.users.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User mapToUser(NewRequestUserDto newUserDto) {
        User user = new User();
        user.setEmail(newUserDto.getEmail());
        user.setName(newUserDto.getName());

        return user;
    }

    public UserResponseDto mapToUserResponseDto(User user) {

        if (user == null) {
            return null;
        }

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());

        return dto;
    }

    // User → UserShortDto (краткий ответ — только id и имя)
    public static UserShortDto toShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
