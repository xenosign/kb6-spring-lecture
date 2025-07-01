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
    Optional<IndexTest> findByEmail(String email);
    List<IndexTest> findByUsername(String username);

    List<IndexTest> findByEmailStartingWith(String email);
    List<IndexTest> findByEmailContaining(String email);
    List<IndexTest> findByUsernameContaining(String username);

    @Query("SELECT i FROM IndexTest i WHERE i.email = :email AND i.username = :username")
    Optional<IndexTest> findByEmailAndUsername(
            @Param("email") String email, @Param("username") String username);
}