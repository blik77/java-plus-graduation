package evm.event.client;

import evm.event.dto.UserDto;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUserById(Long id) {
        UserDto user = new UserDto();
        user.setId(id);
        user.setName("Unknown");
        return user;
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        return ids.stream().map(id -> {
            UserDto user = new UserDto();
            user.setId(id);
            user.setName("Unknown");
            return user;
        }).toList();
    }
}