package com.fu.SWP391_BetaFruit.utils;

public class ImportNoteUtils {
    public static String generateImportCode(Integer importId) {
        if(importId == null){
            return "PNK-DRAFT";
        }
        return "PNK-" + String.format("%03d", importId);
    }
}
