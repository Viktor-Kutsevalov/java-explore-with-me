package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findWithCategoryAndInitiatorById(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("SELECT e FROM Event e WHERE e.initiator.id = :userId ORDER BY e.id")
    Page<Event> findAllByInitiatorId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text = '' OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "              OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categoriesEmpty = TRUE OR e.category.id IN :categories) " +
            "AND (:paidIsNull = TRUE OR e.paid = :paid) " +
            "AND e.eventDate >= :rangeStart " +
            "AND (:rangeEndIsNull = TRUE OR e.eventDate <= :rangeEnd) " +
            "AND (:onlyAvailable = FALSE OR e.participantLimit = 0 " +
            "     OR e.participantLimit > (SELECT COUNT(r) FROM ParticipationRequest r " +
            "                              WHERE r.event.id = e.id AND r.status = 'CONFIRMED'))")
    Page<Event> findPublishedEvents(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("categoriesEmpty") boolean categoriesEmpty,
            @Param("paid") Boolean paid,
            @Param("paidIsNull") boolean paidIsNull,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("rangeEndIsNull") boolean rangeEndIsNull,
            @Param("onlyAvailable") boolean onlyAvailable,
            Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("SELECT e FROM Event e " +
            "WHERE (:usersEmpty = TRUE OR e.initiator.id IN :users) " +
            "AND (:statesEmpty = TRUE OR e.state IN :states) " +
            "AND (:categoriesEmpty = TRUE OR e.category.id IN :categories) " +
            "AND e.eventDate >= :rangeStart " +
            "AND (:rangeEndIsNull = TRUE OR e.eventDate <= :rangeEnd)")
    Page<Event> findEventsByAdmin(
            @Param("users") List<Long> users,
            @Param("usersEmpty") boolean usersEmpty,
            @Param("states") List<EventState> states,
            @Param("statesEmpty") boolean statesEmpty,
            @Param("categories") List<Long> categories,
            @Param("categoriesEmpty") boolean categoriesEmpty,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("rangeEndIsNull") boolean rangeEndIsNull,
            Pageable pageable);
}
