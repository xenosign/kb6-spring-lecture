package org.example.kb6spring.repository.stock;

import org.example.kb6spring.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Integer> {}
