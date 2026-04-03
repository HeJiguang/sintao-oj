import { NextResponse } from "next/server";
import { requestJson, type ApiEnvelope, unwrapData } from "@aioj/api";

import { getAdminAccessToken } from "../../../../lib/server-auth";

export async function PUT(request: Request) {
  const token = await getAdminAccessToken();
  if (!token) {
    return NextResponse.json({ message: "管理员未登录。" }, { status: 401 });
  }

  const body = (await request.json().catch(() => ({}))) as { examId?: string; publish?: boolean };
  if (!body.examId) {
    return NextResponse.json({ message: "examId 不能为空。" }, { status: 400 });
  }

  const path = body.publish
    ? `/system/exam/publish?examId=${encodeURIComponent(body.examId)}`
    : `/system/exam/cancelPublish?examId=${encodeURIComponent(body.examId)}`;

  try {
    await requestJson<ApiEnvelope<null>>(path, {
      method: "PUT",
      token
    }).then(unwrapData);
  } catch (error) {
    return NextResponse.json(
      {
        message: error instanceof Error && error.message ? error.message : "考试状态更新失败。"
      },
      { status: 400 }
    );
  }

  return NextResponse.json({ ok: true });
}
