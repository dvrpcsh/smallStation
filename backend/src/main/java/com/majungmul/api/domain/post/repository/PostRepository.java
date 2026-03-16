package com.majungmul.api.domain.post.repository;

import com.majungmul.api.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글(Post) 데이터 접근 레포지토리.
 *
 * <p>논리 삭제된 게시글({@code is_deleted = true})은 모든 조회에서 자동 제외된다.
 * 모든 페이징 조회는 최신순({@code created_at DESC}) 정렬이 기본값 — Pageable에서 지정.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 삭제되지 않은 게시글 목록을 페이징 조회한다.
     *
     * <p>N+1 방지: author(User) 정보를 fetch join으로 함께 조회.
     * 게시글 목록에서 닉네임 표시를 위해 author를 즉시 로딩함.
     *
     * @param pageable 페이지 번호, 크기, 정렬 정보
     * @return 페이징된 게시글 목록
     */
    /**
     * countQuery 분리 이유: JOIN FETCH는 COUNT 쿼리에서 허용되지 않아 런타임 오류가 발생한다.
     * Spring Data JPA가 countQuery를 자동 파생하면 FETCH가 포함되어 예외가 생기므로
     * 반드시 별도의 countQuery를 지정한다.
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.isDeleted = false",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.isDeleted = false")
    Page<Post> findAllActiveWithAuthor(Pageable pageable);

    /**
     * 삭제되지 않은 특정 게시글을 author와 함께 조회한다.
     *
     * <p>상세 조회 전용: 단건 조회이므로 fetch join으로 author를 함께 가져옴.
     *
     * @param id 조회할 게시글 ID
     * @return 게시글 Optional (삭제된 게시글이면 empty)
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id AND p.isDeleted = false")
    Optional<Post> findActiveByIdWithAuthor(@Param("id") Long id);

    /**
     * 특정 게시글이 존재하고 삭제되지 않았는지 확인한다.
     * 댓글 작성 시 게시글 유효성 검사에 사용.
     *
     * @param id 확인할 게시글 ID
     * @return 유효한 게시글이면 true
     */
    boolean existsByIdAndIsDeletedFalse(Long id);
}
