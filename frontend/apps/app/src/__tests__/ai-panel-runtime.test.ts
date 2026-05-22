import assert from "node:assert/strict";

import type { AiArtifact, AiRunEvent } from "@aioj/api";

import {
  createAiPanelRuntimeState,
  reduceAiPanelRuntimeState
} from "../lib/ai-panel-runtime";

function buildArtifact(overrides: Partial<AiArtifact> = {}): AiArtifact {
  return {
    artifactId: "art-1",
    runId: "run-1",
    artifactType: "answer_card",
    title: "Answer",
    summary: "Summary",
    body: {
      answer: "Final answer",
      nextAction: "Keep going."
    },
    renderHint: "markdown",
    version: 1,
    createdAt: "2026-04-11T00:00:00Z",
    ...overrides
  };
}

function buildEvent(overrides: Partial<AiRunEvent> = {}): AiRunEvent {
  return {
    eventId: "evt-1",
    runId: "run-1",
    seq: 1,
    eventType: "run.accepted",
    level: "INFO",
    timestamp: "2026-04-11T00:00:00Z",
    payload: {},
    ...overrides
  };
}

async function main() {
  const seeded = buildArtifact({
    artifactId: "art-seeded",
    title: "Seeded",
    body: { answer: "Existing answer", nextAction: "Stay focused." }
  });

  const initial = createAiPanelRuntimeState([seeded]);
  assert.equal(initial.phase, "idle");
  assert.equal(initial.entries.length, 1);
  assert.equal(initial.entries[0]?.kind, "artifact");

  const submitting = reduceAiPanelRuntimeState(initial, {
    type: "prompt_submitted",
    prompt: "Give me a hint."
  });
  assert.equal(submitting.phase, "submitting");
  assert.equal(submitting.entries.at(-1)?.kind, "prompt");
  if (submitting.entries.at(-1)?.kind !== "prompt") {
    throw new Error("Prompt entry missing.");
  }
  assert.equal(submitting.entries.at(-1)?.content, "Give me a hint.");

  const running = reduceAiPanelRuntimeState(submitting, {
    type: "run_created",
    runId: "run-2",
    runStatus: "ACCEPTED"
  });
  assert.equal(running.phase, "streaming");
  assert.equal(running.activeRunId, "run-2");
  assert.equal(running.runStatus, "ACCEPTED");

  const streaming = reduceAiPanelRuntimeState(running, {
    type: "event_received",
    event: buildEvent({
      runId: "run-2",
      eventType: "message.delta",
      payload: { delta: "Hash map first. " }
    })
  });
  assert.equal(streaming.phase, "streaming");
  assert.equal(streaming.streamingAnswer, "Hash map first. ");
  assert.equal(streaming.latestEvents.length, 1);

  const progress = reduceAiPanelRuntimeState(streaming, {
    type: "event_received",
    event: buildEvent({
      eventId: "evt-2",
      runId: "run-2",
      seq: 2,
      eventType: "graph.node_completed",
      payload: { node: "planner_node" }
    })
  });
  assert.equal(progress.latestEvents.length, 2);

  const loadingArtifacts = reduceAiPanelRuntimeState(progress, {
    type: "artifact_loading_started"
  });
  assert.equal(loadingArtifacts.phase, "loading_artifacts");

  const done = reduceAiPanelRuntimeState(loadingArtifacts, {
    type: "artifacts_loaded",
    runId: "run-2",
    artifacts: [
      buildArtifact({
        artifactId: "art-final",
        runId: "run-2",
        title: "Final diagnosis"
      })
    ]
  });
  assert.equal(done.phase, "done");
  assert.equal(done.streamingAnswer, "");
  assert.equal(done.entries.at(-1)?.kind, "artifact");
  if (done.entries.at(-1)?.kind !== "artifact") {
    throw new Error("Artifact entry missing.");
  }
  assert.equal(done.entries.at(-1)?.artifact.title, "Final diagnosis");

  const failed = reduceAiPanelRuntimeState(done, {
    type: "run_failed",
    message: "Agent run failed."
  });
  assert.equal(failed.phase, "failed");
  assert.equal(failed.errorMessage, "Agent run failed.");
}

void main();
