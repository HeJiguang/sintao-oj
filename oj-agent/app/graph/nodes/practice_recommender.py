import json
import os
import urllib.parse
import urllib.request

from app.graph.state import AgentState, PracticeTopic, QuestionRecommendation
from app.graph.nodes.node_utils import append_status


def _search_queries(topics: list[PracticeTopic]) -> list[str]:
    return [f"OJ {topic['topic']}" for topic in topics]


def _study_sequence(topics: list[PracticeTopic]) -> list[str]:
    sequence = []
    if not topics:
        return sequence

    primary = topics[0]
    sequence.append(f"Reinforce the current pattern with {primary['topic']}.")
    if len(topics) > 1:
        sequence.append(f"Then widen the pattern set with {topics[1]['topic']}.")
    if len(topics) > 2:
        sequence.append(f"Finish by improving transfer and debugging through {topics[2]['topic']}.")
    return sequence


def fetch_questions_for_queries(queries: list[str], limit: int = 3) -> list[QuestionRecommendation]:
    base_url = os.getenv("OJ_QUESTION_SERVICE_BASE_URL")
    if not base_url or not queries:
        return []

    normalized_base = base_url.rstrip("/")
    seen: set[str] = set()
    recommendations: list[QuestionRecommendation] = []

    for query in queries:
        url = (
            f"{normalized_base}/question/semiLogin/list?"
            + urllib.parse.urlencode({"keyword": query, "pageNum": 1, "pageSize": limit})
        )
        try:
            with urllib.request.urlopen(url, timeout=3) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except Exception:
            continue

        rows = payload.get("rows") or []
        for row in rows:
            question_id = row.get("questionId")
            title = row.get("title")
            if not question_id or not title or str(question_id) in seen:
                continue
            recommendations.append(
                {
                    "question_id": str(question_id),
                    "title": str(title),
                    "difficulty": row.get("difficulty"),
                    "source_query": query,
                }
            )
            seen.add(str(question_id))
            if len(recommendations) >= limit:
                return recommendations

    return recommendations


def practice_recommender_node(state: AgentState) -> AgentState:
    topics = list(state.get("practice_topics") or [])
    if not topics:
        topics = [
            {
                "topic": "Problem classification",
                "reason": "The next useful step is building a better mapping from statements to solution patterns.",
                "next_step": "Practice identifying the pattern before touching the code editor.",
            }
        ]

    search_queries = _search_queries(topics[:3])
    question_recommendations = fetch_questions_for_queries(search_queries)

    practice_plan = {
        "focus": "Turn the current problem into a repeatable practice plan.",
        "topics": topics[:3],
        "search_queries": search_queries,
        "study_sequence": _study_sequence(topics[:3]),
        "when_to_move_on": "Move to the next topic once you can explain the pattern choice and solve one variant without hints.",
        "checkpoint": "You should be able to explain why each recommended topic matches the current failure or question.",
        "question_recommendations": question_recommendations,
    }

    return {
        **state,
        "practice_plan": practice_plan,
        "question_recommendations": question_recommendations,
        "status_events": append_status(
            state,
            "practice_recommender",
            "Practice recommendations prepared from the current problem, failure signals, and catalog suggestions.",
        ),
    }
