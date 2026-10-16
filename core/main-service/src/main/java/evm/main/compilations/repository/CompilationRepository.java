package evm.main.compilations.repository;

import evm.main.compilations.model.Compilation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    // Подборки с фильтром по pinned
    // Если pinned = null — возвращаем все подборки
    List<Compilation> findAllByPinned(Boolean pinned, Pageable pageable);
}

