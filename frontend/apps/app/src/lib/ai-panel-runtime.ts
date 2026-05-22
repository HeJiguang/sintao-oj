import type { AiArtifact, AiRunEvent } from "@aioj/api";

export type TimelineEntry =
  | { id: string; kind: "prompt"; content: string }
  | { id: string; kind: "artifact"; artifact: AiArtifact };

export type AiPanelPhase = "idle" | "submitting" | "streaming" | "loading_artifacts" | "done" | "failed";

export type AiPanelRuntimeState = {
  phase: AiPanelPhase;
  entries: TimelineEntry[];
  latestEvents: AiRunEvent[];
  streamingAnswer: string;
  runStatus: string;
  activeRunId: string | null;
  errorMessage: string | null;
};

export type AiPanelRuntimeAction =
  | { type: "prompt_submitted"; prompt: string }
  | { type: "run_created"; runId: string; runStatus: string }
  | { type: "event_received"; event: AiRunEvent }
  | { type: "artifact_loading_started" }
  | { type: "artifacts_loaded"; runId: string; artifacts: AiArtifact[] }
  | { type: "run_failed"; message: string; artifact?: AiArtifact };

export function createAiPanelRuntimeState(initialArtifacts: AiArtifact[] = []): AiPanelRuntimeState {
  return {
    phase: "idle",
    entries: toTimelineEntries(initialArtifacts),
    latestEvents: [],
    streamingAnswer: "",
    runStatus: "READY",
    activeRunId: null,
    errorMessage: null
  };
}

export function reduceAiPanelRuntimeState(
  state: AiPanelRuntimeState,
  action: AiPanelRuntimeAction
): AiPanelRuntimeState {
  switch (action.type) {
    case "prompt_submitted":
      return {
        ...state,
        phase: "submitting",
        entries: [...state.entries, { id: `prompt-${Date.now()}`, kind: "prompt", content: action.prompt }],
        latestEvents: [],
        streamingAnswer: "",
        runStatus: "RUNNING",
        activeRunId: null,
        errorMessage: null
      };
    case "run_created":
      return {
        ...state,
        phase: "streaming",
        activeRunId: action.runId,
        runStatus: action.runStatus,
        latestEvents: [],
        streamingAnswer: "",
        errorMessage: null
      };
    case "event_received": {
      const delta = action.event.eventType === "message.delta" && typeof action.event.payload.delta === "string"
        ? action.event.payload.delta
        : "";
      return {
        ...state,
        phase: "streaming",
        latestEvents: [...state.latestEvents, action.event],
        streamingAnswer: delta ? state.streamingAnswer + delta : state.streamingAnswer
      };
    }
    case "artifact_loading_started":
      return {
        ...state,
        phase: "loading_artifacts"
      };
    case "artifacts_loaded":
      return {
        ...state,
        phase: "done",
        activeRunId: action.runId,
        entries: mergeArtifacts(state.entries, action.artifacts),
        streamingAnswer: "",
        errorMessage: null
      };
    case "run_failed":
      return {
        ...state,
        phase: "failed",
        runStatus: "FAILED",
        latestEvents: [],
        streamingAnswer: "",
        errorMessage: action.message,
        entries: action.artifact ? mergeArtifacts(state.entries, [action.artifact]) : state.entries
      };
    default:
      return state;
  }
}

function toTimelineEntries(artifacts: AiArtifact[]): TimelineEntry[] {
  return artifacts.map(
    (artifact): TimelineEntry => ({
      id: artifact.artifactId,
      kind: "artifact",
      artifact
    })
  );
}

function mergeArtifacts(current: TimelineEntry[], artifacts: AiArtifact[]): TimelineEntry[] {
  const knownIds = new Set(
    current.filter((item) => item.kind === "artifact").map((item) => item.artifact.artifactId)
  );
  const nextEntries = artifacts
    .filter((artifact) => !knownIds.has(artifact.artifactId))
    .map(
      (artifact): TimelineEntry => ({
        id: artifact.artifactId,
        kind: "artifact",
        artifact
      })
    );
  return [...current, ...nextEntries];
}
