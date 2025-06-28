package org.example.kb6spring.repository.index;

import org.example.kb6spring.domain.index.IndexTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndexTestRepository extends JpaRepository<IndexTest, Long> {
    // 이메일로 조회 (인덱스 사용)
    Optional<IndexTest> findByEmail(String email);

    // 사용자명으로 조회 (인덱스 미사용)
    List<IndexTest> findByUsername(String username);

    // 사용자명 LIKE 검색 (인덱스 미사용)
    List<IndexTest> findByUsernameContaining(String username);

    // 이메일 LIKE 검색 (인덱스 부분 사용)
    List<IndexTest> findByEmailContaining(String email);

    // 생성일 범위 조회 (인덱스 미사용)
    List<IndexTest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 복합 조건 (이메일 + 사용자명)
    @Query("SELECT i FROM IndexTest i WHERE i.email = :email AND i.username = :username")
    Optional<IndexTest> findByEmailAndUsername(@Param("email") String email, @Param("username") String username);

    // 페이징 처리된 이메일 조회
    Page<IndexTest> findByEmailStartingWith(String emailPrefix, Pageable pageable);

    // 네이티브 쿼리로 인덱스 힌트 사용
    @Query(value = "SELECT * FROM index_test USE INDEX (idx_index_test_email) WHERE email = :email", nativeQuery = true)
    Optional<IndexTest> findByEmailWithIndexHint(@Param("email") String email);

    // 전체 카운트 (성능 측정용)
    @Query("SELECT COUNT(i) FROM IndexTest i")
    long getTotalCount();

    // 이메일 도메인별 조회
    @Query("SELECT i FROM IndexTest i WHERE i.email LIKE CONCAT('%@', :domain)")
    List<IndexTest> findByEmailDomain(@Param("domain") String domain);
}