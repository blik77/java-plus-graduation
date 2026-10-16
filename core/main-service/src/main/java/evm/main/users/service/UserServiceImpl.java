package evm.main.users.service;

import evm.main.comment.repository.CommentRepository;
import evm.main.exceptions.ConflictException;
import evm.main.exceptions.NotFoundException;
import evm.main.users.dto.NewRequestUserDto;
import evm.main.users.dto.UserResponseDto;
import evm.main.users.mapper.UserMapper;
import evm.main.users.model.User;
import evm.main.users.repository.UserRepositoryJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepositoryJpa userRepositoryJpa;
    private final UserMapper userMapper;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public UserResponseDto save(NewRequestUserDto newUserDto) {

        boolean checkUser = userRepositoryJpa.existsByEmail(newUserDto.getEmail());

        if (checkUser) {
            throw new ConflictException("Пользователь с email " + newUserDto.getEmail() + " уже создан.");
        }

        User user = userRepositoryJpa.save(userMapper.mapToUser(newUserDto));
        return userMapper.mapToUserResponseDto(user);
    }

    @Override
    public List<UserResponseDto> findUsersByIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepositoryJpa.findAllById(ids);

        return users.stream()
                .map(userMapper::mapToUserResponseDto)
                .toList();
    }

    @Override
    public List<UserResponseDto> findUsersByParam(Integer from, Integer size) {

        Pageable pageable = PageRequest.of(from / size, size);

        return userRepositoryJpa.findAll(pageable)
                .map(userMapper::mapToUserResponseDto)
                .getContent();

    }

    @Override
    @Transactional
    public void delete(Long userId) {

        boolean checkUser = userRepositoryJpa.existsById(userId);

        if (!checkUser) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }

        commentRepository.deleteAllByAuthorId(userId);
        userRepositoryJpa.deleteById(userId);
    }

}
