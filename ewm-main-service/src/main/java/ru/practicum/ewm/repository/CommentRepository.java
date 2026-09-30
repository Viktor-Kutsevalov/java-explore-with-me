package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId ORDER BY c.created DESC")
    Page<Comment> findAllByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "event"})
    @Query("SELECT c FROM Comment c WHERE c.id = :id")
    Optional<Comment> findWithAuthorAndEventById(@Param("id") Long id);
}
