#!/usr/bin/env python3
from pathlib import Path
import re
import shutil


ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "build/rtl/ICacheFormal"
DST = ROOT / "src/formal/icache/generated"


def strip_error_actions(text: str) -> str:
    out = []
    for line in text.splitlines():
        line = re.sub(r"\s+else\s+\$error\(.*\);", ";", line)
        if line.lstrip().startswith("else $error("):
            if out and not out[-1].rstrip().endswith(";"):
                out[-1] = out[-1].rstrip() + ";"
            continue
        out.append(line)
    return "\n".join(out) + "\n"


def module_body(text: str) -> str:
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.lstrip().startswith("module "))
    end = max(i for i, line in enumerate(lines) if line.strip() == "endmodule")
    return "\n".join(lines[start + 1 : end])


def declared_names(body: str) -> set[str]:
    names = set()
    for line in body.splitlines():
        match = re.match(r"\s*(?:reg|wire)\s+(?:\[[^\]]+\]\s+)?([A-Za-z_][A-Za-z0-9_$]*)\b", line)
        if match:
            names.add(match.group(1))
    return names


def flatten_body(path: Path, prefix: str) -> str:
    body = strip_error_actions(module_body(path.read_text()))
    body = body.replace("ICacheFormalHarness.verification.", "verif_")
    body = body.replace("ICacheFormalHarness.", "")

    for name in sorted(declared_names(body), key=len, reverse=True):
        body = re.sub(rf"\b{re.escape(name)}\b", f"{prefix}{name}", body)

    return "\n".join("  " + line if line else "" for line in body.splitlines())


def flatten_harness() -> str:
    top = (SRC / "ICacheFormalHarness.sv").read_text()
    head, _, tail = top.rpartition("endmodule")
    if not tail.strip() == "":
        raise RuntimeError("unexpected text after ICacheFormalHarness endmodule")

    verification = "\n\n".join(
        [
            flatten_body(SRC / "verification/ICacheFormalHarness_Verification.sv", "verif_"),
            flatten_body(SRC / "verification/assume/ICacheFormalHarness_Verification_Assume.sv", "assume_"),
            flatten_body(SRC / "verification/assert/ICacheFormalHarness_Verification_Assert.sv", "assert_"),
            flatten_body(SRC / "verification/cover/ICacheFormalHarness_Verification_Cover.sv", "cover_"),
        ]
    )
    reset_assumption = "\n  // Formal starts in reset; Chisel RegInit values are reset-state contracts, not arbitrary init-state contracts.\n  initial assume(reset);\n"
    return (
        head.rstrip()
        + reset_assumption
        + "\n  // Flattened Chisel verification layer for Yosys/SymbiYosys.\n"
        + verification
        + "\nendmodule\n"
    )


def main() -> None:
    if DST.exists():
        shutil.rmtree(DST)
    DST.mkdir(parents=True)

    for name in ["CacheSet.sv", "ICache.sv"]:
        shutil.copyfile(SRC / name, DST / name)

    (DST / "ICacheFormalHarness.sv").write_text(flatten_harness())


if __name__ == "__main__":
    main()
