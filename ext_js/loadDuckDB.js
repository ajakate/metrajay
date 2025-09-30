import * as duckdb from "https://cdn.jsdelivr.net/npm/@duckdb/duckdb-wasm@1.30.0/+esm";
import { get, set } from 'https://cdn.jsdelivr.net/npm/idb-keyval@6/+esm';

console.log('Loading DuckDB in external JS file...')

async function instantiateDuckDB(duckdb) {
    const CDN_BUNDLES = duckdb.getJsDelivrBundles(),
        bundle = await duckdb.selectBundle(CDN_BUNDLES), // Select a bundle based on browser checks
        worker_url = URL.createObjectURL(
            new Blob([`importScripts("${bundle.mainWorker}");`], {
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

async function loadCsv(tableName, csvText, updateIndexedDB) {
    console.log('Calling loadCsv with args:', tableName, csvText, updateIndexedDB);
    const conn = window.dbConnection;
    const duckdb = window.DuckDB;
    const filename = `/tmp/${tableName}.csv`;
    await duckdb.registerFileText(filename, csvText);
    await conn.query(`CREATE OR REPLACE TABLE ${tableName} AS SELECT * FROM read_csv_auto('${filename}')`);
    if (updateIndexedDB) {
        await set(tableName, csvText);
    }
}

async function insertTable(tableName, csvText, updateIndexedDB, conn, duckdb) {
    console.log('Calling insertTable with args:', tableName, csvText);

    const filename = `/tmp/${tableName}.csv`;
    await duckdb.registerFileText(filename, csvText);

    const fullQuery = `CREATE OR REPLACE TABLE ${tableName} AS SELECT * FROM read_csv_auto('${filename}')`;
    console.log('Executing query', fullQuery);
    await conn.query(fullQuery);

    if (updateIndexedDB) {
        await set(tableName, csvText);
    }
}

function cleanCsvText(csvText) {
  return csvText
    // remove spaces around commas
    .replace(/\s*,\s*/g, ',')
    // remove spaces around newlines
    .replace(/\s*\n\s*/g, '\n')
    // trim start/end of whole file
    .trim();
}

async function loadCsvs(tableNamesAndCsvs) {
    console.log('Calling loadCsvs with args:', tableNamesAndCsvs);
    const conn = window.dbConnection;
    const duckdb = window.DuckDB;

    for (const [tableName, csvText] of tableNamesAndCsvs) {
        const cleanedCsvText = cleanCsvText(csvText);
        await insertTable(tableName, cleanedCsvText, true, conn, duckdb)
    }
}

async function loadCsvsFromIndexedDB(tableNames) {
    console.log('Calling loadCsvsFromIndexedDB with args:', tableNames);
    const conn = window.dbConnection;
    const duckdb = window.DuckDB;

    for (const tableName of tableNames) {
        const csvText = await get(tableName);
        await insertTable(tableName, csvText, false, conn, duckdb)
    }
}

async function checkIfKVExists(tableNames) {
    console.log('checking for keys:', tableNames);
    for (const tableName of tableNames) {
        const result = await get(tableName);
        if (!result) {
            return false;
        }
    }
    return true;
}

window.loadCsv = loadCsv;
window.loadCsvs = loadCsvs;
window.loadCsvsFromIndexedDB = loadCsvsFromIndexedDB;
window.checkIfKVExists = checkIfKVExists;

window.duckdbLoaded = true
