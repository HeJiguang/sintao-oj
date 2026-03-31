"use client";

import * as React from "react";
import { useState } from "react";
import { useRouter } from "next/navigation";

import { setBrowserAccessToken } from "@aioj/api";
import { Button, Input, Panel } from "@aioj/ui";
import { appApiPath, appInternalPath } from "../../lib/paths";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function sendCode() {
    setLoading(true);
    setStatus(null);

    try {
      const response = await fetch(appApiPath("/auth/send-code"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email })
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.message ?? "发送验证码失败。");
      }

      setStatus("验证码已发送，请检查邮箱。");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "发送验证码失败。");
    } finally {
      setLoading(false);
    }
  }

  async function login() {
    setLoading(true);
    setStatus(null);

    try {
      const response = await fetch(appApiPath("/auth/login"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code })
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.message ?? "登录失败。");
      }

      setBrowserAccessToken(payload.token);
      router.push(appInternalPath("/"));
      router.refresh();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "登录失败。");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen px-4 py-6 md:px-6">
      <div className="mx-auto grid min-h-[calc(100vh-3rem)] max-w-[1400px] gap-4 lg:grid-cols-[1.12fr_0.88fr]">
        <Panel className="hidden p-8 lg:flex lg:flex-col lg:justify-between" tone="accent">
          <div>
            <p className="kicker">SynCode</p>
            <h1 className="mt-3 max-w-xl text-5xl font-semibold leading-tight tracking-[-0.04em]">
              登录真实后端环境
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-8 text-[var(--text-secondary)]">
              当前登录会直接连接真实后端服务。输入邮箱后先发送验证码，再使用邮箱验证码登录。
            </p>
          </div>
        </Panel>

        <Panel className="flex items-center justify-center p-6 md:p-10">
          <div className="w-full max-w-[460px]">
            <p className="kicker">Sign In</p>
            <h2 className="mt-3 text-3xl font-semibold">登录 SynCode</h2>
            <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">
              使用邮箱验证码进入工作台。未登录时系统会持续提示当前正在使用演示数据。
            </p>

            <div className="mt-6 space-y-4">
              <Input placeholder="输入邮箱地址" value={email} onChange={(event) => setEmail(event.target.value)} />

              <div className="grid gap-3 md:grid-cols-[1fr_120px]">
                <Input placeholder="输入验证码" value={code} onChange={(event) => setCode(event.target.value)} />
                <Button disabled={loading || !email} variant="secondary" onClick={sendCode}>
                  发送验证码
                </Button>
              </div>

              <Button className="w-full" disabled={loading || !email || !code} size="lg" onClick={login}>
                登录
              </Button>

              {status ? <p className="text-sm text-[var(--text-secondary)]">{status}</p> : null}
            </div>
          </div>
        </Panel>
      </div>
    </main>
  );
}
