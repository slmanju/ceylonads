import type { ClassFormat, ClassPurpose } from "../../tuition/model/tuition";
import { CLASS_FORMAT_LABELS, CLASS_FORMAT_ORDER, CLASS_PURPOSE_LABELS, CLASS_PURPOSE_ORDER } from "../../tuition/model/labels";
import "./SubjectFilters.css";

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

interface AdvancedFilterFieldsProps {
  classFormats: ClassFormat[];
  classPurposes: ClassPurpose[];
  onChange: (classFormats: ClassFormat[], classPurposes: ClassPurpose[]) => void;
}

// "More Filters" secondary fields (Class Format, Class Purpose) - shared by the desktop More
// Filters popover and the mobile filter drawer's collapsible section.
export function AdvancedFilterFields({ classFormats, classPurposes, onChange }: AdvancedFilterFieldsProps) {
  return (
    <>
      <div className="subject-filters__group">
        <span className="subject-filters__label">Class Format</span>
        <div className="subject-filters__checkbox-group">
          {CLASS_FORMAT_ORDER.map((format) => (
            <label key={format} className="subject-filters__checkbox">
              <input
                type="checkbox"
                checked={classFormats.includes(format)}
                onChange={() => onChange(toggle(classFormats, format), classPurposes)}
              />
              {CLASS_FORMAT_LABELS[format]}
            </label>
          ))}
        </div>
      </div>

      <div className="subject-filters__group">
        <span className="subject-filters__label">Class Purpose</span>
        <div className="subject-filters__checkbox-group">
          {CLASS_PURPOSE_ORDER.map((purpose) => (
            <label key={purpose} className="subject-filters__checkbox">
              <input
                type="checkbox"
                checked={classPurposes.includes(purpose)}
                onChange={() => onChange(classFormats, toggle(classPurposes, purpose))}
              />
              {CLASS_PURPOSE_LABELS[purpose]}
            </label>
          ))}
        </div>
      </div>
    </>
  );
}
