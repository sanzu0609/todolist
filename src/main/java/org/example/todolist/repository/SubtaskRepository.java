package org.example.todolist.repository;

import java.util.List;
import java.util.Optional;
import org.example.todolist.domain.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    @Query("select s from Subtask s where s.id = :id and s.task.owner.id = :ownerId")
    Optional<Subtask> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query("select s from Subtask s where s.task.id = :taskId and s.task.owner.id = :ownerId")
    List<Subtask> findByTaskIdAndOwnerId(@Param("taskId") Long taskId, @Param("ownerId") Long ownerId);

    boolean existsByTaskIdAndTitle(Long taskId, String title);
}
