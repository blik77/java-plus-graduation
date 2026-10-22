package evm.users.repository;

import evm.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRepositoryJpa extends JpaRepository<User, Long> {

    @Override
    List<User> findAllById(Iterable<Long> id);

    Page<User> findAll(Pageable pageable);

    boolean existsByEmail(String email);
}