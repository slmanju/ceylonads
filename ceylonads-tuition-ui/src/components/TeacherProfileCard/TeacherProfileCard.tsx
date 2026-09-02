import { FaChalkboardTeacher, FaUniversity } from "react-icons/fa";
import type { TeacherProfile } from "../../tuition/model/tuition";
import { TeacherProfileBadge } from "../Badge/Badge";
import "./TeacherProfileCard.css";

interface TeacherProfileCardProps {
  teacher?: TeacherProfile;
  name: string;
}

export function TeacherProfileCard({ teacher, name }: TeacherProfileCardProps) {
  if (!teacher) return null;
  const AvatarIcon = teacher.profileType === "INSTITUTE" ? FaUniversity : FaChalkboardTeacher;

  return (
    <section className="teacher-profile-card">
      <h2 className="teacher-profile-card__heading">About the Tutor</h2>
      <div className="teacher-profile-card__main">
        <div className="teacher-profile-card__avatar" aria-hidden="true">
          <AvatarIcon />
        </div>

        <div className="teacher-profile-card__info">
          <div className="teacher-profile-card__header">
            <p className="teacher-profile-card__name">{name}</p>
            <TeacherProfileBadge profileType={teacher.profileType} />
          </div>

          {(typeof teacher.experienceYears === "number" || teacher.teachingSince) && (
            <div className="teacher-profile-card__stats">
              {typeof teacher.experienceYears === "number" && <span>{teacher.experienceYears}+ years experience</span>}
              {teacher.teachingSince && <span>Teaching since {teacher.teachingSince}</span>}
            </div>
          )}

          {teacher.affiliation && <p className="teacher-profile-card__affiliation">{teacher.affiliation}</p>}

          {teacher.qualifications && teacher.qualifications.length > 0 && (
            <ul className="teacher-profile-card__qualifications">
              {teacher.qualifications.map((qualification) => (
                <li key={qualification}>{qualification}</li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {teacher.shortBio && <p className="teacher-profile-card__bio">{teacher.shortBio}</p>}
    </section>
  );
}
