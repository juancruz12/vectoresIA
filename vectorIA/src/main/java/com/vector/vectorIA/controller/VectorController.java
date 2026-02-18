package com.vector.vectorIA.controller;

import com.vector.vectorIA.dto.EvaluacionRequest;
import com.vector.vectorIA.service.AnalisisIAProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

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

        // Metadatos para filtrado técnico (SQL puro bajo cuerda)
        Map<String, Object> metadata = Map.of(
                "student_id", request.idAlumno(),
                "curso_id", request.cursoId(),
                "curso_nombre", request.nombreCurso(),
                "alumno_nombre", request.nombreAlumno()
        );

        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
    }

    @GetMapping("/consultar")
    public String realizarConsultaSemantica(@RequestParam String pregunta) {
        return analisisIAProvider.analizarRendimiento(pregunta);
    }
}
