package evm.comment.client;

import evm.comment.client.dto.UserDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @Cacheable(value = "users", key = "#ids.get(0)")
    @GetMapping("/admin/users")
    List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);

}