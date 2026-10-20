// Minimal stand-in for the external payment processor (contract: docs/LLD.md section 8).
//   POST /authorize  { reference, amount, currency }  ->  { reference, status, authCode }
// The decision is made from the cents of the amount, so tests can choose the outcome:
//   .13 -> DECLINED, .99 -> TIMEOUT, anything else -> APPROVED
// Usage: node mock-processor.js [port]   (default 18072, or MOCK_PROCESSOR_PORT)
const http = require('http');

function decide(amount) {
  const cents = Math.round(Number(amount) * 100) % 100;
  if (cents === 13) return 'DECLINED';
  if (cents === 99) return 'TIMEOUT';
  return 'APPROVED';
}

function createServer() {
  return http.createServer((req, res) => {
    if (req.method !== 'POST' || req.url !== '/authorize') {
      res.writeHead(404).end();
      return;
    }
    let raw = '';
    req.on('data', (chunk) => (raw += chunk));
    req.on('end', () => {
      let body;
      try {
        body = JSON.parse(raw);
      } catch {
        res.writeHead(400).end();
        return;
      }
      const status = decide(body.amount);
      const reply = {
        reference: body.reference,
        status,
        authCode: status === 'APPROVED' ? 'MOCK-' + String(body.reference).slice(0, 8) : null,
      };
      res.writeHead(200, { 'Content-Type': 'application/json' }).end(JSON.stringify(reply));
    });
  });
}

module.exports = { createServer, decide };

if (require.main === module) {
  const port = Number(process.argv[2] || process.env.MOCK_PROCESSOR_PORT || 18072);
  createServer().listen(port, () => console.log(`mock processor listening on http://localhost:${port}`));
}
