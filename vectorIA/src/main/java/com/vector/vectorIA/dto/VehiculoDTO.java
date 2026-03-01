package com.vector.vectorIA.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public record VehiculoDTO(
        Long id,
        String patente,
        String marca,
        String modelo,
        Integer anio,
        Long kilometraje,
        Boolean disponible,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        List<MantenimientoDTO> mantenimientos
) {

}
