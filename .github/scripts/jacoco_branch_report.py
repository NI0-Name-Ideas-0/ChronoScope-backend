#!/usr/bin/env python3
"""Prepare JaCoCo XML copies that madrapps/jacoco-report reads as branch coverage."""

from __future__ import annotations

import argparse
import shutil
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class TransformStats:
    counter_groups: int = 0
    branch_counter_groups: int = 0
    lines: int = 0
    branch_lines: int = 0

    def __add__(self, other: "TransformStats") -> "TransformStats":
        return TransformStats(
            counter_groups=self.counter_groups + other.counter_groups,
            branch_counter_groups=self.branch_counter_groups + other.branch_counter_groups,
            lines=self.lines + other.lines,
            branch_lines=self.branch_lines + other.branch_lines,
        )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def direct_counters(element: ET.Element) -> dict[str, ET.Element]:
    counters: dict[str, ET.Element] = {}
    for child in list(element):
        if local_name(child.tag) == "counter":
            counter_type = child.get("type")
            if counter_type:
                counters[counter_type] = child
    return counters


def int_attr(element: ET.Element, name: str) -> int:
    return int(element.get(name, "0"))


def transform_tree(root: ET.Element) -> TransformStats:
    stats = TransformStats()

    for element in root.iter():
        counters = direct_counters(element)
        instruction_counter = counters.get("INSTRUCTION")

        if instruction_counter is not None:
            stats += TransformStats(counter_groups=1)
            branch_counter = counters.get("BRANCH")

            if branch_counter is not None:
                instruction_counter.set("missed", branch_counter.get("missed", "0"))
                instruction_counter.set("covered", branch_counter.get("covered", "0"))
                stats += TransformStats(branch_counter_groups=1)
            else:
                instruction_counter.set("missed", "0")
                instruction_counter.set("covered", "0")

        if local_name(element.tag) == "line":
            missed_branches = int_attr(element, "mb")
            covered_branches = int_attr(element, "cb")

            element.set("mi", str(missed_branches))
            element.set("ci", str(covered_branches))
            stats += TransformStats(
                lines=1,
                branch_lines=1 if missed_branches + covered_branches > 0 else 0,
            )

    return stats


def branch_report_path(report_path: Path) -> Path:
    return report_path.with_name(f"{report_path.stem}-branch{report_path.suffix}")


def transform_report(report_path: Path) -> TransformStats:
    output_path = branch_report_path(report_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(report_path, output_path)

    tree = ET.parse(output_path)
    stats = transform_tree(tree.getroot())
    tree.write(output_path, encoding="utf-8", xml_declaration=True)

    print(
        f"Prepared {output_path} from {report_path} "
        f"({stats.branch_counter_groups}/{stats.counter_groups} counter groups had branches, "
        f"{stats.branch_lines}/{stats.lines} lines had branch counters)"
    )
    return stats


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Create JaCoCo XML copies where INSTRUCTION counters and line mi/ci "
            "attributes contain branch coverage values for madrapps/jacoco-report."
        )
    )
    parser.add_argument("reports", nargs="+", type=Path, help="JaCoCo XML reports to copy and transform")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    total = TransformStats()

    for report_path in args.reports:
        if not report_path.is_file():
            print(f"Missing JaCoCo report: {report_path}", file=sys.stderr)
            return 1

        total += transform_report(report_path)

    if total.branch_counter_groups == 0 and total.branch_lines == 0:
        print("No branch coverage counters were found in the supplied JaCoCo reports.", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
