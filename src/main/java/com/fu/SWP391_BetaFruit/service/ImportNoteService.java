package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ImportNoteRequest;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.ImportNoteResponse;

import java.util.List;

public interface ImportNoteService {
    void createImportNote(Integer ownerId, ImportNoteRequest importNoteRequest) throws Exception;

    List<ImportNoteResponse> getAllImportNote(Integer ownerId) throws Exception;

    ImportNoteDetailResponse getImportNoteDetail(Integer ownerId, Integer importId) throws Exception;
}
