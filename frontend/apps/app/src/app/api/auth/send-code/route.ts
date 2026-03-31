import { NextResponse } from "next/server";

import { requestJson, unwrapData } from "@aioj/api";
import { resolveApiRouteError } from "../../../../lib/api-route-error";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const payload = await requestJson<{ code: number; msg: string; data: null }>("/friend/user/sendCode", {
      method: "POST",
      body: JSON.stringify(body)
    });

    unwrapData(payload);
    return NextResponse.json({ ok: true });
  } catch (error) {
    const { status, body } = resolveApiRouteError(error, "\u9a8c\u8bc1\u7801\u53d1\u9001\u5931\u8d25\u3002");
    return NextResponse.json(body, { status });
  }
}
