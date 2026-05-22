from fastapi import APIRouter
from fastapi.responses import HTMLResponse

from app.config import load_settings
from app.services.run_service import run_service


router = APIRouter(prefix="/debug", tags=["debug"])


@router.get("/config")
def debug_config() -> dict:
    settings = load_settings()
    return {
        "provider": settings.llm_provider,
        "model": settings.llm_model,
        "baseUrl": settings.llm_base_url,
        "realLlmEnabled": settings.real_llm_enabled,
    }


@router.get("/agent", response_class=HTMLResponse)
def agent_debug_page() -> str:
    """内置调试页。

    这个页面的目标不是替代正式前端，而是让我们在接正式前端之前，
    先把 agent 模块自己的输入、事件流和产物看清楚。
    """

    return """<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>OJ Agent 调试页</title>
  <style>
    :root { color-scheme: dark; }
    body { margin: 0; font-family: "JetBrains Mono", "IBM Plex Sans", sans-serif; background: #101418; color: #edf2f7; }
    main { max-width: 1220px; margin: 0 auto; padding: 24px; display: grid; gap: 16px; }
    .panel { background: linear-gradient(180deg, #151b22, #0f141a); border: 1px solid #273341; border-radius: 16px; padding: 16px; box-shadow: 0 18px 40px rgba(0,0,0,.25); }
    h1, h2, h3, p { margin: 0; }
    .grid { display: grid; grid-template-columns: 1.05fr .95fr; gap: 16px; }
    label { display: grid; gap: 8px; font-size: 13px; color: #a7b3c2; }
    input, textarea, select, button { width: 100%; box-sizing: border-box; border-radius: 10px; border: 1px solid #314154; background: #0d1218; color: #edf2f7; padding: 10px 12px; font: inherit; }
    textarea { min-height: 110px; resize: vertical; }
    button { cursor: pointer; background: #0f766e; border-color: #0f766e; font-weight: 700; }
    button:hover { opacity: .92; }
    pre { white-space: pre-wrap; word-break: break-word; background: #0b1015; border: 1px solid #253140; border-radius: 12px; padding: 12px; min-height: 120px; margin: 0; }
    .status { display: flex; gap: 12px; flex-wrap: wrap; color: #a7b3c2; font-size: 13px; }
    .artifact { border: 1px solid #304153; border-radius: 12px; padding: 12px; background: #0d1319; display: grid; gap: 10px; }
    .artifact h3 { margin-bottom: 4px; }
    .evidence { display: grid; gap: 8px; }
    .evidence-item { padding: 10px; border-radius: 10px; border: 1px solid #263444; background: #0a0f14; }
    .split { display: grid; gap: 12px; }
    .hint { color: #a7b3c2; font-size: 13px; }
    @media (max-width: 980px) { .grid { grid-template-columns: 1fr; } }
  </style>
</head>
<body>
<main>
  <div class="panel">
    <h1>OJ Agent 独立调试页</h1>
    <p style="margin-top:8px;color:#a7b3c2;">
      这个页面直接调用当前服务的 <code>/api/runs</code>、SSE 和 <code>/artifacts</code> 接口，
      用来单独验证 agent 模块，而不是依赖正式前端。
    </p>
    <div id="config" class="status" style="margin-top:12px;"></div>
  </div>

  <div class="grid">
    <div class="panel">
      <h2>请求输入</h2>
      <p class="hint" style="margin-top:8px;">这里就是一次 run 的最小测试入口。改这里，就等于在改发送给 agent 的工作区上下文。</p>
      <div style="display:grid;gap:12px;margin-top:12px;">
        <label>运行类型
          <select id="runType">
            <option value="interactive_tutor">interactive_tutor</option>
            <option value="interactive_diagnosis">interactive_diagnosis</option>
            <option value="interactive_recommendation">interactive_recommendation</option>
            <option value="interactive_review">interactive_review</option>
          </select>
        </label>
        <label>题目标题
          <input id="questionTitle" value="Two Sum" />
        </label>
        <label>题目内容
          <textarea id="questionContent">Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.</textarea>
        </label>
        <label>当前代码
          <textarea id="userCode">class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{0, 0};\n    }\n}</textarea>
        </label>
        <label>判题结果
          <input id="judgeResult" value="WA on sample #2" />
        </label>
        <label>用户问题
          <textarea id="userMessage">Help me diagnose the issue, but do not give the full solution immediately.</textarea>
        </label>
        <button id="runButton" type="button">发起一次运行</button>
      </div>
    </div>

    <div class="panel split">
      <div>
        <h2>运行状态</h2>
        <div class="status" style="margin-top:12px;">
          <span id="runStatus">idle</span>
          <span id="runId">runId: -</span>
        </div>
      </div>
      <div>
        <h2>流式正文</h2>
        <pre id="streamOutput" style="margin-top:12px;"></pre>
      </div>
      <div>
        <h2>事件流</h2>
        <pre id="eventOutput" style="margin-top:12px;"></pre>
      </div>
    </div>
  </div>

  <div class="grid">
    <div class="panel">
      <h2>产物卡片</h2>
      <div id="artifacts" style="display:grid;gap:12px;margin-top:12px;"></div>
    </div>
    <div class="panel">
      <h2>产物原始 JSON</h2>
      <pre id="artifactJson" style="margin-top:12px;"></pre>
    </div>
  </div>

  <div class="panel">
    <h2>最终运行时状态</h2>
    <p class="hint" style="margin-top:8px;">这里展示 graph 跑完以后保存在服务端的 state 快照，适合学习每个节点到底往 state 里写了什么。</p>
    <pre id="stateJson" style="margin-top:12px;"></pre>
  </div>
</main>

<script>
  const streamOutput = document.getElementById("streamOutput");
  const eventOutput = document.getElementById("eventOutput");
  const artifactsNode = document.getElementById("artifacts");
  const artifactJsonNode = document.getElementById("artifactJson");
  const stateJsonNode = document.getElementById("stateJson");
  const runStatusNode = document.getElementById("runStatus");
  const runIdNode = document.getElementById("runId");
  let eventSource = null;

  async function loadConfig() {
    const response = await fetch("/debug/config", { cache: "no-store" });
    const config = await response.json();
    document.getElementById("config").innerHTML =
      `<span>provider: ${config.provider}</span><span>model: ${config.model}</span><span>realLlmEnabled: ${config.realLlmEnabled}</span>`;
  }

  function appendEvent(line) {
    eventOutput.textContent += line + "\\n";
    eventOutput.scrollTop = eventOutput.scrollHeight;
  }

  function renderArtifacts(artifacts) {
    artifactsNode.innerHTML = "";
    artifactJsonNode.textContent = JSON.stringify(artifacts, null, 2);
    artifacts.forEach((artifact) => {
      const wrapper = document.createElement("div");
      wrapper.className = "artifact";
      const evidence = Array.isArray(artifact.body?.evidence) ? artifact.body.evidence : [];
      wrapper.innerHTML = `
        <div>
          <h3>${artifact.title}</h3>
          <p class="hint" style="margin-top:6px;">${artifact.summary || ""}</p>
        </div>
        <pre>${artifact.body?.answer || ""}</pre>
        <p><strong>下一步：</strong>${artifact.body?.nextAction || ""}</p>
        <div class="evidence">
          ${evidence.map((item) => `<div class="evidence-item"><strong>${item.title}</strong><div>${item.sourceId}</div><div>${item.snippet}</div></div>`).join("")}
        </div>
      `;
      artifactsNode.appendChild(wrapper);
    });
  }

  async function runAgent() {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    streamOutput.textContent = "";
    eventOutput.textContent = "";
    artifactsNode.innerHTML = "";
    artifactJsonNode.textContent = "";
    stateJsonNode.textContent = "";
    runStatusNode.textContent = "creating";
    runIdNode.textContent = "runId: -";

    const payload = {
      runType: document.getElementById("runType").value,
      source: "workspace_panel",
      context: {
        questionId: "1001",
        questionTitle: document.getElementById("questionTitle").value,
        questionContent: document.getElementById("questionContent").value,
        userCode: document.getElementById("userCode").value,
        judgeResult: document.getElementById("judgeResult").value,
        userMessage: document.getElementById("userMessage").value
      }
    };

    const response = await fetch("/api/runs", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const run = await response.json();
    runStatusNode.textContent = run.status;
    runIdNode.textContent = `runId: ${run.runId}`;

    eventSource = new EventSource(`/api/runs/${run.runId}/events`);
    eventSource.addEventListener("run_event", async (event) => {
      const item = JSON.parse(event.data);
      appendEvent(`${item.seq}. ${item.eventType} ${JSON.stringify(item.payload)}`);
      if (item.eventType === "message.delta") {
        streamOutput.textContent += item.payload.delta || "";
      }
      if (item.eventType === "run.completed" || item.eventType === "run.failed") {
        eventSource.close();
        const artifactResponse = await fetch(`/api/runs/${run.runId}/artifacts`, { cache: "no-store" });
        const artifacts = await artifactResponse.json();
        renderArtifacts(artifacts);
        const stateResponse = await fetch(`/debug/runs/${run.runId}/state`, { cache: "no-store" });
        const state = await stateResponse.json();
        stateJsonNode.textContent = JSON.stringify(state, null, 2);
      }
    });
  }

  document.getElementById("runButton").addEventListener("click", () => {
    runAgent().catch((error) => {
      runStatusNode.textContent = "FAILED";
      appendEvent(`client.error ${error.message}`);
    });
  });

  loadConfig();
</script>
</body>
</html>"""


@router.get("/runs/{run_id}/state")
def debug_run_state(run_id: str) -> dict:
    """读取某次 run 的最终运行时状态快照。"""
    return run_service.get_runtime_state(run_id) or {"runId": run_id, "state": None}
