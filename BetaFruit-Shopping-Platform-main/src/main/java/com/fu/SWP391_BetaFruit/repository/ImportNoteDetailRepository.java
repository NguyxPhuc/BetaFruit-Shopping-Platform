package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ImportNoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface ImportNoteDetailRepository extends JpaRepository<ImportNoteDetail, Integer> {
    @EntityGraph(attributePaths = "variant.product")
    List<ImportNoteDetail> findByImportNoteImportIdOrderByDetailIdAsc(Integer importId);
}
