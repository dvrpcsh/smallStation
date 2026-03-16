package com.majungmul.api.domain.comment.repository;

import com.majungmul.api.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 댓글(Comment) 데이터 접근 레포지토리.
 *
 * <p>논리 삭제된 댓글({@code is_deleted = true})은 모든 조회에서 자동 제외된다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 삭제되지 않은 댓글을 작성 시각 오름차순으로 조회한다.
     *
     * <p>N+1 방지: author(User)를 fetch join으로 함께 조회.
     * 댓글 목록에서 닉네임 표시를 위해 author를 즉시 로딩함.
     *
     * @param postId 조회할 게시글 ID
     * @return 삭제되지 않은 댓글 목록 (작성 순)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveByPostIdWithAuthor(@Param("postId") Long postId);

    /**
     * 삭제되지 않은 댓글을 author와 함께 단건 조회한다.
     *
     * @param id 조회할 댓글 ID
     * @return 댓글 Optional (삭제된 경우 empty)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findActiveByIdWithAuthor(@Param("id") Long id);

    /**
     * 특정 게시글의 활성 댓글 수를 반환한다.
     * 게시글 목록 조회 시 댓글 수 표시에 사용.
     *
     * @param postId 게시글 ID
     * @return 삭제되지 않은 댓글 수
     */
    long countByPostIdAndIsDeletedFalse(Long postId);
}
