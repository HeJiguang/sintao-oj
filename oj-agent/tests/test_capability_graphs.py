from app.graphs.capabilities.diagnose_graph import build_diagnose_graph
from app.graphs.capabilities.plan_graph import build_plan_graph
from app.graphs.capabilities.profile_graph import build_profile_graph
from app.graphs.capabilities.recommend_graph import build_recommend_graph
from app.graphs.capabilities.review_graph import build_review_graph
from app.graphs.capabilities.tutor_graph import build_tutor_graph


def test_capability_graph_builders_return_compiled_graphs():
    graphs = [
        build_tutor_graph(),
        build_diagnose_graph(),
        build_recommend_graph(),
        build_plan_graph(),
        build_review_graph(),
        build_profile_graph(),
    ]

    assert all(graph is not None for graph in graphs)
    assert all(hasattr(graph, "invoke") for graph in graphs)
