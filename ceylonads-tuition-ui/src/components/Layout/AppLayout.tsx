import { Outlet } from "react-router-dom";
import { Header } from "../Header/Header";
import { Footer } from "../Footer/Footer";
import { CampaignBanner } from "../Campaign/CampaignBanner";
import { CampaignModal } from "../Campaign/CampaignModal";

export function AppLayout() {
  return (
    <>
      <CampaignBanner />
      <Header />
      <main className="app-main">
        <Outlet />
      </main>
      <Footer />
      <CampaignModal />
    </>
  );
}
