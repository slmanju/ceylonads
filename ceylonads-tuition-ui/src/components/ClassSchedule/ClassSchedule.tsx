import { FaLaptop, FaMapMarkerAlt, FaRegClock } from "react-icons/fa";
import type { TuitionSession } from "../../tuition/model/tuition";
import { CLASS_FORMAT_LABELS, CLASS_PURPOSE_LABELS, DAY_LABELS, DELIVERY_MODE_LABELS } from "../../tuition/model/labels";
import { formatSessionLocation, formatSessionTimeRange, sortSessions } from "../../utils/formatSchedule";
import "./ClassSchedule.css";

interface ClassScheduleProps {
  sessions: TuitionSession[];
  /** Real ad locations not already represented by a session's venue - shown as a compact
   *  "also available at" line instead of a separate, duplicate locations section. */
  extraLocations?: string[];
}

export function ClassSchedule({ sessions, extraLocations = [] }: ClassScheduleProps) {
  if (sessions.length === 0) return null;
  const sorted = sortSessions(sessions);

  return (
    <section className="class-schedule">
      <h2>Schedule &amp; Locations</h2>
      <ul className="class-schedule__list">
        {sorted.map((session) => (
          <li key={session.id} className="class-schedule__item">
            <div className="class-schedule__location">
              {session.deliveryMode === "ONLINE" ? <FaLaptop aria-hidden="true" /> : <FaMapMarkerAlt aria-hidden="true" />}
              {formatSessionLocation(session)}
            </div>
            <div className="class-schedule__time">
              <FaRegClock aria-hidden="true" />
              {DAY_LABELS[session.dayOfWeek]} · {formatSessionTimeRange(session)}
            </div>
            <div className="class-schedule__tags">
              <span className="class-schedule__tag class-schedule__tag--mode">{DELIVERY_MODE_LABELS[session.deliveryMode]}</span>
              {session.classFormats.map((format) => (
                <span key={format} className="class-schedule__tag">
                  {CLASS_FORMAT_LABELS[format]}
                </span>
              ))}
              {session.classPurposes.map((purpose) => (
                <span key={purpose} className="class-schedule__tag">
                  {CLASS_PURPOSE_LABELS[purpose]}
                </span>
              ))}
            </div>
          </li>
        ))}
      </ul>

      {extraLocations.length > 0 && (
        <p className="class-schedule__extra">
          <FaMapMarkerAlt aria-hidden="true" /> Also available at {extraLocations.join(", ")}
        </p>
      )}
    </section>
  );
}
