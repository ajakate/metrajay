import * as duckdb from "https://cdn.jsdelivr.net/npm/@duckdb/duckdb-wasm@1.30.0/+esm";

console.log('in le script')

async function instantiateDuckDB(duckdb) {
  const CDN_BUNDLES = duckdb.getJsDelivrBundles(),
  bundle = await duckdb.selectBundle(CDN_BUNDLES), // Select a bundle based on browser checks
  worker_url = URL.createObjectURL(
    new Blob([ `importScripts("${bundle.mainWorker}");` ], {
      type: "text/javascript"
    })
  );

  // Instantiate the async version of DuckDB-WASM
  const worker = new Worker(worker_url),
  logger = new duckdb.ConsoleLogger("DEBUG"),
  db = new duckdb.AsyncDuckDB(logger, worker);

  await db.instantiate(bundle.mainModule, bundle.pthreadWorker);
  URL.revokeObjectURL(worker_url);

  return db;
}

window.DuckDB = await instantiateDuckDB(duckdb)

window.dbConnection = await DuckDB.connect();
