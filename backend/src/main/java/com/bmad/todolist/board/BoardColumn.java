package com.bmad.todolist.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "board_columns")
public class BoardColumn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ColumnStatus status;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Column(nullable = false)
	private int position;

	protected BoardColumn() {
	}

	public BoardColumn(Board board, ColumnStatus status, String displayName, int position) {
		this.board = board;
		this.status = status;
		this.displayName = displayName;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Board getBoard() {
		return board;
	}

	public ColumnStatus getStatus() {
		return status;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getPosition() {
		return position;
	}

	public void configure(String displayName, int position) {
		this.displayName = displayName;
		this.position = position;
	}
}
