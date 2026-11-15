// Types mirror the API DTOs (api/src/main/java/br/com/zelo/platform/**/*Response.java).

export interface LoginResponse {
  token: string;
  expiresAt: string;
}

export interface Activity {
  id: number;
  title: string;
  description: string | null;
  recurrencePattern: string | null;
  leadStaffId: number | null;
  status: 'ACTIVE' | 'FINISHED';
  startDate: string;
}

export interface Beneficiary {
  id: number;
  fullName: string;
  documentNumber: string | null;
  birthDate: string;
  phoneNumber: string;
  address: string | null;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface AttendanceEntry {
  beneficiaryId: number;
  present: boolean;
}

export interface AttendanceBatchRequest {
  activityId: number;
  attendanceDate: string;
  entries: AttendanceEntry[];
}

export interface AttendanceBatchResponse {
  activityId: number;
  attendanceDate: string;
  registered: number;
  present: number;
  absent: number;
}

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, detail: string) {
    super(detail);
    this.status = status;
  }
}

const TOKEN_KEY = 'zelo.token';
const EXPIRES_KEY = 'zelo.expiresAt';

// sessionStorage: the token is dropped when the tab is closed, which suits shared devices at the ONG.
export const session = {
  token(): string | null {
    const expiresAt = sessionStorage.getItem(EXPIRES_KEY);
    if (!expiresAt || Date.parse(expiresAt) <= Date.now()) {
      return null;
    }
    return sessionStorage.getItem(TOKEN_KEY);
  },
  save(response: LoginResponse): void {
    sessionStorage.setItem(TOKEN_KEY, response.token);
    sessionStorage.setItem(EXPIRES_KEY, response.expiresAt);
  },
  clear(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
  },
};

export const UNAUTHORIZED_EVENT = 'zelo:unauthorized';

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = session.token();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.status === 401 && token) {
    session.clear();
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as { detail?: string };
    throw new ApiError(response.status, problem.detail ?? response.statusText);
  }
  return (await response.json()) as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>('POST', '/auth/login', { email, password }),
  listActivities: () => request<Activity[]>('GET', '/activities'),
  listBeneficiaries: () => request<Beneficiary[]>('GET', '/beneficiaries'),
  registerAttendance: (batch: AttendanceBatchRequest) =>
    request<AttendanceBatchResponse>('POST', '/attendances/batch', batch),
};
