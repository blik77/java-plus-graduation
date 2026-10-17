package evm.main.users.controller;

import evm.main.users.dto.NewRequestUserDto;
import evm.main.users.dto.UserResponseDto;
import evm.main.users.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponseDto> findUsers(
            @RequestParam(value = "ids", required = false) Long[] ids,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size) {
        if (ids == null || ids.length == 0) {
            return userService.findUsersByParam(from, size);
        }
        return userService.findUsersByIds(Arrays.asList(ids));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto save(
            @Valid @RequestBody NewRequestUserDto newRequestUserDto) {
        return userService.save(newRequestUserDto);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId) {
        userService.delete(userId);
    }

}
