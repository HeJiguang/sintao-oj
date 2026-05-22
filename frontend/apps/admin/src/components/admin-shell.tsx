import * as React from "react";
import type { ReactNode } from "react";

import { adminNav } from "@aioj/config";
import { Button, Panel } from "@aioj/ui";

type AdminShellProps = {
  title: string;
  description: string;
  adminName: string;
  children: ReactNode;
};

export function AdminShell({ children, description, title, adminName }: AdminShellProps) {
  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,_#09090b_0%,_#111827_100%)] px-4 py-4 md:px-6">
      <div className="mx-auto grid min-h-[calc(100vh-2rem)] max-w-[1680px] gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
        <div className="space-y-4">
          <Panel className="p-5" tone="strong">
            <p className="kicker">Admin</p>
            <h1 className="mt-2 text-2xl font-semibold">SynCode Console</h1>
            <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">
              当前登录管理员：{adminName}。后台现在以独立站运行，便于本地调试和后续单独部署。
            </p>
            <form action="/api/auth/logout" method="post" className="mt-4">
              <Button size="sm" variant="secondary">
                退出登录
              </Button>
            </form>
          </Panel>

          <Panel className="p-4">
            <p className="kicker mb-3">Navigation</p>
            <nav className="space-y-2">
              {adminNav.map((item) => (
                <a
                  key={item.label}
                  className="flex items-center justify-between rounded-[18px] border border-transparent px-4 py-3 text-sm text-[var(--text-secondary)] transition hover:border-[var(--border-soft)] hover:bg-white/5 hover:text-[var(--text-primary)]"
                  href={item.href}
                >
                  <span>{item.label}</span>
                  <span className="text-[var(--text-muted)]">/</span>
                </a>
              ))}
            </nav>
          </Panel>
        </div>

        <div className="space-y-4">
          <Panel className="p-6" tone="strong">
            <p className="kicker">Control Plane</p>
            <h2 className="section-title mt-3">{title}</h2>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
          </Panel>
          {children}
        </div>
      </div>
    </div>
  );
}
