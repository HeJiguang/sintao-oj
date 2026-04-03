import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";

import HomePage from "../app/page";

async function main() {
  const html = renderToStaticMarkup(await HomePage());

  assert.match(html, /SynCode/);
  assert.match(html, /开始体验/);
  assert.match(html, /让 AI 成为你的/);
  assert.match(html, /编程训练搭档/);
  assert.match(html, /AI 辅助编程训练平台/);
  assert.match(html, /Workflow Console/);
  assert.match(html, /Session Preview/);
  assert.match(html, /Timeline/);
}

void main();
