package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.JornadaDao;
import com.deyvidjgv.inventario.data.local.entity.JornadaEntity;
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.domain.port.JornadaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomJornadaRepository implements JornadaRepository {
    private final JornadaDao jornadaDao;

    public RoomJornadaRepository(JornadaDao jornadaDao) {
        this.jornadaDao = jornadaDao;
    }

    @Override
    public Jornada save(Jornada jornada) {
        JornadaEntity entity = JornadaEntity.fromDomain(jornada);
        if (jornada.getId() != null) {
            jornadaDao.update(entity);
            return jornada;
        } else {
            long id = jornadaDao.insert(entity);
            return jornada.withId(id);
        }
    }

    @Override
    public Optional<Jornada> findById(long id) {
        JornadaEntity entity = jornadaDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Jornada> findOpen() {
        JornadaEntity entity = jornadaDao.findOpen();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Jornada> findLastClosed() {
        JornadaEntity entity = jornadaDao.findLastClosed();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Jornada> findAll() {
        List<JornadaEntity> entities = jornadaDao.findAll();
        List<Jornada> list = new ArrayList<>();
        for (JornadaEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        jornadaDao.deleteAll();
    }
}
