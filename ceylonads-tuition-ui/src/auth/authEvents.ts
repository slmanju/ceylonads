export const AUTH_CLEARED_EVENT = "ceylonads-tuition:auth-cleared";

export function emitAuthCleared(): void {
  window.dispatchEvent(new Event(AUTH_CLEARED_EVENT));
}
