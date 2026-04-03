import * as React from "react";
import { AlertTriangle, Clock3, FileQuestion, ListOrdered } from "lucide-react";
import { getExamDetail, getProblemDetail, getSubmissionHistory } from "@aioj/api";

import { AppShell } from "../../../components/app-shell";
import { EditorPanel } from "../../../components/editor-panel";
import { JudgePanel } from "../../../components/judge-panel";
import { getServerAccessToken } from "../../../lib/server-auth";
import { Panel, Tag } from "@aioj/ui";

type PageProps = {
  params: Promise<{ examId: string }>;
};

function resolveExamTone(status: string) {
  if (status === "进行中") return "warning" as const;
  if (status === "已结束") return "default" as const;
  return "accent" as const;
}

function ErrorState({
  title,
  description,
  message,
  icon
}: {
  title: string;
  description: string;
  message: string;
  icon: React.ReactNode;
}) {
  return (
    <AppShell eyebrow="Exam Mode" title={title} description={description}>
      <Panel className="border border-white/10 bg-white/[0.03] p-8 backdrop-blur-md">
        <div className="flex items-start gap-4">
          <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-3 text-zinc-200">{icon}</div>
          <div className="space-y-2">
            <p className="kicker">Exam</p>
            <h1 className="text-2xl font-semibold text-zinc-50">{title}</h1>
            <p className="max-w-2xl text-sm leading-7 text-zinc-400">{message}</p>
          </div>
        </div>
      </Panel>
    </AppShell>
  );
}

export default async function ExamWorkspacePage({ params }: PageProps) {
  const { examId } = await params;
  const token = await getServerAccessToken();

  let exam;
  try {
    exam = await getExamDetail(examId, token);
  } catch (error) {
    return (
      <ErrorState
        title="考试信息加载失败"
        description="请稍后刷新，或返回考试列表重新进入。"
        message={error instanceof Error ? error.message : "当前考试暂时无法加载。"}
        icon={<AlertTriangle size={18} />}
      />
    );
  }

  const questionId = exam.firstQuestionId;

  if (!questionId) {
    return (
      <ErrorState
        title={exam.title}
        description="考试信息已加载，但当前还没有可进入的题目。"
        message="当前考试暂时没有可进入的题目，或首题尚未配置完成。请先检查考试题目绑定和发布状态，再重新进入考试页。"
        icon={<FileQuestion size={18} />}
      />
    );
  }

  let detail;
  let submissions;
  try {
    [detail, submissions] = await Promise.all([
      getProblemDetail(questionId, token),
      getSubmissionHistory(questionId, token, examId)
    ]);
  } catch (error) {
    return (
      <ErrorState
        title="题面加载失败"
        description="考试壳层已经打开，但题面数据暂时没有取回。"
        message={error instanceof Error ? error.message : "当前题面暂时无法加载。"}
        icon={<FileQuestion size={18} />}
      />
    );
  }

  const questionContent = detail.content.join("\n\n");

  return (
    <AppShell
      eyebrow="Exam Mode"
      title={exam.title}
      description="考试工作区会把状态、时间和当前题号放在更靠前的位置，避免在作答过程中频繁切换上下文。"
    >
      <Panel className="border border-white/10 bg-white/[0.03] p-5 backdrop-blur-md" tone="strong">
        <div className="grid gap-4 md:grid-cols-4">
          <div className="rounded-[18px] border border-white/10 bg-white/[0.02] p-4 transition-all duration-300 ease-out hover:border-white/20 hover:bg-white/[0.04]">
            <p className="text-sm text-zinc-500">状态</p>
            <div className="mt-2">
              <Tag tone={resolveExamTone(exam.status)}>{exam.status}</Tag>
            </div>
          </div>
          <div className="rounded-[18px] border border-white/10 bg-white/[0.02] p-4 transition-all duration-300 ease-out hover:border-white/20 hover:bg-white/[0.04]">
            <p className="flex items-center gap-2 text-sm text-zinc-500">
              <Clock3 size={14} />
              时间区间
            </p>
            <p className="mt-2 text-base font-semibold text-zinc-100">{exam.startTime} - {exam.endTime}</p>
          </div>
          <div className="rounded-[18px] border border-white/10 bg-white/[0.02] p-4 transition-all duration-300 ease-out hover:border-white/20 hover:bg-white/[0.04]">
            <p className="flex items-center gap-2 text-sm text-zinc-500">
              <ListOrdered size={14} />
              当前题号
            </p>
            <p className="mt-2 text-lg font-semibold text-zinc-50">1 / {exam.questionCount}</p>
          </div>
          <div className="rounded-[18px] border border-white/10 bg-white/[0.02] p-4 transition-all duration-300 ease-out hover:border-white/20 hover:bg-white/[0.04]">
            <p className="text-sm text-zinc-500">导航</p>
            <p className="mt-2 text-lg font-semibold text-zinc-50">受控切题</p>
          </div>
        </div>
      </Panel>

      <div className="grid gap-4 2xl:grid-cols-[0.82fr_1.18fr]">
        <Panel className="border border-white/10 bg-white/[0.03] p-6 backdrop-blur-md" tone="strong">
          <div className="flex items-center gap-3">
            <Tag tone="warning">Exam</Tag>
            <Tag>{detail.algorithmTag}</Tag>
            <Tag tone="accent">Heat {detail.heat}</Tag>
          </div>
          <h3 className="mt-4 text-xl font-semibold text-zinc-50">{detail.title}</h3>
          <div className="mt-5 space-y-4">
            {detail.content.map((item) => (
              <p key={item} className="text-sm leading-8 text-zinc-400">
                {item}
              </p>
            ))}
          </div>
        </Panel>

        <div className="grid gap-4">
          <JudgePanel questionId={questionId} examId={examId} submissions={submissions} />
          <EditorPanel
            initialCode={detail.starterCode}
            questionId={questionId}
            examId={examId}
            questionTitle={detail.title}
            questionContent={questionContent}
            examples={detail.examples}
          />
        </div>
      </div>
    </AppShell>
  );
}
