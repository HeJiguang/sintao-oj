"use client";

import * as React from "react";
import { Button, Input, Panel, Tag } from "@aioj/ui";

export default function AdminLoginPage() {
  const [userAccount, setUserAccount] = React.useState("admin");
  const [password, setPassword] = React.useState("123456");
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userAccount, password })
      });

      const payload = (await response.json().catch(() => ({}))) as { message?: string };
      if (!response.ok) {
        throw new Error(payload.message ?? "管理员登录失败。");
      }

      window.location.href = "/";
    } catch (err) {
      setError(err instanceof Error ? err.message : "管理员登录失败。");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_rgba(255,255,255,0.08),_transparent_32%),linear-gradient(180deg,_#09090b_0%,_#111827_100%)] px-6 py-12 text-white">
      <div className="mx-auto grid min-h-[calc(100vh-6rem)] w-full max-w-6xl gap-6 lg:grid-cols-[1.1fr_440px]">
        <Panel className="flex flex-col justify-between p-8 md:p-10" tone="strong">
          <div>
            <Tag tone="accent">Admin Console</Tag>
            <h1 className="mt-6 max-w-2xl text-4xl font-semibold tracking-tight md:text-5xl">
              SynCode 管理后台
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-8 text-[var(--text-secondary)]">
              这里是独立运行的后台站，不再依赖 `/admin` 子路径。登录后你可以直接进入题目、考试、公告和用户维护面板。
            </p>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <Panel className="p-4">
              <p className="kicker">Scope</p>
              <p className="mt-2 text-sm leading-7 text-[var(--text-secondary)]">题目、考试、公告、用户状态统一在一个后台入口处理。</p>
            </Panel>
            <Panel className="p-4">
              <p className="kicker">Runtime</p>
              <p className="mt-2 text-sm leading-7 text-[var(--text-secondary)]">当前联调模式为本地前端 + 本地 Java 微服务 + 云中间件。</p>
            </Panel>
            <Panel className="p-4">
              <p className="kicker">Default Seed</p>
              <p className="mt-2 text-sm leading-7 text-[var(--text-secondary)]">开发库默认管理员账号已预填，便于你先确认整体链路。</p>
            </Panel>
          </div>
        </Panel>

        <Panel className="flex flex-col justify-center p-8 md:p-10">
          <div className="space-y-2">
            <p className="kicker">Sign In</p>
            <h2 className="text-3xl font-semibold">管理员登录</h2>
            <p className="text-sm leading-7 text-[var(--text-secondary)]">如果登录失败，优先检查网关是否放行 `/system/sysUser/login`。</p>
          </div>

          <form className="mt-8 space-y-4" onSubmit={handleSubmit}>
            <Input placeholder="请输入管理员账号" value={userAccount} onChange={(event) => setUserAccount(event.target.value)} />
            <Input placeholder="请输入密码" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
            {error ? <p className="text-sm text-[var(--danger)]">{error}</p> : null}
            <Button className="w-full" size="lg" type="submit" disabled={submitting}>
              {submitting ? "登录中..." : "进入后台"}
            </Button>
          </form>
        </Panel>
      </div>
    </main>
  );
}
