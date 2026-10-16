package evm.main.users.service;

import evm.main.users.dto.NewRequestUserDto;
import evm.main.users.dto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto save(NewRequestUserDto newUserDto);

    List<UserResponseDto> findUsersByIds(List<Long> ids);

    List<UserResponseDto> findUsersByParam(Integer from, Integer size);

    void delete(Long userId);

}
