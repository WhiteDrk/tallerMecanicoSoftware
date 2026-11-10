"""Extrae tiempos HTTP observados de los reportes locales de esta practica."""

import json
import math
import re
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path


class EmbeddedData(HTMLParser):
    def __init__(self):
        super().__init__()
        self.active = False
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag == "script":
            self.active = dict(attrs).get("id") == "karate-data"

    def handle_endtag(self, tag):
        if tag == "script":
            self.active = False

    def handle_data(self, value):
        if self.active:
            self.parts.append(value)


def data_from(path):
    parser = EmbeddedData()
    parser.feed(path.read_text(encoding="utf-8"))
    return json.loads("".join(parser.parts))


def summarize(folder):
    summary = data_from(folder / "karate-summary.html")["summary"]
    times = []
    statuses = Counter()
    for feature_path in (folder / "feature-html").glob("*.html"):
        feature = data_from(feature_path)
        for scenario in feature.get("scenarios", []):
            for step in scenario.get("steps", []):
                if step.get("keyword") != "method":
                    continue
                log = "\n".join(segment.get("text", "") for segment in step.get("logSegments", []))
                timing = re.search(r"response time in milliseconds: (\d+)", log)
                status = re.search(r"^\d+ < (\d{3})\b", log, re.MULTILINE)
                if timing:
                    times.append(int(timing.group(1)))
                if status:
                    statuses[status.group(1)] += 1
    times.sort()
    return {
        "reporte": str(folder / "karate-summary.html"),
        "escenarios": summary["scenario_count"],
        "aprobados": summary["scenario_passed"],
        "fallidos": summary["scenario_failed"],
        "duracion_suite_ms": summary["duration_millis"],
        "respuestas_con_tiempo": len(times),
        "estados_http": dict(statuses),
        "http_promedio_ms": round(sum(times) / len(times), 2) if times else None,
        "http_p95_ms": times[math.ceil(0.95 * len(times)) - 1] if times else None,
        "http_maximo_ms": max(times) if times else None,
    }


if __name__ == "__main__":
    target = Path(__file__).resolve().parent / "target"
    results = []
    folders = sorted(
        (p for p in target.glob("concurrencia-*") if p.is_dir() and p.name.rsplit("-", 1)[-1].isdigit()),
        key=lambda p: int(p.name.rsplit("-", 1)[-1]),
    )
    for folder in folders:
        threads = int(folder.name.rsplit("-", 1)[-1])
        if not (folder / "karate-summary.html").exists():
            continue
        result = {"concurrencia_maxima_configurada": threads, **summarize(folder)}
        results.append(result)
    output = target / "resumen-concurrencia.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(results, ensure_ascii=False, indent=2))
