import { DAY_ORDER, DAY_SHORT_LABELS } from "../tuition/model/labels";
import type { TuitionSession } from "../tuition/model/tuition";

export function formatTime12h(hhmm: string): string {
  const [hStr, mStr] = hhmm.split(":");
  let hour = Number(hStr);
  const period = hour >= 12 ? "PM" : "AM";
  hour = hour % 12;
  if (hour === 0) hour = 12;
  return `${hour}:${mStr.padStart(2, "0")} ${period}`;
}

export function formatSessionTimeRange(session: TuitionSession): string {
  return `${formatTime12h(session.startTime)} – ${formatTime12h(session.endTime)}`;
}

// Monday-first "sensible order" for the class schedule and for picking a card's primary session.
export function sortSessions(sessions: TuitionSession[]): TuitionSession[] {
  return [...sessions].sort((a, b) => {
    const dayDiff = DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek);
    return dayDiff !== 0 ? dayDiff : a.startTime.localeCompare(b.startTime);
  });
}

export function primarySession(sessions: TuitionSession[]): TuitionSession | undefined {
  return sortSessions(sessions)[0];
}

export function formatSessionLocation(session: TuitionSession): string {
  if (session.deliveryMode === "ONLINE") return "Online";
  if (session.locationName && session.venueName) return `${session.locationName} · ${session.venueName}`;
  return session.locationName ?? session.venueName ?? "Physical";
}

export function formatSessionCardLine(session: TuitionSession): string {
  const day = DAY_SHORT_LABELS[session.dayOfWeek];
  return `${formatSessionLocation(session)} · ${day} ${formatSessionTimeRange(session)}`;
}
