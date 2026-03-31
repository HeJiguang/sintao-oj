import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";

import HomePage from "../app/page";

async function main() {
  const html = renderToStaticMarkup(await HomePage());

  assert.match(html, /SynCode/);
  assert.match(html, /开始体验/);
  assert.match(html, /让 AI 成为你的编程训练搭档/);
  assert.match(html, /AI 原生编程训练平台/);
}

void main();
