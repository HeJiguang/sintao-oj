import { ApiError } from "@aioj/api";
import { NextResponse } from "next/server";

export function toApiRouteErrorResponse(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiError) {
    if (error.code >= 400 && error.code < 600) {
      return NextResponse.json({ message: error.message || fallbackMessage }, { status: error.code });
    }
    if (error.code >= 1000) {
      return NextResponse.json({ message: error.message || fallbackMessage, code: error.code }, { status: 400 });
    }
    return NextResponse.json({ message: error.message || fallbackMessage }, { status: 502 });
  }

  if (error instanceof Error && error.message) {
    return NextResponse.json({ message: error.message }, { status: 500 });
  }

  return NextResponse.json({ message: fallbackMessage }, { status: 500 });
}
