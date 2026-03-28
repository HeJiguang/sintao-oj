from dataclasses import dataclass
import logging
import os
from typing import Any

import httpx
import yaml


LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class NacosBootstrap:
    enabled: bool
    server_addr: str | None
    namespace: str | None
    group: str
    data_id: str
    username: str | None
    password: str | None


def _to_bool(raw: str | None, default: bool) -> bool:
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _bootstrap() -> NacosBootstrap:
    server_addr = os.getenv("OJ_AGENT_NACOS_SERVER_ADDR")
    return NacosBootstrap(
        enabled=_to_bool(os.getenv("OJ_AGENT_NACOS_CONFIG_ENABLED"), bool(server_addr)),
        server_addr=server_addr,
        namespace=os.getenv("OJ_AGENT_NACOS_NAMESPACE"),
        group=os.getenv("OJ_AGENT_NACOS_GROUP", "DEFAULT_GROUP"),
        data_id=os.getenv("OJ_AGENT_NACOS_CONFIG_DATA_ID", "oj-agent-local.yaml"),
        username=os.getenv("OJ_AGENT_NACOS_USERNAME"),
        password=os.getenv("OJ_AGENT_NACOS_PASSWORD"),
    )


def _login(client: httpx.Client, bootstrap: NacosBootstrap) -> str | None:
    if not bootstrap.username or not bootstrap.password:
        return None
    response = client.post(
        bootstrap.server_addr.rstrip("/") + "/nacos/v1/auth/users/login",
        data={
            "username": bootstrap.username,
            "password": bootstrap.password,
        },
    )
    response.raise_for_status()
    payload = response.json()
    return payload.get("accessToken")


def _query_params(bootstrap: NacosBootstrap, access_token: str | None) -> dict[str, str]:
    params = {
        "dataId": bootstrap.data_id,
        "group": bootstrap.group,
    }
    if bootstrap.namespace:
        params["tenant"] = bootstrap.namespace
    if access_token:
        params["accessToken"] = access_token
    return params


def load_nacos_config() -> dict[str, Any]:
    bootstrap = _bootstrap()
    if not bootstrap.enabled or not bootstrap.server_addr:
        return {}

    try:
        with httpx.Client(timeout=5.0) as client:
            access_token = _login(client, bootstrap)
            response = client.get(
                bootstrap.server_addr.rstrip("/") + "/nacos/v1/cs/configs",
                params=_query_params(bootstrap, access_token),
            )
            response.raise_for_status()
            raw = response.text
        data = yaml.safe_load(raw) or {}
        if not isinstance(data, dict):
            return {"raw": raw, "data": {}}
        return {"raw": raw, "data": data}
    except Exception as exc:
        LOGGER.warning("Failed to load oj-agent config from Nacos.", exc_info=exc)
        return {}
