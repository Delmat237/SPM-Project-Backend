package com.techwave.auth.collaboration.dto;

import com.techwave.auth.collaboration.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String content;

    // Auteur
    private Long authorId;
    private String authorName;
    private String authorEmail;

    // Tâche
    private Long taskId;
    private String taskKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getNom())
                .authorEmail(comment.getAuthor().getEmail())
                .taskId(comment.getTask().getId())
                .taskKey(comment.getTask().getTaskKey())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static List<CommentResponse> fromList(List<Comment> comments) {
        return comments.stream().map(CommentResponse::from).collect(Collectors.toList());
    }
}
