package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ImportNoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportNoteDetailRepository extends JpaRepository<ImportNoteDetail,Integer> {
    List<ImportNoteDetail> findByImportNote_ImportId(Integer importId);
}
