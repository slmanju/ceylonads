import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FaCheckCircle, FaCloudUploadAlt, FaCopy, FaExclamationTriangle, FaHourglassHalf } from "react-icons/fa";
import { cancelPayment, getBankTransferDetails, getPayment, submitPayment, uploadPaymentReceipt } from "../api/paymentApi";
import { resolveMediaUrl } from "../api/apiClient";
import { ConfirmDialog } from "../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../components/LoadingState/LoadingState";
import { ErrorState } from "../components/ErrorState/ErrorState";
import type { BankTransferDetailsResponse, PaymentResponse } from "../types/api";
import { formatPrice } from "../utils/formatPrice";
import { formatDate } from "../utils/formatDate";
import { getApiErrorMessage } from "../utils/apiError";
import "./PaymentPage.css";

const EDITABLE_STATUSES = new Set(["PENDING", "REJECTED"]);

export function PaymentPage() {
  const { id } = useParams<{ id: string }>();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [bankDetails, setBankDetails] = useState<BankTransferDetailsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [bankReference, setBankReference] = useState("");
  const [customerNote, setCustomerNote] = useState("");
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);

  const load = () => {
    if (!id) return Promise.resolve();
    setLoading(true);
    setLoadError(null);
    return Promise.all([getPayment(id), getBankTransferDetails()])
      .then(([paymentData, bankData]) => {
        setPayment(paymentData);
        setBankDetails(bankData);
        setBankReference(paymentData.bankReference ?? "");
        setCustomerNote(paymentData.customerNote ?? "");
      })
      .catch((err) => setLoadError(getApiErrorMessage(err, "Could not load this payment.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const copy = async (field: string, value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopiedField(field);
      setTimeout(() => setCopiedField((current) => (current === field ? null : current)), 1500);
    } catch {
      // Clipboard access can be blocked by the browser; silently ignore, the value is still visible to copy manually.
    }
  };

  const handleFileSelect = async (file: File) => {
    if (!id) return;
    setFormError(null);
    setUploading(true);
    setUploadProgress(0);
    try {
      const updated = await uploadPaymentReceipt(id, file, setUploadProgress);
      setPayment(updated);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not upload the receipt."));
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!id || !payment) return;
    if (!bankReference.trim()) {
      setFormError("Please enter your bank transfer reference.");
      return;
    }
    if (!payment.receiptUrl) {
      setFormError("Please upload your bank transfer receipt.");
      return;
    }
    setFormError(null);
    setSubmitting(true);
    try {
      const updated = await submitPayment(id, {
        bankReference: bankReference.trim(),
        customerNote: customerNote.trim() || undefined,
      });
      setPayment(updated);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not submit this payment."));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async () => {
    if (!id) return;
    setCancelling(true);
    try {
      const updated = await cancelPayment(id);
      setPayment(updated);
      setConfirmCancelOpen(false);
    } catch (err) {
      setFormError(getApiErrorMessage(err, "Could not cancel this payment."));
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="container payment-page">
        <LoadingState label="Loading payment details…" />
      </div>
    );
  }

  if (loadError || !payment || !bankDetails) {
    return (
      <div className="container payment-page">
        <ErrorState title="Can't load this payment" message={loadError ?? "This payment is unavailable."} onRetry={load} />
      </div>
    );
  }

  const editable = EDITABLE_STATUSES.has(payment.status);

  return (
    <div className="container payment-page">
      <h1 className="payment-page__title">Complete Your Payment</h1>

      <div className="payment-page__ad-summary">
        <span className="payment-page__ad-plan">{payment.promotionPlanName}</span>
        <p className="payment-page__ad-title">{payment.adTitle}</p>
      </div>

      {payment.status === "REJECTED" && (
        <div className="payment-page__banner payment-page__banner--danger">
          <FaExclamationTriangle aria-hidden="true" />
          <div>
            <p className="payment-page__banner-title">Payment verification failed</p>
            <p className="payment-page__banner-message">
              <strong>Reason:</strong> {payment.adminNote ?? "The submitted proof could not be verified."}
            </p>
            <p className="payment-page__banner-message">Please correct the details below and resubmit.</p>
          </div>
        </div>
      )}

      {payment.status === "SUBMITTED" && (
        <div className="payment-page__banner payment-page__banner--info">
          <FaHourglassHalf aria-hidden="true" />
          <div>
            <p className="payment-page__banner-title">Payment submitted for verification</p>
            <p className="payment-page__banner-message">We'll activate your promotion after the transfer is verified.</p>
          </div>
        </div>
      )}

      {payment.status === "APPROVED" && (
        <div className="payment-page__banner payment-page__banner--success">
          <FaCheckCircle aria-hidden="true" />
          <div>
            <p className="payment-page__banner-title">Payment approved</p>
            <p className="payment-page__banner-message">Your promotion is now active.</p>
          </div>
        </div>
      )}

      {payment.status === "CANCELLED" && (
        <div className="payment-page__banner payment-page__banner--neutral">
          <div>
            <p className="payment-page__banner-title">This payment was cancelled</p>
          </div>
        </div>
      )}

      <div className="payment-page__details">
        <div className="payment-page__field">
          <span className="payment-page__field-label">Amount</span>
          <span className="payment-page__field-value payment-page__field-value--amount">{formatPrice(payment.amount)}</span>
        </div>

        <div className="payment-page__field">
          <span className="payment-page__field-label">Payment Reference</span>
          <span className="payment-page__field-value payment-page__field-value--mono">
            {payment.paymentReference}
            <button
              type="button"
              className="payment-page__copy"
              onClick={() => copy("reference", payment.paymentReference)}
              aria-label="Copy payment reference"
            >
              <FaCopy aria-hidden="true" /> {copiedField === "reference" ? "Copied" : "Copy"}
            </button>
          </span>
        </div>
      </div>

      <div className="payment-page__bank-card">
        <h2 className="payment-page__section-title">Transfer to</h2>
        <dl className="payment-page__bank-details">
          <div>
            <dt>Bank</dt>
            <dd>{bankDetails.bankName}</dd>
          </div>
          <div>
            <dt>Account Name</dt>
            <dd>{bankDetails.accountName}</dd>
          </div>
          <div>
            <dt>Account Number</dt>
            <dd>
              {bankDetails.accountNumber}
              <button
                type="button"
                className="payment-page__copy"
                onClick={() => copy("account", bankDetails.accountNumber)}
                aria-label="Copy account number"
              >
                <FaCopy aria-hidden="true" /> {copiedField === "account" ? "Copied" : "Copy"}
              </button>
            </dd>
          </div>
          <div>
            <dt>Branch</dt>
            <dd>{bankDetails.branch}</dd>
          </div>
        </dl>
        <p className="payment-page__important">
          <strong>Important:</strong> Use <strong>{payment.paymentReference}</strong> as your transfer reference.{" "}
          {bankDetails.instructions}
        </p>
      </div>

      {editable && (
        <div className="payment-page__form">
          <h2 className="payment-page__section-title">After making the transfer</h2>

          {formError && (
            <p className="payment-page__error" role="alert">
              {formError}
            </p>
          )}

          <label className="payment-page__label" htmlFor="bankReference">
            Bank transfer reference
          </label>
          <input
            id="bankReference"
            type="text"
            className="payment-page__input"
            placeholder="e.g. FT123456789"
            value={bankReference}
            onChange={(e) => setBankReference(e.target.value)}
            maxLength={100}
          />

          <label className="payment-page__label" htmlFor="customerNote">
            Note (optional)
          </label>
          <textarea
            id="customerNote"
            className="payment-page__textarea"
            placeholder="e.g. Transferred from Commercial Bank"
            value={customerNote}
            onChange={(e) => setCustomerNote(e.target.value)}
            maxLength={500}
            rows={2}
          />

          <span className="payment-page__label">Receipt</span>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="visually-hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void handleFileSelect(file);
              e.target.value = "";
            }}
          />

          {payment.receiptUrl ? (
            <div className="payment-page__receipt-preview">
              <img src={resolveMediaUrl(payment.receiptUrl)} alt="Uploaded receipt" />
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? `Uploading… ${uploadProgress}%` : "Replace Receipt"}
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn btn-secondary payment-page__upload-btn"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
            >
              <FaCloudUploadAlt aria-hidden="true" /> {uploading ? `Uploading… ${uploadProgress}%` : "Upload Receipt"}
            </button>
          )}

          <div className="payment-page__form-actions">
            <button type="button" className="btn btn-outline" onClick={() => setConfirmCancelOpen(true)} disabled={submitting}>
              Cancel Payment
            </button>
            <button type="button" className="btn btn-primary" onClick={handleSubmit} disabled={submitting || uploading}>
              {submitting ? "Submitting…" : payment.status === "REJECTED" ? "Resubmit Payment" : "Submit Payment"}
            </button>
          </div>
        </div>
      )}

      {!editable && payment.status === "SUBMITTED" && payment.receiptUrl && (
        <div className="payment-page__submitted-summary">
          <h2 className="payment-page__section-title">What you submitted</h2>
          <dl className="payment-page__bank-details">
            <div>
              <dt>Bank reference</dt>
              <dd>{payment.bankReference}</dd>
            </div>
            <div>
              <dt>Submitted</dt>
              <dd>{formatDate(payment.submittedAt)}</dd>
            </div>
          </dl>
          <img className="payment-page__receipt-readonly" src={resolveMediaUrl(payment.receiptUrl)} alt="Submitted receipt" />
        </div>
      )}

      <div className="payment-page__footer-links">
        <Link to="/my-payments" className="btn btn-secondary">
          View My Payments
        </Link>
        {payment.status === "APPROVED" && (
          <Link to="/my-promotions" className="btn btn-primary">
            View My Promotions
          </Link>
        )}
      </div>

      <ConfirmDialog
        open={confirmCancelOpen}
        title="Cancel this payment?"
        message="This will also cancel the pending promotion for this ad. This cannot be undone."
        confirmLabel="Cancel Payment"
        danger
        loading={cancelling}
        onConfirm={handleCancel}
        onCancel={() => setConfirmCancelOpen(false)}
      />
    </div>
  );
}
