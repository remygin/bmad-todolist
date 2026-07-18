package com.bmad.todolist.board;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

	List<Board> findAllByAuthorIdOrderById(Long authorId);

	Optional<Board> findByIdAndAuthorId(Long id, Long authorId);

	boolean existsByAuthorIdAndNameIgnoreCase(Long authorId, String name);

	boolean existsByAuthorIdAndNameIgnoreCaseAndIdNot(Long authorId, String name, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Board b where b.id = :id and b.author.id = :authorId")
	Optional<Board> findOwnedByIdForUpdate(@Param("id") Long id, @Param("authorId") Long authorId);
}
