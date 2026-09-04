import { Routes, Route } from "react-router-dom";
import { AppLayout } from "./components/Layout/AppLayout";
import { ProtectedRoute } from "./auth/ProtectedRoute";
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
    </Routes>
  );
}

export default App;
