import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  FaBullhorn,
  FaUsers,
  FaSitemap,
  FaMapMarkedAlt,
  FaClipboardCheck,
  FaHourglassHalf,
  FaStar,
  FaMoneyCheckAlt,
} from "react-icons/fa";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { StatCard } from "../../components/StatCard/StatCard";
import * as adminApi from "../../api/adminApi";
import * as moderationApi from "../../api/moderationApi";
import * as adminPromotionApi from "../../api/adminPromotionApi";
import * as adminPaymentApi from "../../api/adminPaymentApi";
import * as categoryApi from "../../api/categoryApi";
import * as locationApi from "../../api/locationApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./AdminDashboardPage.css";

interface StatState {
  value: number | null;
  loading: boolean;
  error: string | null;
}

const INITIAL_STAT: StatState = { value: null, loading: true, error: null };

export function AdminDashboardPage() {
  const [pending, setPending] = useState<StatState>(INITIAL_STAT);
  const [customers, setCustomers] = useState<StatState>(INITIAL_STAT);
  const [categories, setCategories] = useState<StatState>(INITIAL_STAT);
  const [locations, setLocations] = useState<StatState>(INITIAL_STAT);
  const [pendingPromotions, setPendingPromotions] = useState<StatState>(INITIAL_STAT);
  const [activePromotions, setActivePromotions] = useState<StatState>(INITIAL_STAT);
  const [paymentsAwaitingReview, setPaymentsAwaitingReview] = useState<StatState>(INITIAL_STAT);

  useEffect(() => {
    moderationApi
      .listPendingAds()
      .then((data) => setPending({ value: data.length, loading: false, error: null }))
      .catch((err) => setPending({ value: null, loading: false, error: getApiErrorMessage(err) }));

    adminApi
      .listCustomers()
      .then((data) => setCustomers({ value: data.length, loading: false, error: null }))
      .catch((err) => setCustomers({ value: null, loading: false, error: getApiErrorMessage(err) }));

    categoryApi
      .listCategories()
      .then((data) => setCategories({ value: data.length, loading: false, error: null }))
      .catch((err) => setCategories({ value: null, loading: false, error: getApiErrorMessage(err) }));

    locationApi
      .listLocations()
      .then((data) => setLocations({ value: data.length, loading: false, error: null }))
      .catch((err) => setLocations({ value: null, loading: false, error: getApiErrorMessage(err) }));

    adminPromotionApi
      .listPromotions("PENDING_PAYMENT")
      .then((data) => setPendingPromotions({ value: data.length, loading: false, error: null }))
      .catch((err) => setPendingPromotions({ value: null, loading: false, error: getApiErrorMessage(err) }));

    adminPromotionApi
      .listPromotions("ACTIVE")
      .then((data) => setActivePromotions({ value: data.length, loading: false, error: null }))
      .catch((err) => setActivePromotions({ value: null, loading: false, error: getApiErrorMessage(err) }));

    // A dedicated count endpoint, not a full payment list, since this list can grow much larger
    // than the promotion lists above.
    adminPaymentApi
      .countPayments("SUBMITTED")
      .then((count) => setPaymentsAwaitingReview({ value: count, loading: false, error: null }))
      .catch((err) => setPaymentsAwaitingReview({ value: null, loading: false, error: getApiErrorMessage(err) }));
  }, []);

  return (
    <div className="admin-dashboard-page">
      <AdminPageHeader title="Dashboard" subtitle="Overview of the CeylonAds marketplace." />

      <div className="admin-dashboard-page__stats">
        <StatCard label="Pending Ads" icon={FaBullhorn} to="/admin/ads" {...pending} />
        <StatCard label="Customers" icon={FaUsers} to="/admin/customers" {...customers} />
        <StatCard label="Categories" icon={FaSitemap} to="/admin/categories" {...categories} />
        <StatCard label="Locations" icon={FaMapMarkedAlt} to="/admin/locations" {...locations} />
        <StatCard label="Pending Promotions" icon={FaHourglassHalf} to="/admin/promotions" {...pendingPromotions} />
        <StatCard label="Active Promotions" icon={FaStar} to="/admin/promotions" {...activePromotions} />
        <StatCard label="Payments Awaiting Review" icon={FaMoneyCheckAlt} to="/admin/payments" {...paymentsAwaitingReview} />
      </div>

      <div className="admin-dashboard-page__shortcut">
        <div>
          <h2>Ad moderation</h2>
          <p>New ads wait for review before they appear on the public marketplace.</p>
        </div>
        <Link to="/admin/ads" className="btn btn-primary">
          <FaClipboardCheck aria-hidden="true" />
          Review Pending Ads
        </Link>
      </div>
    </div>
  );
}
