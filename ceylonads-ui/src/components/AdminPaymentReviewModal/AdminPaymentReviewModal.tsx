import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as adminPaymentApi from "../../api/adminPaymentApi";
import { resolveMediaUrl } from "../../api/apiClient";
import { Modal } from "../Modal/Modal";
import { ConfirmDialog } from "../ConfirmDialog/ConfirmDialog";
import { PaymentStatusBadge } from "../PaymentStatusBadge/PaymentStatusBadge";
import { LoadingState } from "../LoadingState/LoadingState";
import { ErrorState } from "../ErrorState/ErrorState";
import type { PaymentMethod, PaymentResponse } from "../../types/api";
import { formatPrice } from "../../utils/formatPrice";
import { formatDate } from "../../utils/formatDate";
import { formatPaymentMethod } from "../../utils/formatPaymentMethod";
import { getApiErrorMessage } from "../../utils/apiError";
import "./AdminPaymentReviewModal.css";

interface AdminPaymentReviewModalProps {
  paymentId: number | null;
  onClose: () => void;
  onChanged: (updated: PaymentResponse) => void;
}

export function AdminPaymentReviewModal({ paymentId, onClose, onChanged }: AdminPaymentReviewModalProps) {
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [confirmApproveOpen, setConfirmApproveOpen] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Manual verification: an admin confirming a payment that has no uploaded proof yet (cash,
  // phone-arranged, or other offline settlement) records how it was actually paid.
  const [verifying, setVerifying] = useState(false);
  const [verifyMethod, setVerifyMethod] = useState<PaymentMethod>("CASH");
  const [verifyNote, setVerifyNote] = useState("");
  const [confirmVerifyOpen, setConfirmVerifyOpen] = useState(false);

  useEffect(() => {
    if (paymentId === null) return;
    setLoading(true);
    setLoadError(null);
    setRejecting(false);
    setRejectReason("");
    setActionError(null);
    setVerifying(false);
    setVerifyMethod("CASH");
    setVerifyNote("");
    adminPaymentApi
      .getPayment(paymentId)
      .then(setPayment)
      .catch((err) => setLoadError(getApiErrorMessage(err, "Could not load this payment.")))
      .finally(() => setLoading(false));
  }, [paymentId]);

  const handleApprove = async () => {
    if (!payment) return;
    setActionBusy(true);
    setActionError(null);
    try {
      const updated = await adminPaymentApi.approvePayment(payment.id);
      setConfirmApproveOpen(false);
      onChanged(updated);
      onClose();
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not approve this payment."));
      setConfirmApproveOpen(false);
    } finally {
      setActionBusy(false);
    }
  };

  const handleVerify = async () => {
    if (!payment) return;
    setActionBusy(true);
    setActionError(null);
    try {
      const updated = await adminPaymentApi.approvePayment(payment.id, {
        paymentMethod: verifyMethod,
        adminNote: verifyNote.trim() || undefined,
      });
      setConfirmVerifyOpen(false);
      onChanged(updated);
      onClose();
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not verify this payment."));
      setConfirmVerifyOpen(false);
    } finally {
      setActionBusy(false);
    }
  };

  const handleReject = async () => {
    if (!payment || !rejectReason.trim()) return;
    setActionBusy(true);
    setActionError(null);
    try {
      const updated = await adminPaymentApi.rejectPayment(payment.id, { reason: rejectReason.trim() });
      onChanged(updated);
      onClose();
    } catch (err) {
      setActionError(getApiErrorMessage(err, "Could not reject this payment."));
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <Modal open={paymentId !== null} title="Review Payment" onClose={onClose}>
      {loading && <LoadingState label="Loading payment…" />}

      {!loading && loadError && <ErrorState message={loadError} />}

      {!loading && !loadError && payment && (
        <div className="payment-review">
          <div className="payment-review__header">
            <span className="payment-review__reference">{payment.paymentReference}</span>
            <PaymentStatusBadge status={payment.status} />
          </div>

          {actionError && (
            <p className="payment-review__error" role="alert">
              {actionError}
            </p>
          )}

          <dl className="payment-review__details">
            <div>
              <dt>Customer</dt>
              <dd>
                {payment.customerDisplayName}
                <span className="payment-review__sub">
                  {payment.customerEmail}
                  {payment.customerPhone ? ` · ${payment.customerPhone}` : ""}
                </span>
              </dd>
            </div>
            <div>
              <dt>Ad</dt>
              <dd>
                <Link to={`/ads/${payment.adId}`} target="_blank" rel="noreferrer">
                  {payment.adTitle}
                </Link>
              </dd>
            </div>
            <div>
              <dt>Promotion</dt>
              <dd>{payment.promotionPlanName}</dd>
            </div>
            <div>
              <dt>Expected Amount</dt>
              <dd className="payment-review__amount">{formatPrice(payment.amount)}</dd>
            </div>
            <div>
              <dt>Payment Method</dt>
              <dd>{formatPaymentMethod(payment.paymentMethod)}</dd>
            </div>
            <div>
              <dt>Bank Reference</dt>
              <dd>{payment.bankReference ?? "—"}</dd>
            </div>
            <div>
              <dt>Customer Note</dt>
              <dd>{payment.customerNote ?? "—"}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDate(payment.createdAt)}</dd>
            </div>
            <div>
              <dt>Submitted</dt>
              <dd>{formatDate(payment.submittedAt)}</dd>
            </div>
            {payment.adminNote && (
              <div>
                <dt>Admin Note</dt>
                <dd>{payment.adminNote}</dd>
              </div>
            )}
          </dl>

          <div className="payment-review__receipt">
            <span className="payment-review__label">Receipt</span>
            {payment.receiptUrl ? (
              <a href={resolveMediaUrl(payment.receiptUrl)} target="_blank" rel="noreferrer">
                <img src={resolveMediaUrl(payment.receiptUrl)} alt="Payment receipt" />
              </a>
            ) : (
              <p className="payment-review__no-receipt">No receipt uploaded.</p>
            )}
          </div>

          {payment.status === "PENDING" && !verifying && (
            <div className="payment-review__actions">
              <button type="button" className="btn btn-primary" onClick={() => setVerifying(true)} disabled={actionBusy}>
                Verify Payment (No Proof / Manual)
              </button>
            </div>
          )}

          {payment.status === "PENDING" && verifying && (
            <div className="payment-review__reject-form">
              <p className="payment-review__no-receipt payment-review__verify-hint">
                No proof has been uploaded. If payment was received by cash, over the phone, or another offline
                method, record how it was settled and verify it directly.
              </p>
              <label className="payment-review__label" htmlFor="verifyMethod">
                Payment method
              </label>
              <select
                id="verifyMethod"
                className="payment-review__select"
                value={verifyMethod}
                onChange={(e) => setVerifyMethod(e.target.value as PaymentMethod)}
              >
                <option value="CASH">Cash</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="OTHER">Other</option>
              </select>
              <label className="payment-review__label" htmlFor="verifyNote">
                Note (optional)
              </label>
              <textarea
                id="verifyNote"
                className="payment-review__textarea"
                placeholder="e.g. Cash received at office on 22 Aug"
                value={verifyNote}
                onChange={(e) => setVerifyNote(e.target.value)}
                maxLength={500}
                rows={2}
              />
              <div className="payment-review__actions">
                <button type="button" className="btn btn-secondary" onClick={() => setVerifying(false)} disabled={actionBusy}>
                  Back
                </button>
                <button type="button" className="btn btn-primary" onClick={() => setConfirmVerifyOpen(true)} disabled={actionBusy}>
                  Verify & Activate
                </button>
              </div>
            </div>
          )}

          {payment.status === "SUBMITTED" && !rejecting && (
            <div className="payment-review__actions">
              <button type="button" className="btn btn-outline" onClick={() => setRejecting(true)} disabled={actionBusy}>
                Reject Payment
              </button>
              <button type="button" className="btn btn-primary" onClick={() => setConfirmApproveOpen(true)} disabled={actionBusy}>
                Approve Payment
              </button>
            </div>
          )}

          {payment.status === "SUBMITTED" && rejecting && (
            <div className="payment-review__reject-form">
              <label className="payment-review__label" htmlFor="rejectReason">
                Reason for rejection
              </label>
              <textarea
                id="rejectReason"
                className="payment-review__textarea"
                placeholder="e.g. Receipt is unclear. Bank reference could not be verified."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                maxLength={500}
                rows={3}
              />
              <div className="payment-review__actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setRejecting(false)}
                  disabled={actionBusy}
                >
                  Back
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleReject}
                  disabled={actionBusy || !rejectReason.trim()}
                >
                  {actionBusy ? "Please wait…" : "Confirm Rejection"}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <ConfirmDialog
        open={confirmApproveOpen}
        title="Approve this payment?"
        message="This will activate the customer's promotion."
        confirmLabel="Approve & Activate"
        loading={actionBusy}
        onConfirm={handleApprove}
        onCancel={() => setConfirmApproveOpen(false)}
      />

      <ConfirmDialog
        open={confirmVerifyOpen}
        title="Verify this payment?"
        message="This records the payment as manually verified and activates the customer's promotion, even though no proof was uploaded."
        confirmLabel="Verify & Activate"
        loading={actionBusy}
        onConfirm={handleVerify}
        onCancel={() => setConfirmVerifyOpen(false)}
      />
    </Modal>
  );
}
