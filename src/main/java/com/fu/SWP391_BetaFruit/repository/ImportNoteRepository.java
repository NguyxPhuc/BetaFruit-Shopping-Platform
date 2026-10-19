package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ImportNote;
import com.fu.SWP391_BetaFruit.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportNoteRepository extends JpaRepository<ImportNote,Integer> {
    List<ImportNote> findAllByShopOrderByCreatedAtDesc(Shop shop);
}
