package com.vector.vectorIA.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

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

    public String analizarRendimiento(String consultaUsuario) {
        // 1. Búsqueda (Se mantiene igual)
        List<Document> documentosRelevantes = vectorStore.similaritySearch(
                SearchRequest.builder().query(consultaUsuario).topK(10).build()
        );

        String contexto = documentosRelevantes.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n---\n"));

        // 2. Prompt Template MEJORADO
        // Añadimos la variable {pregunta} explícitamente en el cuerpo
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

        // IMPORTANTE: Asegúrate de pasar tanto el contexto como la pregunta al mapa
        Prompt prompt = promptTemplate.create(Map.of(
                "contexto", contexto,
                "pregunta", consultaUsuario
        ));

        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
