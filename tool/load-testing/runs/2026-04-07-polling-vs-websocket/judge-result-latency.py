import argparse
import asyncio
import json
from pathlib import Path
from statistics import mean
from time import time
from urllib.parse import quote

import aiohttp

IN_JUDGE_PASS = 3


def now_ms() -> int:
    return int(time() * 1000)


def iso_from_ms(value: int) -> str:
    return __import__("datetime").datetime.fromtimestamp(value / 1000, tz=__import__("datetime").timezone.utc).isoformat()


def average(values: list[float]) -> float | None:
    if not values:
        return None
    return round(mean(values), 2)


def percentile(values: list[float], p: int) -> float | None:
    if not values:
        return None
    sorted_values = sorted(values)
    rank = max(0, min(len(sorted_values) - 1, int(__import__("math").ceil((p / 100) * len(sorted_values)) - 1)))
    return round(sorted_values[rank], 2)


def as_bearer_token(token: str) -> str:
    return token if token.startswith("Bearer ") else f"Bearer {token}"


def build_ws_url(base_url: str) -> str:
    trimmed = base_url.rstrip("/")
    if trimmed.startswith("http://"):
        return trimmed.replace("http://", "ws://", 1) + "/friend/ws/judge/result"
    if trimmed.startswith("https://"):
        return trimmed.replace("https://", "wss://", 1) + "/friend/ws/judge/result"
    raise ValueError(f"Unsupported base URL: {base_url}")


async def request_json(session: aiohttp.ClientSession, method: str, url: str, **kwargs) -> tuple[aiohttp.ClientResponse, dict, str]:
    async with session.request(method, url, **kwargs) as response:
        text = await response.text()
        payload = {}
        if text:
            try:
                payload = json.loads(text)
            except json.JSONDecodeError as exc:
                raise RuntimeError(f"Failed to parse JSON from {url}: {exc}. Raw body: {text}") from exc
        return response, payload, text


def unwrap_envelope(url: str, response: aiohttp.ClientResponse, payload: dict, raw_text: str) -> dict:
    if response.status != 200:
        raise RuntimeError(f"HTTP {response.status} from {url}: {raw_text}")
    if payload.get("code") != 1000:
        raise RuntimeError(f"Business failure from {url}: {raw_text}")
    return payload.get("data") or {}


async def submit_async_judge(session: aiohttp.ClientSession, config: argparse.Namespace) -> tuple[str, int]:
    submit_url = f"{config.base_url.rstrip('/')}/friend/user/question/rabbit/submit"
    body = {
        "questionId": int(config.question_id),
        "programType": int(config.program_type),
        "userCode": config.user_code,
    }
    if config.exam_id:
        body["examId"] = int(config.exam_id)

    response, payload, raw_text = await request_json(
        session,
        "POST",
        submit_url,
        headers={"Authorization": as_bearer_token(config.token), "Content-Type": "application/json"},
        data=json.dumps(body),
    )
    data = unwrap_envelope(submit_url, response, payload, raw_text)
    request_id = str(data.get("requestId", "")).strip()
    if not request_id:
        raise RuntimeError(f"Submit response does not contain requestId: {raw_text}")
    return request_id, now_ms()


async def wait_for_poll_result(session: aiohttp.ClientSession, config: argparse.Namespace, request_id: str, accepted_at: int) -> dict:
    params = {"questionId": str(config.question_id), "requestId": request_id}
    if config.exam_id:
        params["examId"] = str(config.exam_id)
    url = f"{config.base_url.rstrip('/')}/friend/user/question/exe/result"

    poll_requests = 0
    for attempt in range(config.poll_max_attempts):
        poll_requests += 1
        response, payload, raw_text = await request_json(
            session,
            "GET",
            url,
            headers={"Authorization": as_bearer_token(config.token)},
            params=params,
        )
        data = unwrap_envelope(url, response, payload, raw_text)
        pass_value = data.get("pass")
        if pass_value != IN_JUDGE_PASS:
            finished_at = now_ms()
            return {
                "ok": True,
                "mode": "poll",
                "pass": pass_value,
                "pollRequests": poll_requests,
                "finishedAt": finished_at,
                "latencyMs": finished_at - accepted_at,
            }
        if attempt < config.poll_max_attempts - 1:
            await asyncio.sleep(config.poll_interval_ms / 1000)

    return {
        "ok": False,
        "mode": "poll",
        "pollRequests": poll_requests,
        "error": f"Polling timed out after {config.poll_max_attempts} attempts",
        "finishedAt": None,
        "latencyMs": None,
    }


async def wait_for_ws_result(session: aiohttp.ClientSession, config: argparse.Namespace, request_id: str, accepted_at: int) -> dict:
    ws_url = config.ws_url or build_ws_url(config.base_url)
    ws_url = f"{ws_url}?token={quote(as_bearer_token(config.token), safe='')}"
    opened = False
    last_message = None
    try:
        async with session.ws_connect(
            ws_url,
            headers={"Authorization": as_bearer_token(config.token)},
            timeout=config.timeout_ms / 1000,
            autoclose=True,
            autoping=True,
        ) as ws:
            opened = True
            await ws.send_json({"type": "subscribe", "requestId": request_id})
            message = await asyncio.wait_for(ws.receive(), timeout=config.timeout_ms / 1000)
            if message.type == aiohttp.WSMsgType.TEXT:
                last_message = message.data
                payload = json.loads(message.data)
                if payload.get("type") == "error":
                    return {
                        "ok": False,
                        "mode": "ws",
                        "requestId": request_id,
                        "opened": opened,
                        "lastMessage": last_message,
                        "error": payload.get("message") or "WebSocket returned error payload",
                        "finishedAt": None,
                        "latencyMs": None,
                    }
                finished_at = now_ms()
                return {
                    "ok": True,
                    "mode": "ws",
                    "requestId": request_id,
                    "opened": opened,
                    "lastMessage": last_message,
                    "pass": payload.get("pass"),
                    "asyncStatus": payload.get("asyncStatus"),
                    "finishedAt": finished_at,
                    "latencyMs": finished_at - accepted_at,
                }
            return {
                "ok": False,
                "mode": "ws",
                "requestId": request_id,
                "opened": opened,
                "lastMessage": last_message,
                "closeCode": ws.close_code,
                "error": f"Unexpected websocket message type: {message.type}",
                "finishedAt": None,
                "latencyMs": None,
            }
    except asyncio.TimeoutError:
        return {
            "ok": False,
            "mode": "ws",
            "requestId": request_id,
            "opened": opened,
            "lastMessage": last_message,
            "error": f"WebSocket timed out after {config.timeout_ms}ms",
            "finishedAt": None,
            "latencyMs": None,
        }
    except Exception as exc:
        close_code = None
        if hasattr(exc, "code"):
            close_code = getattr(exc, "code")
        return {
            "ok": False,
            "mode": "ws",
            "requestId": request_id,
            "opened": opened,
            "lastMessage": last_message,
            "closeCode": close_code,
            "error": str(exc),
            "finishedAt": None,
            "latencyMs": None,
        }


def summarize_channel(records: list[dict], key: str) -> dict:
    successful = [item for item in records if item[key].get("ok") and isinstance(item[key].get("latencyMs"), (int, float))]
    latencies = [float(item[key]["latencyMs"]) for item in successful]
    summary = {
        "totalSamples": len(records),
        "successCount": len(successful),
        "successRatePct": round((len(successful) / len(records)) * 100, 2) if records else 0,
        "avgMs": average(latencies),
        "p95Ms": percentile(latencies, 95),
        "p99Ms": percentile(latencies, 99),
        "minMs": min(latencies) if latencies else None,
        "maxMs": max(latencies) if latencies else None,
    }
    if key == "poll":
        poll_requests = [item["poll"]["pollRequests"] for item in successful]
        summary["avgPollRequests"] = average([float(item) for item in poll_requests])
        summary["maxPollRequests"] = max(poll_requests) if poll_requests else None
    return summary


def summarize_comparison(records: list[dict]) -> dict:
    paired = [item for item in records if item["ws"].get("ok") and item["poll"].get("ok")]
    deltas = [round(item["poll"]["latencyMs"] - item["ws"]["latencyMs"], 2) for item in paired]
    ws_latencies = [float(item["ws"]["latencyMs"]) for item in paired]
    poll_latencies = [float(item["poll"]["latencyMs"]) for item in paired]
    avg_delta_pct_values = [
        ((item["poll"]["latencyMs"] - item["ws"]["latencyMs"]) / item["poll"]["latencyMs"]) * 100
        for item in paired
        if item["poll"]["latencyMs"] > 0
    ]
    ws_p95 = percentile(ws_latencies, 95)
    poll_p95 = percentile(poll_latencies, 95)
    return {
        "pairedSuccessCount": len(paired),
        "avgDeltaMs": average(deltas),
        "avgDeltaPct": average(avg_delta_pct_values),
        "p95DeltaMs": round(poll_p95 - ws_p95, 2) if ws_p95 is not None and poll_p95 is not None else None,
        "p95DeltaPct": round(((poll_p95 - ws_p95) / poll_p95) * 100, 2) if ws_p95 is not None and poll_p95 not in (None, 0) else None,
        "avgPollRequests": average([float(item["poll"]["pollRequests"]) for item in paired]),
    }


def print_console_summary(summary: dict) -> None:
    ws = summary["summary"]["ws"]
    poll = summary["summary"]["poll"]
    comparison = summary["summary"]["comparison"]
    print("")
    print(f"Samples: {summary['samples']}")
    print(f"WebSocket  avg={ws['avgMs']}ms  p95={ws['p95Ms']}ms  p99={ws['p99Ms']}ms  success={ws['successRatePct']}%")
    print(
        f"Polling    avg={poll['avgMs']}ms  p95={poll['p95Ms']}ms  p99={poll['p99Ms']}ms  success={poll['successRatePct']}%  avgPollRequests={poll.get('avgPollRequests')}"
    )
    print(
        f"Delta      avg={comparison['avgDeltaMs']}ms  avgPct={comparison['avgDeltaPct']}%  p95Delta={comparison['p95DeltaMs']}ms  p95Pct={comparison['p95DeltaPct']}%"
    )


async def run(config: argparse.Namespace) -> None:
    timeout = aiohttp.ClientTimeout(total=(config.timeout_ms / 1000) + 5)
    connector = aiohttp.TCPConnector(limit=20)
    records: list[dict] = []
    started_at = now_ms()

    async with aiohttp.ClientSession(timeout=timeout, connector=connector) as session:
        for index in range(config.samples):
            sample_no = index + 1
            print(f"Running sample {sample_no}/{config.samples}...")
            request_id, accepted_at = await submit_async_judge(session, config)
            ws_task = asyncio.create_task(wait_for_ws_result(session, config, request_id, accepted_at))
            poll_task = asyncio.create_task(wait_for_poll_result(session, config, request_id, accepted_at))
            ws, poll = await asyncio.gather(ws_task, poll_task)
            record = {
                "sample": sample_no,
                "requestId": request_id,
                "acceptedAt": iso_from_ms(accepted_at),
                "ws": ws,
                "poll": poll,
                "deltaMs": round(poll["latencyMs"] - ws["latencyMs"], 2) if ws.get("ok") and poll.get("ok") else None,
            }
            records.append(record)
            ws_desc = f"{ws['latencyMs']}ms" if ws.get("ok") else f"ERR({ws.get('error')})"
            poll_desc = f"{poll['latencyMs']}ms" if poll.get("ok") else f"ERR({poll.get('error')})"
            print(f"  requestId={request_id} ws={ws_desc} poll={poll_desc} polls={poll.get('pollRequests', 0)}")
            if index < config.samples - 1 and config.pause_ms > 0:
                await asyncio.sleep(config.pause_ms / 1000)

    summary = {
        "runName": "2026-04-07-polling-vs-websocket",
        "startedAt": iso_from_ms(started_at),
        "completedAt": iso_from_ms(now_ms()),
        "baseUrl": config.base_url.rstrip("/"),
        "questionId": int(config.question_id),
        "examId": int(config.exam_id) if config.exam_id else None,
        "samples": config.samples,
        "pollIntervalMs": config.poll_interval_ms,
        "pollMaxAttempts": config.poll_max_attempts,
        "timeoutMs": config.timeout_ms,
        "sampleResults": records,
        "summary": {
            "ws": summarize_channel(records, "ws"),
            "poll": summarize_channel(records, "poll"),
            "comparison": summarize_comparison(records),
        },
    }

    output_path = Path(config.output).resolve()
    output_path.write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")
    print_console_summary(summary)
    print(f"Summary written to {output_path}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--ws-url", default="")
    parser.add_argument("--token", required=True)
    parser.add_argument("--question-id", required=True)
    parser.add_argument("--exam-id", default="")
    parser.add_argument("--program-type", required=True, type=int)
    parser.add_argument("--user-code", required=True)
    parser.add_argument("--samples", required=True, type=int)
    parser.add_argument("--poll-interval-ms", required=True, type=int)
    parser.add_argument("--poll-max-attempts", required=True, type=int)
    parser.add_argument("--timeout-ms", required=True, type=int)
    parser.add_argument("--pause-ms", required=True, type=int)
    parser.add_argument("--output", required=True)
    return parser


if __name__ == "__main__":
    asyncio.run(run(build_parser().parse_args()))
