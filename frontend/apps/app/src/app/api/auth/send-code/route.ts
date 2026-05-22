import { NextResponse } from "next/server";

import { requestJson, unwrapData } from "@aioj/api";

import { toApiRouteErrorResponse } from "../../../../lib/api-route-error";

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
    return toApiRouteErrorResponse(error, "验证码发送失败，请稍后重试。");
  }
}
