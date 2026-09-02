import { Link } from "react-router-dom";
import "./Footer.css";

export function Footer() {
  return (
    <footer className="footer">
      <div className="container footer__grid">
        <div className="footer__brand">
          <p className="footer__logo">
            Ceylon<span>Ads</span>
          </p>
          <p className="footer__tagline">Sri Lanka's trusted marketplace to find, connect and sell.</p>
        </div>

        <div className="footer__col">
          <p className="footer__heading">Marketplace</p>
          <Link to="/ads">Browse all ads</Link>
          <Link to="/post-ad">Post a free ad</Link>
          <Link to="/register">Create an account</Link>
        </div>

        <div className="footer__col">
          <p className="footer__heading">Account</p>
          <Link to="/login">Login</Link>
          <Link to="/my-ads">My ads</Link>
        </div>

        <div className="footer__col">
          <p className="footer__heading">Support</p>
          <span className="footer__static">Help &amp; support</span>
          <span className="footer__static">Terms of use</span>
          <span className="footer__static">Privacy policy</span>
        </div>
      </div>

      <div className="container footer__bottom">
        <p>© {new Date().getFullYear()} CeylonAds. All rights reserved.</p>
      </div>
    </footer>
  );
}
