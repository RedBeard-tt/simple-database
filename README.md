# Simple Key-Value Store (Java)

A minimal persistent key-value store with **SET**, **GET**, and **EXIT** commands. Uses an append-only log (`data.db`) and a custom in-memory index (no `Map`).

## Build and run

From the project root (e.g. `simple database`):

```bash
# Compile
javac -d out -encoding UTF-8 --release 8 src/kvstore/*.java

# Run (interactive or piped input for Gradebot)
java -cp out kvstore.Main
```

On Windows (PowerShell):

```powershell
javac -d out -encoding UTF-8 --release 8 src/kvstore/*.java
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

1. Set **work directory** to this project root (where `src/` and `out/` live).
2. Set **command to run** to:
   ```text
   java -cp out kvstore.Main
   ```
3. Ensure the project is built first (`javac -d out -encoding UTF-8 --release 8 src/kvstore/*.java`).
4. The tester will pipe commands to STDIN; no manual input is required.

## Design

- **Index**: Custom `KeyValueIndex` (list of key-value entries, linear scan). No `HashMap`/`Map`.
- **Persistence**: Append-only writes to `data.db`; each line is `SET key value`. On startup, the log is replayed to rebuild the index (last-write-wins).
- **EUID**: tjt0134
