import { NextResponse } from "next/server";

import type { AiArtifact } from "@aioj/api";
import { requestJson } from "@aioj/api";

import { resolveAgentApiBaseUrl } from "../../../../../../lib/agent-base-url";
import { resolveApiRouteError } from "../../../../../../lib/api-route-error";
import { getServerAccessToken } from "../../../../../../lib/server-auth";

type RouteProps = {
  params: Promise<{ runId: string }>;
};

export async function GET(_request: Request, { params }: RouteProps) {
  try {
    const token = await getServerAccessToken();
    const { runId } = await params;

    const payload = await requestJson<AiArtifact[]>(`/ai/runs/${encodeURIComponent(runId)}/artifacts`, {
      token,
      baseUrl: resolveAgentApiBaseUrl()
    });

    return NextResponse.json(payload);
  } catch (error) {
    const resolved = resolveApiRouteError(error, "Failed to load run artifacts.");
    return NextResponse.json(resolved.body, { status: resolved.status });
  }
}
