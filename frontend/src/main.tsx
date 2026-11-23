import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { BackofficeApp } from "./backoffice/BackofficeApp";
import { WebshopApp } from "./webshop/WebshopApp";
import { DriverApp } from "./driver/DriverApp";
import "./styles.css";

// One build, three apps with separate Keycloak realms/roles:
//   /webshop/*  customer webshop  (realm "customers")
//   /driver/*   driver app        (realm "staff", role DRIVER)
//   /*          restaurant back office (realm "staff")
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/webshop/*" element={<WebshopApp />} />
        <Route path="/driver/*" element={<DriverApp />} />
        <Route path="/*" element={<BackofficeApp />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);
