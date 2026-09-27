package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ImportNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImportNoteRepository extends JpaRepository<ImportNote, Integer> {
    List<ImportNote> findByShopShopIdOrderByCreatedAtDescImportIdDesc(Integer shopId);
}
