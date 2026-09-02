import { useEffect, useState } from "react";
import * as adminApi from "../../api/adminApi";
import { AdminPageHeader } from "../../components/AdminPageHeader/AdminPageHeader";
import { AdminCustomerRow } from "../../components/AdminCustomerRow/AdminCustomerRow";
import { ConfirmDialog } from "../../components/ConfirmDialog/ConfirmDialog";
import { LoadingState } from "../../components/LoadingState/LoadingState";
import { ErrorState } from "../../components/ErrorState/ErrorState";
import { EmptyState } from "../../components/EmptyState/EmptyState";
import { useToast } from "../../components/Toast/ToastProvider";
import { getApiErrorMessage } from "../../utils/apiError";
import type { CustomerResponse, CustomerStatus } from "../../types/api";
import "./AdminCustomersPage.css";

type FilterTab = "ALL" | CustomerStatus;

const FILTERS: { key: FilterTab; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "ACTIVE", label: "Active" },
  { key: "SUSPENDED", label: "Suspended" },
  { key: "DISABLED", label: "Disabled" },
];

export function AdminCustomersPage() {
  const { showToast } = useToast();
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<FilterTab>("ALL");
  const [pendingSuspend, setPendingSuspend] = useState<CustomerResponse | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [dialogLoading, setDialogLoading] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    return adminApi
      .listCustomers()
      .then(setCustomers)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load customers.")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const visible = tab === "ALL" ? customers : customers.filter((c) => c.status === tab);

  const confirmSuspend = async () => {
    if (!pendingSuspend) return;
    setDialogLoading(true);
    try {
      const updated = await adminApi.updateCustomerStatus(pendingSuspend.id, "SUSPENDED");
      setCustomers((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      showToast(`${updated.displayName} suspended.`);
      setPendingSuspend(null);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not suspend this customer."), "error");
    } finally {
      setDialogLoading(false);
    }
  };

  const handleActivate = async (customer: CustomerResponse) => {
    setBusyId(customer.id);
    try {
      const updated = await adminApi.updateCustomerStatus(customer.id, "ACTIVE");
      setCustomers((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      showToast(`${updated.displayName} activated.`);
    } catch (err) {
      showToast(getApiErrorMessage(err, "Could not activate this customer."), "error");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="admin-customers-page">
      <AdminPageHeader title="Customers" subtitle="View and manage customer accounts." />

      {!loading && !error && customers.length > 0 && (
        <div className="admin-customers-page__tabs">
          {FILTERS.map(({ key, label }) => (
            <button
              key={key}
              type="button"
              className={`admin-customers-page__tab ${tab === key ? "admin-customers-page__tab--active" : ""}`}
              onClick={() => setTab(key)}
            >
              {label} ({key === "ALL" ? customers.length : customers.filter((c) => c.status === key).length})
            </button>
          ))}
        </div>
      )}

      {loading && <LoadingState label="Loading customers…" />}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && customers.length === 0 && <EmptyState title="No customers found." />}

      {!loading && !error && customers.length > 0 && visible.length === 0 && (
        <EmptyState title="No customers in this status." />
      )}

      {!loading && !error && visible.length > 0 && (
        <div className="admin-customers-page__list">
          {visible.map((customer) => (
            <AdminCustomerRow
              key={customer.id}
              customer={customer}
              busy={busyId === customer.id}
              onSuspend={setPendingSuspend}
              onActivate={handleActivate}
            />
          ))}
        </div>
      )}

      <ConfirmDialog
        open={pendingSuspend !== null}
        title={`Suspend ${pendingSuspend?.displayName}?`}
        message="This customer will no longer be able to use protected marketplace actions."
        confirmLabel="Suspend"
        danger
        loading={dialogLoading}
        onConfirm={confirmSuspend}
        onCancel={() => setPendingSuspend(null)}
      />
    </div>
  );
}
