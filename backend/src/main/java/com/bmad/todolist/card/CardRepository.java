package com.bmad.todolist.card;

import com.bmad.todolist.board.ColumnStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

	List<Card> findAllByBoardIdOrderByStatusAscPositionAsc(Long boardId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Card> findAllByBoardIdAndStatusOrderByPosition(Long boardId, ColumnStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Card> findByIdAndBoardAuthorId(Long id, Long authorId);

	@Query("select c.board.id from Card c where c.id = :id and c.board.author.id = :authorId")
	Optional<Long> findOwnedBoardId(@Param("id") Long id, @Param("authorId") Long authorId);

	long countByBoardIdAndStatus(Long boardId, ColumnStatus status);
}
