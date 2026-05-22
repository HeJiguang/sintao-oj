export const DEFAULT_AGENT_API_BASE_URL = "http://127.0.0.1:19090";

export function resolveAgentApiBaseUrl() {
  const baseUrl = process.env.SYNCODE_AGENT_BASE_URL ?? DEFAULT_AGENT_API_BASE_URL;

  return baseUrl.replace(/\/+$/, "");
}
