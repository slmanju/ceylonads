import { Link } from "react-router-dom";
import "./Footer.css";

export function Footer() {
  return (
    <footer className="tuition-footer">
      <div className="container tuition-footer__grid">
        <div className="tuition-footer__brand">
          <p className="tuition-footer__logo">
            ez<span>Class</span>
          </p>
          <p className="tuition-footer__tagline">
            Find classes, tutors and courses across Sri Lanka.
          </p>
        </div>

        <div className="tuition-footer__col">
          <p className="tuition-footer__heading">Discover</p>
          <Link to="/classes">Browse all classes</Link>
          <Link to="/online-classes">Online classes</Link>
        </div>

        <div className="tuition-footer__col">
          <p className="tuition-footer__heading">Account</p>
          <Link to="/post-ad">Post a Class</Link>
          <Link to="/login">Login</Link>
        </div>

        <div className="tuition-footer__col">
          <p className="tuition-footer__heading">Support</p>
          <Link to="/contact">Contact Us</Link>
          <Link to="/suggest">Suggestion / Feedback</Link>
        </div>

        <div className="tuition-footer__col">
          <p className="tuition-footer__heading">Legal</p>
          <span className="tuition-footer__static">Terms of use</span>
          <span className="tuition-footer__static">Privacy policy</span>
        </div>
      </div>

      <div className="container tuition-footer__bottom">
        <p>© {new Date().getFullYear()} ezClass. All rights reserved.</p>
      </div>
    </footer>
  );
}
