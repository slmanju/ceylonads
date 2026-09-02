import { FaHome, FaMapMarkerAlt } from "react-icons/fa";
import type { HomeVisitInfo } from "../../tuition/model/tuition";
import { CLASS_FORMAT_LABELS } from "../../tuition/model/labels";
import "./HomeVisitPanel.css";

interface HomeVisitPanelProps {
  homeVisit?: HomeVisitInfo;
}

export function HomeVisitPanel({ homeVisit }: HomeVisitPanelProps) {
  if (!homeVisit?.available) return null;

  return (
    <section className="home-visit-panel">
      <h2>
        <FaHome aria-hidden="true" /> Home Visit Available
      </h2>

      {homeVisit.serviceAreas.length > 0 && (
        <div className="home-visit-panel__group">
          <p className="home-visit-panel__label">Service Areas</p>
          <div className="home-visit-panel__chips">
            {homeVisit.serviceAreas.map((area) => (
              <span key={area.locationName} className="home-visit-panel__chip">
                <FaMapMarkerAlt aria-hidden="true" /> {area.locationName}
              </span>
            ))}
          </div>
        </div>
      )}

      {homeVisit.classFormats.length > 0 && (
        <p className="home-visit-panel__class-types">{homeVisit.classFormats.map((f) => CLASS_FORMAT_LABELS[f]).join(" / ")}</p>
      )}
    </section>
  );
}
