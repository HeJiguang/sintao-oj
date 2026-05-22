import * as React from "react";
import { ApiError, getExamDetail, getExamFirstQuestion, getProblemDetail } from "@aioj/api";

import { AppShell } from "../../../components/app-shell";
import { EditorPanel } from "../../../components/editor-panel";
import { getServerAccessToken } from "../../../lib/server-auth";
import { Panel, Tag } from "@aioj/ui";

type PageProps = {
  params: Promise<{ examId: string }>;
};

function ExamBlockedState({
  title,
  description
}: {
  title: string;
  description: string;
}) {
  return (
    <AppShell eyebrow="Exam Mode" title={title} description={description}>
      <Panel className="max-w-3xl p-6" tone="strong">
        <div className="space-y-3">
          <Tag tone="warning">Exam Access</Tag>
          <h3 className="text-xl font-semibold text-[var(--text-primary)]">{title}</h3>
          <p className="text-sm leading-7 text-[var(--text-secondary)]">{description}</p>
          <a href="/app/exams" className="inline-flex text-sm font-medium text-[var(--accent)]">
            返回考试列表
          </a>
        </div>
      </Panel>
    </AppShell>
  );
}

export default async function ExamWorkspacePage({ params }: PageProps) {
  const { examId } = await params;
  const token = await getServerAccessToken();
  const exam = await getExamDetail(examId);

  if (!token) {
    return <ExamBlockedState title={exam.title} description="请先登录并报名考试，然后再进入考试工作区。" />;
  }

  if (exam.status !== "进行中") {
    return (
      <ExamBlockedState
        title={exam.title}
        description={exam.status === "未开始" ? "考试尚未开始，当前页面不再提前放行。请在开始时间后再进入。" : "考试已结束，当前页面不再继续开放作答入口。"}
      />
    );
  }

  try {
    const firstQuestionId = await getExamFirstQuestion(examId, token);
    const detail = await getProblemDetail(firstQuestionId, token);

    return (
      <AppShell
        eyebrow="Exam Mode"
        title={exam.title}
        description="考试工作区会优先展示当前考试状态与题目内容，并将判题请求明确归属到考试上下文。"
      >
        <Panel className="p-5" tone="strong">
          <div className="grid gap-4 md:grid-cols-4">
            <div className="rounded-[18px] border border-[var(--border-soft)] bg-black/20 p-4">
              <p className="text-sm text-[var(--text-muted)]">状态</p>
              <p className="mt-2 text-lg font-semibold">{exam.status}</p>
            </div>
            <div className="rounded-[18px] border border-[var(--border-soft)] bg-black/20 p-4">
              <p className="text-sm text-[var(--text-muted)]">开始时间</p>
              <p className="mt-2 text-lg font-semibold">{exam.startTime}</p>
            </div>
            <div className="rounded-[18px] border border-[var(--border-soft)] bg-black/20 p-4">
              <p className="text-sm text-[var(--text-muted)]">当前题号</p>
              <p className="mt-2 text-lg font-semibold">1 / {exam.questionCount}</p>
            </div>
            <div className="rounded-[18px] border border-[var(--border-soft)] bg-black/20 p-4">
              <p className="text-sm text-[var(--text-muted)]">考试结束</p>
              <p className="mt-2 text-lg font-semibold">{exam.endTime}</p>
            </div>
          </div>
        </Panel>
        <div className="grid gap-4 2xl:grid-cols-[0.82fr_1.18fr]">
          <Panel className="p-6" tone="strong">
            <div className="flex items-center gap-3">
              <Tag tone="warning">Exam</Tag>
              <Tag>{detail.algorithmTag}</Tag>
            </div>
            <h3 className="mt-4 text-xl font-semibold">{detail.title}</h3>
            <div className="mt-5 space-y-4">
              {detail.content.map((item) => (
                <p key={item} className="text-sm leading-8 text-[var(--text-secondary)]">
                  {item}
                </p>
              ))}
            </div>
          </Panel>
          <EditorPanel
            initialCode={detail.starterCode}
            questionId={detail.questionId}
            examId={examId}
            questionTitle={detail.title}
            questionContent={detail.content.join("\n\n")}
            examples={detail.examples}
          />
        </div>
      </AppShell>
    );
  } catch (error) {
    const message =
      error instanceof ApiError
        ? error.message
        : error instanceof Error && error.message
          ? error.message
          : "进入考试失败，请稍后重试。";

    return <ExamBlockedState title={exam.title} description={message} />;
  }
}
