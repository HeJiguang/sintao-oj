import json
from pathlib import Path


def test_seed_documents_file_exists_and_contains_minimum_fields():
    seed_path = Path(__file__).resolve().parents[1] / "resources" / "knowledge" / "seed_documents.json"

    assert seed_path.exists()

    payload = json.loads(seed_path.read_text(encoding="utf-8"))
    assert isinstance(payload, list)
    assert payload

    first = payload[0]
    required_keys = {
        "id",
        "title",
        "snippet",
        "source_name",
        "source_url",
        "license",
        "topic",
        "tags",
        "evidence_type",
    }
    assert required_keys.issubset(first.keys())
