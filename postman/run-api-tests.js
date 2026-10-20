#!/usr/bin/env node
// Runs the Finthos Postman collection end-to-end with Newman.
//
//   1. starts the mock payment processor (mock-processor.js)
//   2. starts the app with ./mvnw spring-boot:run, pointed at the mock
//      (Spring's Docker Compose support starts PostgreSQL from docker-compose.yaml)
//   3. runs folder "00 Setup", promotes the merchant/admin users in the database,
//      then runs every other folder in order
//   4. writes CLI + JUnit + JSON reports to postman/reports and stops everything it started
//
// Options:
//   --port N          port for the app this script starts (default 8080), e.g. --port 18080
//                     to run next to an app you already have on 8080
//   --no-start        use an app that is already running (it must use the mock processor URL)
//   --base-url URL    app URL for --no-start (default http://localhost:8080)
//
// Environment overrides: FINTHOS_DB_URL (default postgres://finthos:finthos@localhost:5432/finthos),
// MOCK_PROCESSOR_PORT (default 18072), APP_START_TIMEOUT_MS (default 240000).
const path = require('path');
const fs = require('fs');
const { spawn, spawnSync } = require('child_process');
const newman = require('newman');
const { Client } = require('pg');
const { createServer } = require('./mock-processor');

const ROOT = path.resolve(__dirname, '..');
const COLLECTION = path.join(__dirname, 'finthos.postman_collection.json');
const ENVIRONMENT = path.join(__dirname, 'finthos.local.postman_environment.json');
const REPORTS = path.join(__dirname, 'reports');
const SETUP_FOLDER = '00 Setup';

const args = process.argv.slice(2);
const startApp = !args.includes('--no-start');
const appPort = Number(valueOf('--port') || 8080);
const baseUrl = valueOf('--base-url') || `http://localhost:${appPort}`;
const mockPort = Number(process.env.MOCK_PROCESSOR_PORT || 18072);
const dbUrl = process.env.FINTHOS_DB_URL || 'postgres://finthos:finthos@localhost:5432/finthos';
const startTimeoutMs = Number(process.env.APP_START_TIMEOUT_MS || 240000);

function valueOf(flag) {
  const i = args.indexOf(flag);
  return i >= 0 ? args[i + 1] : undefined;
}

function log(msg) {
  console.log(`[run-api-tests] ${msg}`);
}

async function isUp(url) {
  try {
    // Any HTTP answer (even 400/401) means the app is serving requests.
    await fetch(url + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' });
    return true;
  } catch {
    return false;
  }
}

async function waitForApp(url, app) {
  const deadline = Date.now() + startTimeoutMs;
  while (Date.now() < deadline) {
    if (app && app.exitCode !== null) throw new Error(`app exited with code ${app.exitCode} (see ${REPORTS}/app.log)`);
    if (await isUp(url)) return;
    await new Promise((r) => setTimeout(r, 2000));
  }
  throw new Error(`app did not start within ${startTimeoutMs / 1000}s (see ${REPORTS}/app.log)`);
}

function launchApp() {
  const mvnw = path.join(ROOT, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
  const out = fs.openSync(path.join(REPORTS, 'app.log'), 'w');
  log(`starting app: ${path.basename(mvnw)} clean spring-boot:run (log: postman/reports/app.log)`);
  // clean: Maven never deletes resources removed from src/, so a stale migration in
  // target/classes would make Flyway fail ("more than one migration with version 1").
  return spawn(mvnw, ['-q', 'clean', 'spring-boot:run'], {
    cwd: ROOT,
    env: { ...process.env, SERVER_PORT: String(appPort), PROCESSOR_BASE_URL: `http://localhost:${mockPort}` },
    stdio: ['ignore', out, out],
    shell: process.platform === 'win32', // .cmd files need a shell on Windows
    detached: process.platform !== 'win32', // own process group so we can stop Maven + the JVM
  });
}

function stopApp(app) {
  if (!app || app.exitCode !== null) return;
  log('stopping app');
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/pid', String(app.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    try {
      process.kill(-app.pid, 'SIGTERM');
    } catch {
      /* already gone */
    }
  }
}

function runNewman(options) {
  return new Promise((resolve, reject) => {
    newman.run(
      {
        collection: COLLECTION,
        timeoutRequest: 30000,
        reporters: ['cli', 'junit', 'json'],
        ...options,
      },
      (err, summary) => (err ? reject(err) : resolve(summary)),
    );
  });
}

function envValue(summary, key) {
  const v = summary.environment.values.find((x) => x.key === key);
  return v && v.value;
}

async function promoteRoles(merchantId, adminId) {
  const client = new Client({ connectionString: dbUrl });
  await client.connect();
  try {
    await client.query("UPDATE users SET role = 'MERCHANT' WHERE id = $1", [merchantId]);
    await client.query("UPDATE users SET role = 'ADMIN' WHERE id = $1", [adminId]);
  } finally {
    await client.end();
  }
  log(`promoted merchant ${merchantId} and admin ${adminId}`);
}

function folderNames() {
  return JSON.parse(fs.readFileSync(COLLECTION, 'utf8')).item.map((f) => f.name).filter((n) => n !== SETUP_FOLDER);
}

function failures(summary) {
  return summary.run.failures.length + (summary.error ? 1 : 0);
}

async function main() {
  fs.mkdirSync(REPORTS, { recursive: true });
  let app;
  const mock = createServer().listen(mockPort);
  log(`mock processor on http://localhost:${mockPort}`);
  try {
    if (startApp) {
      if (await isUp(baseUrl)) {
        throw new Error(`something is already serving ${baseUrl}; use --port <other port> to start a separate instance, or --no-start to test that one`);
      }
      if (spawnSync('docker', ['version', '--format', '{{.Server.Version}}'], { stdio: 'ignore' }).status !== 0) {
        throw new Error('Docker is not running; start Docker Desktop (PostgreSQL comes from docker-compose.yaml)');
      }
      app = launchApp();
    }
    await waitForApp(baseUrl, app);
    log(`app is up at ${baseUrl}`);

    const envVars = [{ key: 'baseUrl', value: baseUrl }];
    const setup = await runNewman({
      environment: ENVIRONMENT,
      envVar: envVars,
      folder: SETUP_FOLDER,
      reporter: { junit: { export: path.join(REPORTS, 'setup-junit.xml') }, json: { export: path.join(REPORTS, 'setup.json') } },
    });
    if (failures(setup) > 0) {
      log('setup failed; skipping the remaining folders');
      return 1;
    }
    await promoteRoles(envValue(setup, 'merchantId'), envValue(setup, 'adminId'));

    const main = await runNewman({
      environment: setup.environment.toJSON(),
      envVar: envVars,
      folder: folderNames(),
      reporter: { junit: { export: path.join(REPORTS, 'junit.xml') }, json: { export: path.join(REPORTS, 'results.json') } },
    });
    const failed = failures(main);
    log(failed === 0 ? 'all checks passed' : `${failed} failure(s); reports in postman/reports`);
    return failed === 0 ? 0 : 1;
  } finally {
    stopApp(app);
    mock.close();
  }
}

main().then(
  (code) => process.exit(code),
  (err) => {
    console.error(`[run-api-tests] ${err.message}`);
    process.exit(1);
  },
);
