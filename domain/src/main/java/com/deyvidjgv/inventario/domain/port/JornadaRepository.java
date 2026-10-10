package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.Jornada;

import java.util.List;
import java.util.Optional;

public interface JornadaRepository {
    Jornada save(Jornada jornada);
    Optional<Jornada> findById(long id);
    Optional<Jornada> findOpen();
    Optional<Jornada> findLastClosed();
    List<Jornada> findAll();
    void deleteAll();
}
