export const GATEWAY_BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:8080';
export const MCP_SERVER_BASE_URL = process.env.E2E_MCP_BASE_URL ?? 'http://localhost:8087';

export const NIL_UUID = '00000000-0000-0000-0000-000000000000';

/** Unique per run so repeat smoke runs never collide on "email already registered". */
export function uniqueTestEmail(): string {
  return `smoke-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}
