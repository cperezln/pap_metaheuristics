"""
Aggregates per-instance CSV results produced by centrality_heuristics.py
(results_centrality/<instance>.csv) into a single Excel workbook with
pivot tables: instances x algorithms, one sheet for objective (seed size)
and one for time (s).
"""
import glob
import os

import pandas as pd

path = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = f"{path}/results_centrality"
OUT_XLSX = f"{path}/results_centrality/centrality_heuristics_results.xlsx"


def main():
    csv_files = sorted(glob.glob(f"{RESULTS_DIR}/*.csv"))
    if not csv_files:
        print(f"No CSV files found in {RESULTS_DIR}")
        return

    df = pd.concat((pd.read_csv(f) for f in csv_files), ignore_index=True)

    summary = df.pivot(index="instance", columns="algorithm",
                        values=["objective", "time_s", "feasible"])
    # group by algorithm first, then metric: (BC, objective), (BC, time_s), (BC, feasible), ...
    summary = summary.swaplevel(0, 1, axis=1).sort_index(axis=1, level=0)

    with pd.ExcelWriter(OUT_XLSX, engine="openpyxl") as writer:
        summary.to_excel(writer, sheet_name="summary")
        df.to_excel(writer, sheet_name="raw", index=False)

    print(f"Written {OUT_XLSX}")
    print("\nSummary:")
    print(summary)


if __name__ == "__main__":
    main()
