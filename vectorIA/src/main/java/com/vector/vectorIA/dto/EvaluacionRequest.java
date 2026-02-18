package com.vector.vectorIA.dto;

public record EvaluacionRequest(
        Long idAlumno,
        String nombreAlumno,
        Double nota,
        String observaciones,
        Long cursoId,
        String nombreCurso
) {

}
