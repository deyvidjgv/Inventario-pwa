package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.Jornada;

import java.time.Instant;

@Entity(tableName = "jornadas")
public class JornadaEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public Instant openedAt;
    public Instant closedAt;

    public JornadaEntity() {}

    @Ignore
    public JornadaEntity(Long id, Instant openedAt, Instant closedAt) {
        this.id = id;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public static JornadaEntity fromDomain(Jornada jornada) {
        return new JornadaEntity(jornada.getId(), jornada.getOpenedAt(), jornada.getClosedAt());
    }

    public Jornada toDomain() {
        return new Jornada(id, openedAt, closedAt);
    }
}
