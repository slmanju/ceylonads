import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./styles/global.css";
import App from "./App.tsx";
import { AuthProvider } from "./auth/AuthContext.tsx";
import { CampaignProvider } from "./campaign/CampaignContext.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <CampaignProvider>
          <App />
        </CampaignProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
