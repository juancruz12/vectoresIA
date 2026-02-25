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

    public Flux<String> analizarRendimiento(String consultaUsuario) {

        List<String> cursosExistentes = List.of("Matematicas Avanzadas", "Programación en Java");

        String promptExtraccion = String.format("""
        Analiza la pregunta del usuario y los cursos disponibles: %s
        
        REGLA:
        - Si pregunta por un curso específico, responde SOLO el nombre exacto del curso.
        - Si pregunta por 'todos' o 'en general', responde 'GLOBAL'.
        - Si no detectas nada, responde 'NONE'.
        
        PREGUNTA: %s
        """, cursosExistentes, consultaUsuario);

        String decision = chatModel.call(promptExtraccion).trim();

        var requestBuilder = SearchRequest.builder()
                .query(consultaUsuario);

        if (!decision.equalsIgnoreCase("GLOBAL") && !decision.equalsIgnoreCase("NONE")) {
            var filterBuilder = new FilterExpressionBuilder();
            requestBuilder.filterExpression(filterBuilder.eq("curso_nombre", decision).build());
            requestBuilder.topK(50)
            .similarityThreshold(0.5);
        } else {
            requestBuilder.topK(100)
            .similarityThreshold(0.0);
        }

        List<Document> documentosRelevantes = vectorStore.similaritySearch(requestBuilder.build());

        String contexto = documentosRelevantes.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n---\n"));

        String mensajeSistema = """
            Eres un Asistente de Análisis Académico experto. 
            Tu objetivo es analizar las evaluaciones proporcionadas y responder a la consulta del usuario de forma directa y analítica.
            
            REGLAS CRÍTICAS:
            1. Si el usuario pregunta "quién necesita ayuda", identifica a los alumnos con notas bajas (menores a 6) o comentarios negativos.
            2. No digas "no has planteado una duda", utiliza la PREGUNTA DEL USUARIO para filtrar el CONTEXTO.
            3. Si no hay datos sobre el curso mencionado, indícalo.
            
            CONTEXTO DE EVALUACIONES:
            {contexto}
            
            PREGUNTA DEL USUARIO:
            {pregunta}
            
            RESPUESTA ANALÍTICA:
            """;

        PromptTemplate promptTemplate = new PromptTemplate(mensajeSistema);

        Prompt prompt = promptTemplate.create(Map.of(
                "contexto", contexto,
                "pregunta", consultaUsuario
        ));

        return chatModel.stream(prompt)
                .map(response -> {
                    String content = response.getResult().getOutput().getText();
                    return content != null ? content : "";
                });
    }
}
