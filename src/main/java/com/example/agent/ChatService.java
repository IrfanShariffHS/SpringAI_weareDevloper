
package com.example.agent;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful AI assistant for Unicorn Rentals,
            a fictional company that rents unicorns.

            Be friendly, helpful, and concise in your responses.

            Use the provided documents to answer questions when relevant.

            You have access to the following tools:
            1. DateTimeTools: For date and time information.
            2. WeatherTools: For weather information.
            3. MCP Tools: For accessing external services and capabilities.

            Use the appropriate tools whenever they are relevant
            to the user's request.

            If you do not have enough information, respond:
            "I don't know."

            Do not invent information.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            DataSource dataSource,
            VectorStore vectorStore,
            ToolCallbackProvider tools) {

        this.vectorStore = vectorStore;

        // 1. Configure PostgreSQL JDBC chat memory repository
        JdbcChatMemoryRepository chatMemoryRepository =
                JdbcChatMemoryRepository.builder()
                        .dataSource(dataSource)
                        .dialect(new PostgresChatMemoryRepositoryDialect())
                        .build();

        // 2. Configure conversation memory
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        // 3. Configure ChatClient with memory, RAG and tools
        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)

                // Conversation memory and RAG
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )

                // Register local tools and MCP tools
                .defaultTools(
                        new DateTimeTools(),
                        new WeatherTools(),
                        tools
                )

                .build();
    }

    /**
     * Send a prompt to Amazon Bedrock and stream the response.
     *
     * @param prompt   User question
     * @param username Conversation identifier
     * @return Streaming AI response
     */
    public Flux<String> chat(String prompt, String username) {

        if (prompt == null || prompt.isBlank()) {
            return Flux.error(
                    new IllegalArgumentException(
                            "Prompt cannot be empty"
                    )
            );
        }

        String conversationId =
                (username != null && !username.isBlank())
                        ? username
                        : "default";

        return chatClient.prompt()
                .user(prompt)
                .advisors(advisor ->
                        advisor.param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .stream()
                .content();
    }

    /**
     * Store document content in PGVector.
     *
     * @param content Document content
     */
    public void loadDocument(String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Document content cannot be empty"
            );
        }

        Document document = new Document(content);

        vectorStore.add(List.of(document));
    }
}
