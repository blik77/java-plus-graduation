package evm.users.service;

import evm.users.dto.NewRequestUserDto;
import evm.users.dto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto save(NewRequestUserDto newUserDto);

    List<UserResponseDto> findUsersByIds(List<Long> ids);

    List<UserResponseDto> findUsersByParam(Integer from, Integer size);

    void delete(Long userId);

    UserResponseDto findById(Long userId);

}
