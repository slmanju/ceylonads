import type { IconType } from "react-icons";
import {
  FaCar,
  FaHome,
  FaMobileAlt,
  FaLaptop,
  FaGraduationCap,
  FaBriefcase,
  FaIndustry,
  FaTools,
  FaCouch,
  FaTshirt,
  FaSeedling,
  FaPaw,
  FaFutbol,
  FaShoppingBasket,
  FaUtensils,
  FaTags,
} from "react-icons/fa";

const ICONS_BY_SLUG: Record<string, IconType> = {
  mobiles: FaMobileAlt,
  "mobile-phones": FaMobileAlt,
  electronics: FaLaptop,
  vehicles: FaCar,
  cars: FaCar,
  property: FaHome,
  houses: FaHome,
  "home-garden": FaCouch,
  animals: FaPaw,
  pets: FaPaw,
  services: FaTools,
  "business-industry": FaIndustry,
  jobs: FaBriefcase,
  "hobby-sport-kids": FaFutbol,
  "sports-hobbies": FaFutbol,
  "fashion-beauty": FaTshirt,
  essentials: FaShoppingBasket,
  "food-beverages": FaUtensils,
  "education-tuition": FaGraduationCap,
  "school-tuition": FaGraduationCap,
  tuition: FaGraduationCap,
  agriculture: FaSeedling,
  other: FaTags,
};

export function getCategoryIcon(slug: string): IconType {
  return ICONS_BY_SLUG[slug] ?? FaTags;
}
