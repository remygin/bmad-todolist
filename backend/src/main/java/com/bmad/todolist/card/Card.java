package com.bmad.todolist.card;

import com.bmad.todolist.board.Board;
import com.bmad.todolist.board.ColumnStatus;
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
import java.time.Instant;

@Entity
@Table(name = "cards")
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ColumnStatus status;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected Card() {
	}

	public Card(Board board, String title, String description, ColumnStatus status, int position) {
		this.board = board;
		this.title = title;
		this.description = description;
		this.status = status;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Board getBoard() {
		return board;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public ColumnStatus getStatus() {
		return status;
	}

	public int getPosition() {
		return position;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void update(String title, String description) {
		this.title = title;
		this.description = description;
		this.updatedAt = Instant.now();
	}

	public void move(ColumnStatus status, int position) {
		this.status = status;
		this.position = position;
		this.updatedAt = Instant.now();
	}
}
