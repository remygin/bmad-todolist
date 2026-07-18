package com.bmad.todolist.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

	List<BoardColumn> findAllByBoardIdOrderByPosition(Long boardId);
}
