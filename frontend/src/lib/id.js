// crypto.randomUUID() only exists in secure contexts (HTTPS or localhost) — plain-HTTP
// deployments (e.g. an IP address with no TLS yet) don't have it. These IDs are only used
// for React list keys and toast identity, not anything security-sensitive, so a
// Math.random()-based fallback is fine when the native API is unavailable.
export function generateId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const rand = (Math.random() * 16) | 0
    const value = char === 'x' ? rand : (rand & 0x3) | 0x8
    return value.toString(16)
  })
}
