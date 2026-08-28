let sharedContext: AudioContext | null = null;

/** Reused across calls — browsers cap how many AudioContexts a page may create. */
function getAudioContext(): AudioContext | null {
  const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  if (!sharedContext) sharedContext = new Ctor();
  return sharedContext;
}

/**
 * A short two-note chime, synthesized rather than loaded from an audio file — no asset to ship,
 * ship over the network, or keep in sync with licensing. Silently no-ops if the browser blocks
 * audio before any user gesture has happened on the page (autoplay policy), which is expected and
 * fine: the visual popup still carries the notification.
 */
export function playNotificationSound() {
  const ctx = getAudioContext();
  if (!ctx) return;
  if (ctx.state === 'suspended') ctx.resume().catch(() => {});

  const now = ctx.currentTime;
  const notes: [frequency: number, start: number][] = [
    [880, 0],
    [1318.5, 0.11],
  ];

  notes.forEach(([frequency, start]) => {
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.value = frequency;

    const noteStart = now + start;
    const noteEnd = noteStart + 0.18;
    gain.gain.setValueAtTime(0, noteStart);
    gain.gain.linearRampToValueAtTime(0.18, noteStart + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, noteEnd);

    oscillator.connect(gain);
    gain.connect(ctx.destination);
    oscillator.start(noteStart);
    oscillator.stop(noteEnd + 0.02);
  });
}
