import { Routes, Route } from "react-router-dom";
import { AppLayout } from "./components/Layout/AppLayout";
import { TuitionAdminLayout } from "./components/TuitionAdminLayout/TuitionAdminLayout";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { ProtectedAdminRoute } from "./auth/ProtectedAdminRoute";
import { HomePage } from "./pages/HomePage";
import { ClassesPage } from "./pages/ClassesPage";
import { ClassDetailPage } from "./pages/ClassDetailPage";
import { OnlineClassesPage } from "./pages/OnlineClassesPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { PostAdPage } from "./pages/PostAdPage";
import { EditAdPage } from "./pages/EditAdPage";
import { MyAdsPage } from "./pages/MyAdsPage";
import { PromoteClassPage } from "./pages/PromoteClassPage";
import { PricingPage } from "./pages/PricingPage";
import { AccountPage } from "./pages/AccountPage";
import { SuggestPage } from "./pages/SuggestPage";
import { ContactPage } from "./pages/ContactPage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminPendingClassesPage } from "./pages/admin/AdminPendingClassesPage";
import { AdminClassReviewPage } from "./pages/admin/AdminClassReviewPage";
import { AdminSuggestionsPage } from "./pages/admin/AdminSuggestionsPage";
import { AdminPromotionsPage } from "./pages/admin/AdminPromotionsPage";
import { AdminPromotionReviewPage } from "./pages/admin/AdminPromotionReviewPage";
import { AdminPromotionPlansPage } from "./pages/admin/AdminPromotionPlansPage";
import { AdminPromotionPlanFormPage } from "./pages/admin/AdminPromotionPlanFormPage";
import { AdminCampaignsPage } from "./pages/admin/AdminCampaignsPage";
import { AdminCampaignFormPage } from "./pages/admin/AdminCampaignFormPage";
import { NotFoundPage } from "./pages/NotFoundPage";

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/classes" element={<ClassesPage />} />
        <Route path="/classes/:slug" element={<ClassDetailPage />} />
        <Route path="/online-classes" element={<OnlineClassesPage />} />
        <Route path="/pricing" element={<PricingPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/suggest" element={<SuggestPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/post-ad"
          element={
            <ProtectedRoute>
              <PostAdPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-ads"
          element={
            <ProtectedRoute>
              <MyAdsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-ads/:id/edit"
          element={
            <ProtectedRoute>
              <EditAdPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-ads/:id/promote"
          element={
            <ProtectedRoute>
              <PromoteClassPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route
        path="/admin/tuition"
        element={
          <ProtectedAdminRoute>
            <TuitionAdminLayout />
          </ProtectedAdminRoute>
        }
      >
        <Route index element={<AdminDashboardPage />} />
        <Route path="pending" element={<AdminPendingClassesPage />} />
        <Route path="pending/:id" element={<AdminClassReviewPage />} />
        <Route path="promotions" element={<AdminPromotionsPage />} />
        <Route path="promotions/:id" element={<AdminPromotionReviewPage />} />
        <Route path="promotion-plans" element={<AdminPromotionPlansPage />} />
        <Route path="promotion-plans/new" element={<AdminPromotionPlanFormPage />} />
        <Route path="promotion-plans/:id/edit" element={<AdminPromotionPlanFormPage />} />
        <Route path="campaigns" element={<AdminCampaignsPage />} />
        <Route path="campaigns/new" element={<AdminCampaignFormPage />} />
        <Route path="campaigns/:id/edit" element={<AdminCampaignFormPage />} />
        <Route path="suggestions" element={<AdminSuggestionsPage />} />
      </Route>
    </Routes>
  );
}

export default App;
