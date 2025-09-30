import duckdb
from IPython import embed
# Connect to a persistent DuckDB file
con = duckdb.connect("metrajay.duckdb")

def load_csv_cleaned(table_name: str, csv_path: str, cleaned_path: str = None):
    if cleaned_path is None:
        cleaned_path = f"cleaned_{csv_path}"

    # 1. Read original CSV and clean whitespace
    with open(csv_path, "r", encoding="utf-8") as fin, open(cleaned_path, "w", encoding="utf-8") as fout:
        for line in fin:
            # Strip whitespace around each cell
            parts = [p.strip() for p in line.split(",")]
            fout.write(",".join(parts) + "\n")

    # 2. Load cleaned CSV into DuckDB
    a = con.sql(f"""
        CREATE OR REPLACE TABLE {table_name} AS
        SELECT * FROM read_csv_auto('{cleaned_path}')
    """)

    print(a)

    print(f"Loaded {cleaned_path} into table {table_name}")


# Example usage
load_csv_cleaned("stops", "schedule/stops.txt")
load_csv_cleaned("stop_times", "schedule/stop_times.txt")
load_csv_cleaned("calendar_dates", "schedule/calendar_dates.txt")
load_csv_cleaned("calendar", "schedule/calendar.txt")
load_csv_cleaned("routes", "schedule/routes.txt")
load_csv_cleaned("trips", "schedule/trips.txt")

# Verify tables exist
print(con.execute("SHOW TABLES").fetchall())
