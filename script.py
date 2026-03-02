import os
import random
import subprocess
from collections import defaultdict, Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Optional, Tuple, List

# --- Configuration ---
JAVA_HOME = r"C:\Program Files\Java\jdk1.8.0_202"
ANT_EXE = r"C:\Users\alexv\Downloads\apache-ant-1.10.15-bin\apache-ant-1.10.15\bin\ant.bat"

BUILD_DEFAULTS_FILE = "build.defaults"   # no longer edited by this script
MAPS_FOLDER = "maps"
LOGS_FOLDER = "logs"
RUNS_PER_MAP = 3

# Players
PACKAGE1 = "myplayer_curr"
PACKAGE2 = "myplayer_last"

# Parallelism
MAX_WORKERS = max(1, (os.cpu_count() or 4) - 1)  # leave 1 core free


def _base_env() -> dict:
    """Prepare a base environment (JAVA_HOME + PATH) once."""
    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_HOME
    env["PATH"] = env["PATH"] + ";" + os.path.join(JAVA_HOME, "bin")
    return env


def ant_prepare(env: dict) -> None:
    """
    Compile & instrument once. Uses -Dpackage1/-Dpackage2 so we don't touch build.defaults.
    """
    cmd = [
        ANT_EXE, "prepare",
        f"-Dpackage1={PACKAGE1}",
        f"-Dpackage2={PACKAGE2}",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, env=env)
    if result.returncode != 0:
        raise RuntimeError(
            f"ant prepare failed ({result.returncode})\nSTDERR:\n{result.stderr}\nSTDOUT:\n{result.stdout}"
        )


def parse_winner_and_condition_from_tail(stdout: str, stderr: str) -> Tuple[Optional[str], Optional[str]]:
    """
    Scan from the bottom for 'Winner:' and 'WinCondition:' lines, ignoring extra lines
    like 'BUILD SUCCESSFUL' or 'Total time'.
    """
    combined = (stdout + "\n" + stderr).splitlines()
    winner = None
    win_condition = None

    # Walk bottom-up to find both lines
    for line in reversed(combined):
        stripped = line.strip()
        if "WinCondition:" in stripped:
            win_condition = stripped.split("WinCondition:", 1)[1].strip() or None
        elif "Winner:" in stripped:
            name = stripped.split("Winner:", 1)[1].split("(", 1)[0].strip()
            if name in (PACKAGE1, PACKAGE2):
                winner = name

        # Stop if we have both
        if winner and win_condition:
            break

    return winner, win_condition


def run_one_game(map_name: str, run_number: int, seed: int, env: dict) -> Tuple[str, int, int, Optional[str], Optional[str], int, str]:
    """
    Launch a single 'ant run_only' for (map, seed). Returns a tuple:
      (map_name, run_number, seed, winner, win_condition, returncode, log_path)
    """
    log_filename = os.path.join(LOGS_FOLDER, f"{map_name}_run{run_number}.txt")
    Path(LOGS_FOLDER).mkdir(parents=True, exist_ok=True)

    cmd = [
        ANT_EXE, "run_only",
        f"-Dpackage1={PACKAGE1}",
        f"-Dpackage2={PACKAGE2}",
        f"-Dmap={map_name}",
        f"-Dseed={seed}",
    ]

    # Run Ant
    result = subprocess.run(cmd, capture_output=True, text=True, env=env)

    # Write log
    with open(log_filename, "w", encoding="utf-8") as log_file:
        # persist stdout first for readability, then stderr
        log_file.write(result.stdout)
        if result.stderr:
            log_file.write("\n--- STDERR ---\n")
            log_file.write(result.stderr)

    # Parse last two lines (winner + condition)
    winner, win_condition = parse_winner_and_condition_from_tail(result.stdout, result.stderr)

    return map_name, run_number, seed, winner, win_condition, result.returncode, log_filename


def discover_maps(maps_folder: str) -> List[str]:
    return [
        os.path.splitext(f)[0]
        for f in os.listdir(maps_folder)
        if f.endswith(".txt")
    ]


def main():
    env = _base_env()

    # 1) Prepare build once (compile + instrument once for both packages)
    print("Preparing build (compile + instrument) ...")
    ant_prepare(env)
    print("Prepare done.\n")

    # 2) Create jobs
    maps = discover_maps(MAPS_FOLDER)
    if not maps:
        print(f"No maps found in {MAPS_FOLDER}")
        return

    maps.reverse()

    rng = random.SystemRandom()
    jobs = []
    for map_name in maps:
        for run_number in range(1, RUNS_PER_MAP + 1):
            seed = rng.randint(1, 10_000_000)
            jobs.append((map_name, run_number, seed))

    # 3) Run in parallel
    print(f"Running {len(jobs)} games across {len(maps)} maps with up to {MAX_WORKERS} workers ...\n")

    results = []
    failures = 0
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as ex:
        fut_to_job = {
            ex.submit(run_one_game, map_name, run_number, seed, env): (map_name, run_number, seed)
            for (map_name, run_number, seed) in jobs
        }
        for fut in as_completed(fut_to_job):
            map_name, run_number, seed = fut_to_job[fut]
            try:
                res = fut.result()
                results.append(res)
                _, _, _, winner, win_condition, rc, log_path = res
                status = "OK" if rc == 0 else f"ANT_RC={rc}"
                if winner:
                    print(f"[{status}] {map_name} | Run {run_number} | Seed {seed} | Winner: {winner} | Condition: {win_condition} | Log: {log_path}")
                else:
                    print(f"[{status}] {map_name} | Run {run_number} | Seed {seed} | Winner not found | Log: {log_path}")
            except Exception as e:
                failures += 1
                print(f"[EXC] {map_name} | Run {run_number} | Seed {seed} | {e!r}")

    # 4) Aggregate
    total_games = len(results)
    total_wins = Counter()
    per_map_wins = defaultdict(lambda: Counter())
    conditions = Counter()

    for map_name, run_number, seed, winner, win_condition, rc, log_path in results:
        if winner:
            total_wins[winner] += 1
            per_map_wins[map_name][winner] += 1
        if win_condition:
            conditions[win_condition] += 1

    # 5) Summary
    print("\n===== OVERALL RESULTS =====")
    print(f"Total games: {total_games}  (failures: {failures})")
    for player in (PACKAGE1, PACKAGE2):
        wins = total_wins[player]
        percent = (wins / total_games) * 100 if total_games else 0.0
        print(f"{player} wins: {wins} ({percent:.2f}%)")

    if conditions:
        print("\n===== WIN CONDITIONS =====")
        for cond, cnt in conditions.most_common():
            pct = (cnt / total_games) * 100 if total_games else 0.0
            print(f"{cond}: {cnt} ({pct:.2f}%)")

    print("\n===== PER-MAP RESULTS =====")
    for map_name in sorted(per_map_wins.keys()):
        results_map = per_map_wins[map_name]
        map_total = sum(results_map.values())
        print(f"Map: {map_name} ({map_total} games)")
        for player in (PACKAGE1, PACKAGE2):
            wins = results_map[player]
            percent = (wins / map_total) * 100 if map_total else 0.0
            print(f"  {player}: {wins} wins ({percent:.2f}%)")


if __name__ == "__main__":
    main()
