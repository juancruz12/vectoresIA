package com.vector.vectorIA.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalisisIAProvider {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public AnalisisIAProvider(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    /**
     * Analiza mantenimientos de vehículos usando búsqueda semántica y IA.
     *
     * @param consultaUsuario Pregunta del usuario sobre mantenimientos
     * @return Stream de respuestas procesadas por la IA
     */
    public Flux<String> analizarRendimiento(String consultaUsuario) {

        // Paso 1: Identificar si la consulta es GLOBAL o busca un tipo específico de mantenimiento
        String promptExtraccion = """
        Analiza esta pregunta sobre mantenimientos de vehículos. Identifica si:
        - Pregunta por un tipo específico (ej: "cambio de aceite", "rotación de llantas", "revisión de frenos")
        - Pregunta por un vehículo específico (ej: "Honda", "Toyota", "patente ABC123")
        - Pregunta general sobre todos los mantenimientos
        
        RESPONDE EN FORMATO:
        TIPO_MANTENIMIENTO: [el tipo específico o GENERAL]
        VEHICULO: [marca/modelo/patente o GLOBAL]
        
        PREGUNTA: """ + consultaUsuario;

        String decision = chatModel.call(promptExtraccion).trim();

        // Paso 2: Construir búsqueda semántica con filtros inteligentes
        var requestBuilder = SearchRequest.builder()
                .query(consultaUsuario)
                .topK(100)  // Obtener más documentos para análisis exhaustivo
                .similarityThreshold(0.2);  // Umbral más bajo para capturar contexto relacionado

        // Aplicar filtros basados en la decisión (opcional - por ahora busca todo)
        // Si se necesita filtrado específico, se puede implementar con metadata de los documentos
        // Por ahora, la búsqueda semántica es suficiente para encontrar documentos relevantes

        // Paso 3: Obtener documentos relevantes
        List<Document> documentosRelevantes = vectorStore.similaritySearch(requestBuilder.build());

        // Paso 4: Enriquecer contexto con análisis preliminar
        String contextoRaw = documentosRelevantes.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n---\n"));

        String contextoEnriquecido = enriquecerContexto(contextoRaw, documentosRelevantes);

        // Paso 5: Crear prompt avanzado para análisis de mantenimientos
        String mensajeSistema = """
            Eres un ANALISTA EXPERTO EN MANTENIMIENTO DE FLOTA VEHICULAR para KAVAK.
            Tu objetivo es proporcionar análisis detallados, precisos y accionables sobre los mantenimientos de la flota.
            
            ╔════════════════════════════════════════════════════════════════╗
            ║ INSTRUCCIONES CRÍTICAS:                                       ║
            ║ 1. Analiza TODOS los datos disponibles en el contexto        ║
            ║ 2. Identifica patrones, tendencias y anomalías               ║
            ║ 3. Compara costos estimados vs. costos finales              ║
            ║ 4. Proporciona recomendaciones accionables                   ║
            ║ 5. Si no hay suficientes datos, indícalo claramente         ║
            ╚════════════════════════════════════════════════════════════════╝
            
            INFORMACIÓN DE CONTEXTO:
            {contexto}
            
            TIPOS DE ANÁLISIS ESPERADOS:
            • Frecuencia y distribución de mantenimientos
            • Análisis de costos (variaciones, promedios, outliers)
            • Salud general de la flota por tipo de vehículo
            • Identificación de problemas recurrentes
            • Comparativas entre vehículos
            • Predicciones de mantenimiento próximo
            
            PREGUNTA DEL USUARIO:
            {pregunta}
            
            RESPUESTA DETALLADA Y ANALÍTICA:
            """;

        PromptTemplate promptTemplate = new PromptTemplate(mensajeSistema);

        Prompt prompt = promptTemplate.create(Map.of(
                "contexto", contextoEnriquecido.isEmpty() ? "No hay datos de mantenimiento disponibles" : contextoEnriquecido,
                "pregunta", consultaUsuario
        ));

        return chatModel.stream(prompt)
                .map(response -> {
                    String content = response.getResult().getOutput().getText();
                    return content != null ? content : "";
                });
    }


    /**
     * Enriquece el contexto con análisis estadístico preliminar
     */
    private String enriquecerContexto(String contextoRaw, List<Document> documentos) {
        if (documentos.isEmpty()) {
            return "SIN DATOS DISPONIBLES";
        }

        // Calcular estadísticas básicas
        int totalMantenimientos = documentos.size();

        double costoPromedio = documentos.stream()
                .mapToDouble(doc -> extraerCosto(doc.getFormattedContent()))
                .filter(c -> c > 0)
                .average()
                .orElse(0.0);

        StringBuilder enriquecido = new StringBuilder();
        enriquecido.append("═══ RESUMEN ANALÍTICO ═══\n");
        enriquecido.append(String.format("Total de registros: %d\n", totalMantenimientos));
        enriquecido.append(String.format("Costo promedio estimado: $%.2f\n", costoPromedio));
        enriquecido.append("\n═══ DATOS DETALLADOS ═══\n");
        enriquecido.append(contextoRaw);

        return enriquecido.toString();
    }

    /**
     * Extrae el costo estimado de un documento de mantenimiento
     */
    private double extraerCosto(String contenido) {
        try {
            int inicio = contenido.indexOf("Costo Estimado:");
            if (inicio == -1) return 0.0;

            String substring = contenido.substring(inicio + 15);
            String[] partes = substring.split("[^0-9.]");
            return Double.parseDouble(partes[1]);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
