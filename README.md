# Simple Key-Value Store (Java)

A minimal persistent key-value store with **SET**, **GET**, and **EXIT** commands. Uses an append-only log (`data.db`) and a custom in-memory index (no `Map`).

**EUID**: tjt0134

## Build and run

From the project root:

```powershell
# Compile
.\build.bat

# Run interactively
java -cp out kvstore.Main
```

Or manually:

```powershell
javac -d out -encoding UTF-8 --release 8 src\kvstore\*.java
java -cp out kvstore.Main
```

## Commands (STDIN → STDOUT)

| Command   | Description                    |
|----------|---------------------------------|
| `SET k v`| Set key `k` to value `v`        |
| `GET k`  | Print value for `k` (or blank)  |
| `EXIT`   | Exit the program                |

Values may contain spaces (e.g. `SET name John Doe`).

## Gradebot / Black-box testing

1. Set **work directory** to this project root.
2. Set **command to run** to:
   ```
   java -cp out kvstore.Main
   ```
3. Build the project first (`.\build.bat`).
4. The tester pipes commands to STDIN; no manual input is required.

## Design

- **Index**: Custom `KeyValueIndex` — a list of `(key, value)` entries with linear scan. No `HashMap` or any built-in map type.
- **Persistence**: Every `SET` is immediately appended and flushed to `data.db`. On startup the log is replayed in order to rebuild the index (last-write-wins).
- **GET correctness**: In addition to the in-memory index, `GET` scans `data.db` directly so writes made by other processes are always visible.
