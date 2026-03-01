package com.vector.vectorIA.controller;

import com.vector.vectorIA.dto.EvaluacionRequest;
import com.vector.vectorIA.dto.MantenimientoDTO;
import com.vector.vectorIA.dto.VehiculoDTO;
import com.vector.vectorIA.service.AnalisisIAProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vector")
public class VectorController {

    private final VectorStore vectorStore;
    private final AnalisisIAProvider analisisIAProvider;

    public VectorController(VectorStore vectorStore, AnalisisIAProvider analisisIAProvider) {
        this.analisisIAProvider = analisisIAProvider;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/insert")
    public void ingestData(@RequestBody EvaluacionRequest request) {
        // Texto enriquecido para que el modelo de Transformers encuentre relaciones semánticas
        String content = String.format(
                "Evaluación del curso %s. Alumno: %s. Nota: %.2f. Observaciones: %s",
                request.nombreCurso(), request.nombreAlumno(), request.nota(), request.observaciones()
        );

        Map<String, Object> metadata = Map.of(
                "student_id", request.idAlumno(),
                "curso_id", request.cursoId(),
                "curso_nombre", request.nombreCurso(),
                "alumno_nombre", request.nombreAlumno()
        );

        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
    }

    @PostMapping("/insertMantenimiento")
    public void ingestDataMantenimiento(@RequestBody VehiculoDTO request) {
        // Insertar TODOS los mantenimientos del vehículo, no solo el primero
        List<Document> documentos = request.mantenimientos().stream()
                .map(mantenimientoDTO -> {
                    String content = String.format(
                            "Mantenimiento del vehiculo %s. Patente: %s. Año: %d. Kilometraje: %d. Tipo de Mantenimiento: %s. Descripcion: %s. Costo Estimado: %.2f. Costo Final: %.2f. Estado: %s",
                            request.marca() + " " + request.modelo(),
                            request.patente(),
                            request.anio(),
                            request.kilometraje(),
                            mantenimientoDTO.tipoMantenimiento(),
                            mantenimientoDTO.descripcion(),
                            mantenimientoDTO.costoEstimado(),
                            mantenimientoDTO.costoFinal(),
                            mantenimientoDTO.estado()
                    );

                    Map<String, Object> metadata = Map.of(
                            "tipo_mantenimiento", mantenimientoDTO.tipoMantenimiento(),
                            "vehiculo_auto", request.marca() + " " + request.modelo(),
                            "patente", request.patente(),
                            "anio", request.anio(),
                            "kilometraje", request.kilometraje().toString(),
                            "costo_estimado", mantenimientoDTO.costoEstimado().toString(),
                            "costo_final", mantenimientoDTO.costoFinal().toString()
                    );

                    return new Document(content, metadata);
                })
                .toList();

        vectorStore.add(documentos);
    }

    @GetMapping("/consultar")
    public Flux<String> realizarConsultaSemantica(@RequestParam String pregunta) {
        return analisisIAProvider.analizarRendimiento(pregunta);
    }
}
