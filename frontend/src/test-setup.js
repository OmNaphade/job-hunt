import '@testing-library/jest-dom/vitest'

// jsdom does not implement matchMedia; several components (dark-mode detection)
// call it during initial render, so provide a minimal always-light-mode stub.
if (typeof window !== 'undefined' && !window.matchMedia) {
  window.matchMedia = (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}

// jsdom logs a "not implemented" error for scrollTo/scrollIntoView; AppLayout's
// route-change scroll behavior calls both, so stub them out for quiet test runs.
if (typeof window !== 'undefined') {
  window.scrollTo = () => {}
}
if (typeof Element !== 'undefined' && !Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}
