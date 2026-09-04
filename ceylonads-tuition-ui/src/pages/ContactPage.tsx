import { FaWhatsapp } from "react-icons/fa";
import { Seo } from "../components/Seo/Seo";
import { buildEzClassWhatsAppLink } from "../utils/whatsapp";
import "./ContactPage.css";

export function ContactPage() {
  const whatsappLink = buildEzClassWhatsAppLink("Hi ezClass, I need some help.");

  return (
    <div className="contact-page container">
      <Seo title="Contact Us" description="Get in touch with ezClass over WhatsApp." />
      <div className="contact-card">
        <h1 className="contact-card__title">Contact ezClass</h1>
        <p className="contact-card__subtitle">
          Have a question about a class, a payment, or your account? Message us on WhatsApp and
          we'll get back to you.
        </p>

        <a
          href={whatsappLink}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-accent contact-card__whatsapp"
        >
          <FaWhatsapp aria-hidden="true" /> WhatsApp Us
        </a>
      </div>
    </div>
  );
}
