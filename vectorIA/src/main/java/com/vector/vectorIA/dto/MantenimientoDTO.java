package com.vector.vectorIA.dto;

import java.time.LocalDateTime;

public record MantenimientoDTO(
        Long id,
        String tipoMantenimiento,
        String descripcion,
        Long kilometrajeEnMantenimiento,
        String estado,
        Double costoEstimado,
        Double costoFinal,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {

}
