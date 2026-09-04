import { Outlet } from "react-router-dom";
import { Header } from "../Header/Header";
import { Footer } from "../Footer/Footer";
import { CampaignBanner } from "../Campaign/CampaignBanner";
import { CampaignModal } from "../Campaign/CampaignModal";
import { JsonLd } from "../Seo/JsonLd";
import { organizationJsonLd, websiteJsonLd } from "../../utils/structuredData";

export function AppLayout() {
  return (
    <>
      {/* Site-wide, not per-page - mounted once here rather than repeated on every route. */}
      <JsonLd id="site" data={[organizationJsonLd(), websiteJsonLd()]} />
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
