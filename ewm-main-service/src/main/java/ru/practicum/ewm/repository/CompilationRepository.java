package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Compilation;

import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @EntityGraph(attributePaths = {"events"})
    @Query("SELECT c FROM Compilation c WHERE c.id = :id")
    Optional<Compilation> findWithEventsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"events"})
    @Query("SELECT c FROM Compilation c " +
            "WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Compilation> findAllByPinned(@Param("pinned") Boolean pinned, Pageable pageable);
}
