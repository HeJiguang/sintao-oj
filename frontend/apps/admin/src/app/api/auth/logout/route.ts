import { NextResponse } from "next/server";
import { requestJson, type ApiEnvelope } from "@aioj/api";

import { ADMIN_ACCESS_TOKEN_COOKIE } from "../../../../lib/server-auth";

export async function POST(request: Request) {
  const cookieHeader = request.headers.get("cookie") ?? "";
  const tokenMatch = cookieHeader.match(/syncode_admin_access_token=([^;]+)/);
  const token = tokenMatch?.[1];

  if (token) {
    try {
      await requestJson<ApiEnvelope<null>>("/system/sysUser/logout", {
        method: "DELETE",
        token
      });
    } catch {
      // Ignore backend logout failures and clear the local session anyway.
    }
  }

  const response = NextResponse.redirect(new URL("/login", request.url));
  response.cookies.set(ADMIN_ACCESS_TOKEN_COOKIE, "", {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    maxAge: 0
  });
  return response;
}
