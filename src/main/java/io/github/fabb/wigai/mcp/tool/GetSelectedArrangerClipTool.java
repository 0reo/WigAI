package io.github.fabb.wigai.mcp.tool;

import io.github.fabb.wigai.common.logging.StructuredLogger;
import io.github.fabb.wigai.features.ClipSceneController;
import io.github.fabb.wigai.mcp.McpErrorHandler;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP tool for introspecting the arranger (timeline) clip the user currently has selected.
 *
 * <p><b>API limitation:</b> Bitwig Extension API v19 exposes no per-track enumeration of
 * arranger clips and no per-track "has arranger content" flag. The only arranger-clip
 * handle is a single cursor that follows the user's current arranger selection. This tool
 * therefore reports the selected arranger clip's timeline range; it cannot survey tracks.
 * The read is non-mutating.
 */
public class GetSelectedArrangerClipTool {

    /**
     * Creates a "get_selected_arranger_clip" tool specification using the unified error handling system.
     *
     * @param clipSceneController The controller for clip and scene operations
     * @param logger The structured logger for logging operations
     * @return A SyncToolSpecification for the "get_selected_arranger_clip" tool
     */
    public static McpServerFeatures.SyncToolSpecification specification(
            ClipSceneController clipSceneController, StructuredLogger logger) {

        var schema = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": false
            }""";

        var tool = McpSchema.Tool.builder()
            .name("get_selected_arranger_clip")
            .description("Introspect the arranger (timeline) clip currently selected in Bitwig. Returns whether an arranger clip is selected and, if so, its timeline range (start, length, play_stop, loop info) and owning track. NOTE: Bitwig's API exposes no way to enumerate all arranger clips per track, so this reports only the user's currently-selected arranger clip.")
            .inputSchema(schema)
            .build();

        BiFunction<McpSyncServerExchange, CallToolRequest, McpSchema.CallToolResult> handler =
            (exchange, req) -> McpErrorHandler.executeWithValidation(
                "get_selected_arranger_clip",
                req.arguments(),
                logger,
                GetSelectedArrangerClipTool::validateParameters,
                (validatedParams) -> clipSceneController.getSelectedArrangerClip()
            );

        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler(handler)
            .build();
    }

    /**
     * Validates the parameters for the get_selected_arranger_clip tool.
     * This tool takes no parameters, so this simply returns an empty validated params object.
     *
     * @param arguments The raw arguments map
     * @param operation The operation name for error context
     * @return Validated parameters (empty for this tool)
     */
    private static ValidatedParams validateParameters(Map<String, Object> arguments, String operation) {
        return new ValidatedParams();
    }

    /**
     * Record to hold validated parameters for the get_selected_arranger_clip tool.
     * Empty since no parameters are required.
     */
    private record ValidatedParams() {}
}
