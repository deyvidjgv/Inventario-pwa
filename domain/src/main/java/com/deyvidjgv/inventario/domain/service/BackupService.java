package com.deyvidjgv.inventario.domain.service;

public interface BackupService {
    String exportJson();
    void importJson(String json);
}
