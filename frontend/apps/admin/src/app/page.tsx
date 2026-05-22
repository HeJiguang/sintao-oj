import * as React from "react";

import { AdminShell } from "../components/admin-shell";
import { Panel, StatCard, Tag } from "@aioj/ui";
import { getAdminExamRows, getAdminNoticeRows, getAdminProfile, getAdminQuestionRows, getAdminUserRows } from "../lib/admin-api";
import { requireAdminAccessToken } from "../lib/server-auth";

export default async function AdminDashboardPage() {
  const token = await requireAdminAccessToken();
  const [admin, questions, exams, users, notices] = await Promise.all([
    getAdminProfile(token),
    getAdminQuestionRows(token),
    getAdminExamRows(token),
    getAdminUserRows(token),
    getAdminNoticeRows(token)
  ]);

  const publishedNoticeCount = notices.filter((item) => item.statusLabel === "已发布").length;
  const activeExamCount = exams.filter((item) => item.status === "已发布").length;
  const frozenUserCount = users.filter((item) => item.status === "冻结").length;

  return (
    <AdminShell
      adminName={admin.nickName}
      title="管理概览"
      description="后台首页优先给出当前可操作资源的总览、风险点和快捷入口，而不是堆叠低价值信息。"
    >
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard detail="当前题库中可维护的题目总量" label="题目数" value={String(questions.length)} />
        <StatCard detail="当前已创建的考试场次数量" label="考试数" value={String(exams.length)} />
        <StatCard detail="当前已发布公告数量" label="已发布公告" value={String(publishedNoticeCount)} />
        <StatCard detail="当前处于冻结状态的用户数量" label="冻结用户" value={String(frozenUserCount)} />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.08fr_0.92fr]">
        <Panel className="p-5" tone="strong">
          <p className="kicker">Operations Focus</p>
          <h3 className="mt-2 text-xl font-semibold">当前后台关注点</h3>
          <div className="mt-5 space-y-3 text-sm leading-7 text-[var(--text-secondary)]">
            <p>考试当前共 {exams.length} 场，其中可发布或已发布的考试 {activeExamCount} 场。</p>
            <p>公告模块已切到真实 `tb_notice`，现在可以直接验证新增、编辑、发布、置顶和撤回。</p>
            <p>题目编辑、考试编排和用户状态切换都已经走真实后端接口，不再依赖 mock。</p>
          </div>
        </Panel>

        <Panel className="p-5">
          <p className="kicker">Quick Actions</p>
          <h3 className="mt-2 text-xl font-semibold">常用入口</h3>
          <div className="mt-5 grid gap-3">
            <a className="rounded-[18px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3 text-sm hover:bg-white/5" href="/questions/new">
              新建题目
            </a>
            <a className="rounded-[18px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3 text-sm hover:bg-white/5" href="/exams/new">
              新建考试
            </a>
            <a className="rounded-[18px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3 text-sm hover:bg-white/5" href="/notices/new">
              发布公告
            </a>
            <a className="rounded-[18px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3 text-sm hover:bg-white/5" href="/users">
              查看用户状态
            </a>
          </div>
        </Panel>
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <Panel className="p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="kicker">Question Bank</p>
              <h3 className="mt-2 text-lg font-semibold">最近题目</h3>
            </div>
            <a href="/questions" className="text-sm text-[var(--accent)]">查看全部</a>
          </div>
          <div className="mt-4 space-y-3">
            {questions.slice(0, 4).map((item) => (
              <div key={item.questionId} className="rounded-[16px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3">
                <p className="text-sm font-medium text-[var(--text-primary)]">{item.title}</p>
                <p className="mt-1 text-xs text-[var(--text-muted)]">#{item.questionId} · {item.difficulty}</p>
              </div>
            ))}
          </div>
        </Panel>

        <Panel className="p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="kicker">Notices</p>
              <h3 className="mt-2 text-lg font-semibold">公告状态</h3>
            </div>
            <a href="/notices" className="text-sm text-[var(--accent)]">查看全部</a>
          </div>
          <div className="mt-4 space-y-3">
            {notices.slice(0, 4).map((item) => (
              <div key={item.noticeId} className="rounded-[16px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-medium text-[var(--text-primary)]">{item.title}</p>
                  <Tag tone={item.statusLabel === "已发布" ? "success" : "default"}>{item.statusLabel}</Tag>
                </div>
                <p className="mt-1 text-xs text-[var(--text-muted)]">{item.publishTime}</p>
              </div>
            ))}
          </div>
        </Panel>

        <Panel className="p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="kicker">Users</p>
              <h3 className="mt-2 text-lg font-semibold">用户风险视图</h3>
            </div>
            <a href="/users" className="text-sm text-[var(--accent)]">查看全部</a>
          </div>
          <div className="mt-4 space-y-3">
            {users.slice(0, 4).map((item) => (
              <div key={item.userId} className="rounded-[16px] border border-[var(--border-soft)] bg-[var(--surface-2)] px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-medium text-[var(--text-primary)]">{item.nickName}</p>
                  <Tag tone={item.status === "正常" ? "success" : "danger"}>{item.status}</Tag>
                </div>
                <p className="mt-1 text-xs text-[var(--text-muted)]">{item.email}</p>
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </AdminShell>
  );
}
