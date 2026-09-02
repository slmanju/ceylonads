import { Routes, Route } from "react-router-dom";
import { AppLayout } from "./components/Layout/AppLayout";
import { AdminLayout } from "./components/AdminLayout/AdminLayout";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { ToastProvider } from "./components/Toast/ToastProvider";
import { HomePage } from "./pages/HomePage";
import { AdsPage } from "./pages/AdsPage";
import { AdDetailsPage } from "./pages/AdDetailsPage";
import { CategoryPage } from "./pages/CategoryPage";
import { CategoryLocationPage } from "./pages/CategoryLocationPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { PostAdPage } from "./pages/PostAdPage";
import { EditAdPage } from "./pages/EditAdPage";
import { MyAdsPage } from "./pages/MyAdsPage";
import { PromoteAdPage } from "./pages/PromoteAdPage";
import { MyPromotionsPage } from "./pages/MyPromotionsPage";
import { MyPaymentsPage } from "./pages/MyPaymentsPage";
import { PaymentPage } from "./pages/PaymentPage";
import { ChangePasswordPage } from "./pages/ChangePasswordPage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminAdsPage } from "./pages/admin/AdminAdsPage";
import { AdminAdReviewPage } from "./pages/admin/AdminAdReviewPage";
import { AdminCustomersPage } from "./pages/admin/AdminCustomersPage";
import { AdminCategoriesPage } from "./pages/admin/AdminCategoriesPage";
import { AdminCategoryAttributesPage } from "./pages/admin/AdminCategoryAttributesPage";
import { AdminLocationsPage } from "./pages/admin/AdminLocationsPage";
import { AdminPromotionSlotsPage } from "./pages/admin/AdminPromotionSlotsPage";
import { AdminPromotionPlansPage } from "./pages/admin/AdminPromotionPlansPage";
import { AdminPromotionsPage } from "./pages/admin/AdminPromotionsPage";
import { AdminPaymentsPage } from "./pages/admin/AdminPaymentsPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import type { Role } from "./types/api";

const MODERATION_ROLES: Role[] = ["MODERATOR", "ADMIN"];
const AD_OWNER_ROLES: Role[] = ["CUSTOMER", "MODERATOR"];

function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/ads" element={<AdsPage />} />
          <Route path="/ads/:slug" element={<AdDetailsPage />} />
          <Route path="/category/:slug" element={<CategoryPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          {/* Category + location SEO landing pages, e.g. /vehicles/colombo. Ranked matching means
              static routes above (e.g. /category/:slug, /ads/:slug) still win when their first
              segment matches literally, so this generic two-segment route only catches genuine
              category/location combinations. */}
          <Route path="/:categorySlug/:locationSlug" element={<CategoryLocationPage />} />
          <Route
            path="/post-ad"
            element={
              <ProtectedRoute requireRole={AD_OWNER_ROLES}>
                <PostAdPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-ads"
            element={
              <ProtectedRoute requireRole={AD_OWNER_ROLES}>
                <MyAdsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-ads/:id/edit"
            element={
              <ProtectedRoute requireRole={AD_OWNER_ROLES}>
                <EditAdPage />
              </ProtectedRoute>
            }
          />
          {/* Ad moderation for MODERATOR + ADMIN, reusing the same ad-review components the /admin
              section uses (see AdminAdsPage/AdminAdReviewPage) rather than a duplicate page. Lives
              under AppLayout (Header nav), not AdminLayout, since Moderator has no promotion/payment
              admin access. */}
          <Route
            path="/moderation"
            element={
              <ProtectedRoute requireRole={MODERATION_ROLES}>
                <AdminAdsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderation/:id"
            element={
              <ProtectedRoute requireRole={MODERATION_ROLES}>
                <AdminAdReviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-ads/:id/promote"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <PromoteAdPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-promotions"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <MyPromotionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-promotions/request"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <PromoteAdPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-payments"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <MyPaymentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-payments/:id"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <PaymentPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/account/change-password"
            element={
              <ProtectedRoute>
                <ChangePasswordPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Route>

        <Route
          path="/admin"
          element={
            <ProtectedRoute requireRole="ADMIN">
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminDashboardPage />} />
          <Route path="ads" element={<AdminAdsPage />} />
          <Route path="ads/:id" element={<AdminAdReviewPage />} />
          <Route path="customers" element={<AdminCustomersPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="categories/:id/attributes" element={<AdminCategoryAttributesPage />} />
          <Route path="locations" element={<AdminLocationsPage />} />
          <Route path="promotion-slots" element={<AdminPromotionSlotsPage />} />
          <Route path="promotion-plans" element={<AdminPromotionPlansPage />} />
          <Route path="promotions" element={<AdminPromotionsPage />} />
          <Route path="payments" element={<AdminPaymentsPage />} />
        </Route>
      </Routes>
    </ToastProvider>
  );
}

export default App;
