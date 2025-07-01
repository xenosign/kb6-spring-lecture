package org.example.kb6spring.repository.index;


import org.example.kb6spring.domain.index.Nightmare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NightmareRepository extends JpaRepository<Nightmare, Long> {}
