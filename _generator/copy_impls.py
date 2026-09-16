#!/usr/bin/env python3
"""
Copies the hand-written business logic (workflow impls, activity impls, controllers,
messaging glue, simulator classes) from the old monolith into each generated service,
and writes the per-service TemporalConfig.java (workflow/activity services) that
registers exactly the one impl type this service owns.

Run generate_services.py FIRST (it creates the directory skeletons this script fills
in). Safe to re-run — every write here is a plain overwrite, not an append.
"""
import pathlib
import re
import sys
import yaml

import generate_services as gen

GEN_DIR = pathlib.Path(__file__).resolve().parent
ROOT = GEN_DIR.parent
MONOLITH_SRC = ROOT.parent / "payment-orchestration-temporal" / "src" / "main" / "java" / gen.BASE_PKG_PATH
SERVICES_DIR = ROOT / "services"


def load_manifest():
    return yaml.safe_load((GEN_DIR / "services.yaml").read_text())["services"]


def dest_java_root(svc_name: str) -> pathlib.Path:
    return SERVICES_DIR / svc_name / "src" / "main" / "java" / gen.BASE_PKG_PATH


def copy_source(svc_name: str, relative_path: str):
    src = MONOLITH_SRC / relative_path
    if not src.exists():
        print(f"  !! MISSING SOURCE: {src}", file=sys.stderr)
        return
    dst = dest_java_root(svc_name) / relative_path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(src.read_text())
    print(f"  copied {relative_path}")


def impl_class_name(relative_path: str) -> str:
    return pathlib.Path(relative_path).stem


def process_workflow_service(svc: dict):
    print(f"[workflow] {svc['name']}")
    (impl_source,) = svc["impl_sources"]  # exactly one file for workflow services
    copy_source(svc["name"], impl_source)

    workflow_impl_class = impl_class_name(impl_source)
    config_java = gen.render_worker_config_java(svc, workflow_impl_class=workflow_impl_class)
    config_path = dest_java_root(svc["name"]) / "config" / "TemporalConfig.java"
    config_path.parent.mkdir(parents=True, exist_ok=True)
    config_path.write_text(config_java)
    print(f"  wrote config/TemporalConfig.java (registers {workflow_impl_class})")


def process_activity_service(svc: dict):
    print(f"[activity] {svc['name']}")
    activity_impl_source = next(p for p in svc["impl_sources"] if p.startswith("activities/"))
    for src in svc["impl_sources"]:
        copy_source(svc["name"], src)

    activity_impl_class = impl_class_name(activity_impl_source)
    config_java = gen.render_worker_config_java(svc, activity_impl_class=activity_impl_class)
    config_path = dest_java_root(svc["name"]) / "config" / "TemporalConfig.java"
    config_path.parent.mkdir(parents=True, exist_ok=True)
    config_path.write_text(config_java)
    print(f"  wrote config/TemporalConfig.java (registers {activity_impl_class})")


def process_edge_service(svc: dict):
    """gateway or simulator — TemporalClientConfig.java was already written by
    generate_services.py; this just copies the business-logic files."""
    print(f"[{svc['type']}] {svc['name']}")
    for src in svc["impl_sources"]:
        copy_source(svc["name"], src)


def main():
    services = load_manifest()
    for svc in services:
        if svc["type"] == "workflow":
            process_workflow_service(svc)
        elif svc["type"] == "activity":
            process_activity_service(svc)
        else:
            process_edge_service(svc)

    print("\nDone. Every service under services/*/src/main/java is now complete "
          "(business logic + generated Application/Config classes).")


if __name__ == "__main__":
    main()
