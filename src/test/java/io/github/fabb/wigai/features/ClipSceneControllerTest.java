package io.github.fabb.wigai.features;

import io.github.fabb.wigai.bitwig.BitwigApiFacade;
import io.github.fabb.wigai.common.Logger;
import io.github.fabb.wigai.common.error.BitwigApiException;
import io.github.fabb.wigai.common.error.ErrorCode;
import io.github.fabb.wigai.features.ClipSceneController.ClipLaunchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClipSceneController.
 */
class ClipSceneControllerTest {

    @Mock
    private BitwigApiFacade bitwigApiFacade;

    @Mock
    private Logger logger;

    private ClipSceneController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ClipSceneController(bitwigApiFacade, logger);
    }

    @Test
    void testLaunchClip_Success() {
        // Arrange
        String trackName = "Drums";
        int clipIndex = 0;

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenReturn(0);
        when(bitwigApiFacade.getTrackClipCount(trackName)).thenReturn(8);
        doNothing().when(bitwigApiFacade).launchClip(trackName, clipIndex);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Clip at Drums[0] launched.", result.getMessage());
        assertNull(result.getErrorCode());

        verify(bitwigApiFacade).findTrackIndexByName(trackName);
        verify(bitwigApiFacade).getTrackClipCount(trackName);
        verify(bitwigApiFacade).launchClip(trackName, clipIndex);
    }

    @Test
    void testLaunchClip_TrackNotFound() {
        // Arrange
        String trackName = "NonExistentTrack";
        int clipIndex = 0;

        when(bitwigApiFacade.findTrackIndexByName(trackName))
            .thenThrow(new BitwigApiException(ErrorCode.TRACK_NOT_FOUND, "findTrackIndexByName", "Track '" + trackName + "' not found"));

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("TRACK_NOT_FOUND", result.getErrorCode());
        assertEquals("Track 'NonExistentTrack' not found", result.getMessage());

        verify(bitwigApiFacade).findTrackIndexByName(trackName);
        verify(bitwigApiFacade, never()).getTrackClipCount(anyString());
        verify(bitwigApiFacade, never()).launchClip(anyString(), anyInt());
    }

    @Test
    void testLaunchClip_ClipIndexOutOfBounds() {
        // Arrange
        String trackName = "Drums";
        int clipIndex = 10;

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenReturn(0);
        when(bitwigApiFacade.getTrackClipCount(trackName)).thenReturn(8);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("CLIP_INDEX_OUT_OF_BOUNDS", result.getErrorCode());
        assertEquals("Clip index 10 is out of bounds for track 'Drums'", result.getMessage());

        verify(bitwigApiFacade).findTrackIndexByName(trackName);
        verify(bitwigApiFacade).getTrackClipCount(trackName);
        verify(bitwigApiFacade, never()).launchClip(anyString(), anyInt());
    }

    @Test
    void testLaunchClip_BitwigLaunchFailed() {
        // Arrange
        String trackName = "Drums";
        int clipIndex = 0;

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenReturn(0);
        when(bitwigApiFacade.getTrackClipCount(trackName)).thenReturn(8);
        doThrow(new BitwigApiException(ErrorCode.BITWIG_API_ERROR, "launchClip", "Failed to launch clip"))
            .when(bitwigApiFacade).launchClip(trackName, clipIndex);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("BITWIG_ERROR", result.getErrorCode());
        assertTrue(result.getMessage().contains("Failed to launch clip"));

        verify(bitwigApiFacade).findTrackIndexByName(trackName);
        verify(bitwigApiFacade).getTrackClipCount(trackName);
        verify(bitwigApiFacade).launchClip(trackName, clipIndex);
    }

    @Test
    void testLaunchClip_ExceptionHandling() {
        // Arrange
        String trackName = "Drums";
        int clipIndex = 0;
        RuntimeException exception = new RuntimeException("Bitwig API error");

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenThrow(exception);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("BITWIG_ERROR", result.getErrorCode());
        assertTrue(result.getMessage().contains("Internal error occurred while launching clip"));

        verify(bitwigApiFacade).findTrackIndexByName(trackName);
    }

    @Test
    void testLaunchClip_EmptyTrackName() {
        // Arrange
        String trackName = "";
        int clipIndex = 0;

        when(bitwigApiFacade.findTrackIndexByName(trackName))
            .thenThrow(new BitwigApiException(ErrorCode.EMPTY_PARAMETER, "findTrackIndexByName", "trackName cannot be empty"));

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("BITWIG_ERROR", result.getErrorCode());
        assertTrue(result.getMessage().contains("trackName cannot be empty"));
    }

    @Test
    void testLaunchClip_NegativeClipIndex() {
        // This test verifies the controller can handle negative indices gracefully
        // The validation should be done at the tool level, but controller should handle it too
        String trackName = "Drums";
        int clipIndex = -1;

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenReturn(0);
        when(bitwigApiFacade.getTrackClipCount(trackName)).thenReturn(8);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("CLIP_INDEX_OUT_OF_BOUNDS", result.getErrorCode());
        assertTrue(result.getMessage().contains("out of bounds"));
    }

    @Test
    void testLaunchClip_ValidBoundaryClipIndex() {
        // Test launching the last valid clip slot
        String trackName = "Drums";
        int clipIndex = 7; // Last slot in an 8-slot bank

        when(bitwigApiFacade.findTrackIndexByName(trackName)).thenReturn(0);
        when(bitwigApiFacade.getTrackClipCount(trackName)).thenReturn(8);
        doNothing().when(bitwigApiFacade).launchClip(trackName, clipIndex);

        // Act
        ClipLaunchResult result = controller.launchClip(trackName, clipIndex);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Clip at Drums[7] launched.", result.getMessage());

        verify(bitwigApiFacade).launchClip(trackName, clipIndex);
    }

    @Test
    void testGetSelectedArrangerClip_ClipSelected() {
        // Arrange: facade reports a selected arranger clip
        java.util.Map<String, Object> facadeInfo = new java.util.LinkedHashMap<>();
        facadeInfo.put("has_selected_arranger_clip", true);
        facadeInfo.put("start", 4.0);
        facadeInfo.put("length", 8.0);
        facadeInfo.put("play_stop", 12.0);
        facadeInfo.put("loop_enabled", true);
        facadeInfo.put("loop_start", 4.0);
        facadeInfo.put("loop_length", 8.0);
        facadeInfo.put("track_name", "Bass");
        facadeInfo.put("track_index", 2);
        when(bitwigApiFacade.getSelectedArrangerClipInfo()).thenReturn(facadeInfo);

        // Act
        java.util.Map<String, Object> result = controller.getSelectedArrangerClip();

        // Assert: controller adds a distinct past-tense action event name, not the tool name
        assertEquals("arranger_clip_introspected", result.get("action"));
        assertEquals(true, result.get("has_selected_arranger_clip"));
        assertEquals(4.0, result.get("start"));
        assertEquals(8.0, result.get("length"));
        assertEquals(12.0, result.get("play_stop"));
        assertEquals("Bass", result.get("track_name"));
        assertEquals(2, result.get("track_index"));

        verify(bitwigApiFacade).getSelectedArrangerClipInfo();
    }

    @Test
    void testGetSelectedArrangerClip_NoClipSelected() {
        // Arrange: facade reports no selected arranger clip
        java.util.Map<String, Object> facadeInfo = new java.util.LinkedHashMap<>();
        facadeInfo.put("has_selected_arranger_clip", false);
        facadeInfo.put("start", null);
        facadeInfo.put("length", null);
        facadeInfo.put("play_stop", null);
        facadeInfo.put("loop_enabled", null);
        facadeInfo.put("loop_start", null);
        facadeInfo.put("loop_length", null);
        facadeInfo.put("track_name", null);
        facadeInfo.put("track_index", null);
        when(bitwigApiFacade.getSelectedArrangerClipInfo()).thenReturn(facadeInfo);

        // Act
        java.util.Map<String, Object> result = controller.getSelectedArrangerClip();

        // Assert
        assertEquals("arranger_clip_introspected", result.get("action"));
        assertEquals(false, result.get("has_selected_arranger_clip"));
        assertNull(result.get("start"));
        assertNull(result.get("track_name"));

        verify(bitwigApiFacade).getSelectedArrangerClipInfo();
    }

    @Test
    void testGetSelectedArrangerClip_WrapsUnexpectedError() {
        // Arrange: facade throws an unexpected (non-BitwigApiException) runtime error
        when(bitwigApiFacade.getSelectedArrangerClipInfo()).thenThrow(new RuntimeException("boom"));

        // Act & Assert
        BitwigApiException ex = assertThrows(BitwigApiException.class, () -> controller.getSelectedArrangerClip());
        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
    }

    @Test
    void testGetSelectedArrangerClip_PropagatesBitwigApiError() {
        // Arrange: facade reports a genuine Bitwig API failure (must NOT be masked as success)
        when(bitwigApiFacade.getSelectedArrangerClipInfo()).thenThrow(
            new BitwigApiException(ErrorCode.BITWIG_API_ERROR, "get_selected_arranger_clip",
                "Failed to read selected arranger clip: host error"));

        // Act & Assert: the controller surfaces the original error code unchanged
        BitwigApiException ex = assertThrows(BitwigApiException.class, () -> controller.getSelectedArrangerClip());
        assertEquals(ErrorCode.BITWIG_API_ERROR, ex.getErrorCode());
    }
}
