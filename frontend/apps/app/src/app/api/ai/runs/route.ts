import { NextResponse } from "next/server";

import type { AiRunCreateResponse } from "@aioj/api";
import { requestJson } from "@aioj/api";

import { resolveAgentApiBaseUrl } from "../../../../lib/agent-base-url";
import { resolveApiRouteError } from "../../../../lib/api-route-error";
import { getServerAccessToken } from "../../../../lib/server-auth";

export async function POST(request: Request) {
  try {
    const token = await getServerAccessToken();
    const body = await request.json();

    if (!body?.context?.userMessage?.trim()) {
      return NextResponse.json({ message: "AI prompt is required." }, { status: 400 });
    }

    const payload = await requestJson<AiRunCreateResponse>("/ai/runs", {
      method: "POST",
      token,
      baseUrl: resolveAgentApiBaseUrl(),
      body: JSON.stringify(body)
    });

    return NextResponse.json(payload);
  } catch (error) {
    const resolved = resolveApiRouteError(error, "Agent run failed.");
    return NextResponse.json(resolved.body, { status: resolved.status });
  }
}
