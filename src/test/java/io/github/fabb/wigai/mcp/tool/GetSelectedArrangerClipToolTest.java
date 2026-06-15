package io.github.fabb.wigai.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fabb.wigai.common.Logger;
import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.features.ClipSceneController;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSelectedArrangerClipToolTest {
    @Mock
    private ClipSceneController clipSceneController;
    @Mock
    private StructuredLogger structuredLogger;
    @Mock
    private Logger baseLogger;
    @Mock
    private StructuredLogger.TimedOperation timedOperation;
    @Mock
    private McpSyncServerExchange exchange;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(structuredLogger.getBaseLogger()).thenReturn(baseLogger);
        when(structuredLogger.generateOperationId()).thenReturn("op-123");
        when(structuredLogger.startTimedOperation(any(), any(), any())).thenReturn(timedOperation);
    }

    @Test
    void testSpecificationCreation() {
        McpServerFeatures.SyncToolSpecification spec =
            GetSelectedArrangerClipTool.specification(clipSceneController, structuredLogger);

        assertNotNull(spec);
        assertNotNull(spec.tool());
        assertEquals("get_selected_arranger_clip", spec.tool().name());
        assertNotNull(spec.tool().description());
        assertNotNull(spec.tool().inputSchema());
    }

    @Test
    void testDescriptionDocumentsApiLimitation() {
        McpServerFeatures.SyncToolSpecification spec =
            GetSelectedArrangerClipTool.specification(clipSceneController, structuredLogger);

        String description = spec.tool().description();
        // The description must make the per-track limitation explicit so callers are not misled.
        assertTrue(description.toLowerCase().contains("selected"),
            "Description should clarify it reports the selected arranger clip");
        assertTrue(description.toLowerCase().contains("arranger"),
            "Description should mention the arranger");
    }

    @Test
    void testHandlerDelegatesToControllerAndWrapsSuccess() throws Exception {
        // Arrange
        Map<String, Object> controllerResult = new LinkedHashMap<>();
        controllerResult.put("action", "arranger_clip_introspected");
        controllerResult.put("has_selected_arranger_clip", true);
        controllerResult.put("start", 4.0);
        controllerResult.put("length", 8.0);
        controllerResult.put("track_name", "Bass");
        controllerResult.put("track_index", 2);
        when(clipSceneController.getSelectedArrangerClip()).thenReturn(controllerResult);

        McpServerFeatures.SyncToolSpecification spec =
            GetSelectedArrangerClipTool.specification(clipSceneController, structuredLogger);

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
            .name("get_selected_arranger_clip")
            .arguments(Map.of())
            .build();

        // Act
        McpSchema.CallToolResult result = spec.callHandler().apply(exchange, request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());

        String responseJson = ((McpSchema.TextContent) result.content().get(0)).text();
        JsonNode response = objectMapper.readTree(responseJson);

        assertEquals("success", response.get("status").asText());
        JsonNode data = response.get("data");
        assertEquals("arranger_clip_introspected", data.get("action").asText());
        assertEquals(true, data.get("has_selected_arranger_clip").asBoolean());
        assertEquals(4.0, data.get("start").asDouble());
        assertEquals(8.0, data.get("length").asDouble());
        assertEquals("Bass", data.get("track_name").asText());
        assertEquals(2, data.get("track_index").asInt());

        verify(clipSceneController).getSelectedArrangerClip();
    }
}
