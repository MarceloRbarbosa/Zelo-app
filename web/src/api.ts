// Types mirror the API DTOs (api/src/main/java/br/com/zelo/platform/**/*Response.java).

export type Role = 'ROLE_ADMIN' | 'ROLE_COORDINATOR' | 'ROLE_VOLUNTEER';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface LoginResponse {
  token: string;
  expiresAt: string;
  mustChangePassword: boolean;
}

export interface Profile {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  mustChangePassword: boolean;
}

export interface StaffMember {
  id: number;
  fullName: string;
  email: string;
  memberType: 'VOLUNTEER' | 'PROFESSIONAL' | 'COORDINATOR';
  role: Role;
  status: 'ACTIVE' | 'BLOCKED';
  mustChangePassword: boolean;
}

export interface StaffMemberRequest {
  fullName: string;
  email: string;
  password: string;
  memberType: StaffMember['memberType'];
  role: Role;
}

export interface StaffSummary {
  id: number;
  fullName: string;
}

export interface ActivitySchedule {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface Activity {
  id: number;
  title: string;
  description: string | null;
  status: 'ACTIVE' | 'FINISHED';
  startDate: string;
  endDate: string | null;
  schedules: ActivitySchedule[];
  staff: StaffSummary[];
}

export interface ActivityRequest {
  title: string;
  description: string | null;
  startDate: string;
  schedules: ActivitySchedule[];
  staffIds: number[];
}

export type EnrollmentStatus = 'ACTIVE' | 'LEFT';

export interface Enrollment {
  beneficiaryId: number;
  fullName: string;
  status: EnrollmentStatus;
  leftAt: string | null;
}

export interface DiaryMark {
  date: string;
  present: boolean;
  notes: string | null;
}

export interface DiaryStudent {
  beneficiaryId: number;
  fullName: string;
  enrollmentStatus: EnrollmentStatus;
  leftAt: string | null;
  attendanceRate: number | null;
  dropoutRisk: boolean;
  marks: DiaryMark[];
}

export interface Diary {
  activityId: number;
  title: string;
  status: Activity['status'];
  dates: string[];
  students: DiaryStudent[];
}

export interface ClassSession {
  activityId: number;
  title: string;
  status: Activity['status'];
  date: string;
  startTime: string;
  endTime: string;
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

export interface BeneficiaryRequest {
  fullName: string;
  documentNumber: string | null;
  birthDate: string;
  phoneNumber: string;
  address: string | null;
}

export interface BeneficiarySearch {
  name?: string;
  documentNumber?: string;
  activityId?: number;
  order?: 'ASC' | 'DESC';
  status?: 'ACTIVE' | 'INACTIVE' | 'ALL';
}

export interface BeneficiaryDocument {
  id: number;
  title: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface DataRemoval {
  id: number;
  beneficiaryId: number;
  removedAt: string;
  removedBy: string | null;
  note: string;
}

export interface AttendanceEntry {
  beneficiaryId: number;
  present: boolean;
  notes?: string | null;
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

export interface DropoutAlert {
  activityId: number;
  activityTitle: string;
  beneficiaryId: number;
  beneficiaryName: string;
  phoneNumber: string;
  consecutiveAbsences: number;
  lastAttendanceDate: string;
}

export interface ActivityOverview {
  activityId: number;
  title: string;
  status: Activity['status'];
  attendanceRate: number | null;
  activeEnrollments: number;
}

export interface ProgramOverview {
  from: string;
  to: string;
  attendanceRate: number | null;
  activeEnrollments: number;
  leftEnrollments: number;
  activities: ActivityOverview[];
}

// activeStaff is null for volunteers, who do not see the team.
export interface Totals {
  activeBeneficiaries: number;
  activeActivities: number;
  activeEnrollments: number;
  activeStaff: number | null;
}

export interface ZeloEvent {
  id: number;
  title: string;
  description: string | null;
  eventDate: string;
  location: string | null;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELED';
  staff: StaffSummary[];
}

export interface EventRequest {
  title: string;
  description: string | null;
  eventDate: string;
  location: string | null;
  staffIds: number[];
}

export interface Participant {
  beneficiaryId: number;
  fullName: string;
  registeredAt: string;
  attended: boolean;
}

export interface AssistanceItem {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
  received: number;
  delivered: number;
  balance: number;
}

export interface Receipt {
  id: number;
  itemId: number;
  itemName: string;
  quantity: number;
  receivedAt: string;
  donor: string | null;
  notes: string | null;
  recordedBy: string | null;
}

export interface Delivery {
  id: number;
  itemId: number;
  itemName: string;
  beneficiaryId: number;
  beneficiaryName: string | null;
  quantity: number;
  deliveredAt: string;
  notes: string | null;
  recordedBy: string | null;
}

export class ApiError extends Error {
  readonly status: number;
  readonly errors: Record<string, string>;
  // Extra ProblemDetail fields, e.g. who already has a CPF on a 409.
  readonly problem: Record<string, unknown>;

  constructor(status: number, detail: string, errors: Record<string, string> = {}, problem: Record<string, unknown> = {}) {
    super(detail);
    this.status = status;
    this.errors = errors;
    this.problem = problem;
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

// Files go as multipart form data (the browser sets the boundary); everything else as JSON.
async function send(method: string, path: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {};
  const token = session.token();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  });

  if (response.status === 401 && token) {
    session.clear();
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Record<string, unknown> & {
      detail?: string;
      errors?: Record<string, string>;
    };
    throw new ApiError(response.status, problem.detail ?? response.statusText, problem.errors, problem);
  }
  return response;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await send(method, path, body);
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [name, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(name, String(value));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : '';
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>('POST', '/auth/login', { email, password }),

  me: () => request<Profile>('GET', '/me'),
  updateMe: (fullName: string) => request<Profile>('PUT', '/me', { fullName }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('PUT', '/me/password', { currentPassword, newPassword }),

  listStaff: () => request<StaffMember[]>('GET', '/staff-members'),
  createStaff: (body: StaffMemberRequest) => request<StaffMember>('POST', '/staff-members', body),
  blockStaff: (id: number) => request<StaffMember>('POST', `/staff-members/${id}/block`),
  unblockStaff: (id: number) => request<StaffMember>('POST', `/staff-members/${id}/unblock`),

  listActivities: (status: Activity['status'] = 'ACTIVE') =>
    request<Activity[]>('GET', `/activities${query({ status })}`),
  getActivity: (id: number) => request<Activity>('GET', `/activities/${id}`),
  createActivity: (body: ActivityRequest) => request<Activity>('POST', '/activities', body),
  updateActivity: (id: number, body: ActivityRequest) => request<Activity>('PUT', `/activities/${id}`, body),
  finishActivity: (id: number, endDate: string) =>
    request<Activity>('POST', `/activities/${id}/finish`, { endDate }),
  listEnrollments: (activityId: number) => request<Enrollment[]>('GET', `/activities/${activityId}/enrollments`),
  enroll: (activityId: number, beneficiaryId: number) =>
    request<Enrollment>('POST', `/activities/${activityId}/enrollments`, { beneficiaryId }),
  changeEnrollment: (activityId: number, beneficiaryId: number, status: EnrollmentStatus, leftAt?: string) =>
    request<Enrollment>('PUT', `/activities/${activityId}/enrollments/${beneficiaryId}`, { status, leftAt }),
  getDiary: (activityId: number, from: string, to: string) =>
    request<Diary>('GET', `/activities/${activityId}/diary${query({ from, to })}`),
  listClassSessions: (from: string, to: string) =>
    request<ClassSession[]>('GET', `/class-sessions${query({ from, to })}`),

  registerAttendance: (batch: AttendanceBatchRequest) =>
    request<AttendanceBatchResponse>('POST', '/attendances/batch', batch),
  correctAttendance: (batch: AttendanceBatchRequest) =>
    request<AttendanceBatchResponse>('PUT', '/attendances/batch', batch),
  listAlerts: (activityId?: number) => request<DropoutAlert[]>('GET', `/dropout-alerts${query({ activityId })}`),
  getProgramOverview: (from: string, to: string) =>
    request<ProgramOverview>('GET', `/program-overview${query({ from, to })}`),
  getTotals: () => request<Totals>('GET', '/totals'),

  searchBeneficiaries: (search: BeneficiarySearch = {}) =>
    request<Beneficiary[]>('GET', `/beneficiaries${query({ ...search })}`),
  getBeneficiary: (id: number) => request<Beneficiary>('GET', `/beneficiaries/${id}`),
  createBeneficiary: (body: BeneficiaryRequest) => request<Beneficiary>('POST', '/beneficiaries', body),
  updateBeneficiary: (id: number, body: BeneficiaryRequest) =>
    request<Beneficiary>('PUT', `/beneficiaries/${id}`, body),
  deactivateBeneficiary: (id: number) => request<void>('DELETE', `/beneficiaries/${id}`),
  reactivateBeneficiary: (id: number) => request<Beneficiary>('POST', `/beneficiaries/${id}/reactivate`),

  listDocuments: (beneficiaryId: number) =>
    request<BeneficiaryDocument[]>('GET', `/beneficiaries/${beneficiaryId}/documents`),
  uploadDocument: (beneficiaryId: number, title: string, file: File) => {
    const form = new FormData();
    form.append('title', title);
    form.append('file', file);
    return request<BeneficiaryDocument>('POST', `/beneficiaries/${beneficiaryId}/documents`, form);
  },
  // Downloads need the token header, so the file comes as a blob instead of a plain link.
  downloadDocument: async (beneficiaryId: number, documentId: number) =>
    (await send('GET', `/beneficiaries/${beneficiaryId}/documents/${documentId}/content`)).blob(),
  deleteDocument: (beneficiaryId: number, documentId: number) =>
    request<void>('DELETE', `/beneficiaries/${beneficiaryId}/documents/${documentId}`),

  listDataRemovals: () => request<DataRemoval[]>('GET', '/organization/data-removals'),
  removeBeneficiaryData: (beneficiaryId: number, note: string) =>
    request<DataRemoval>('POST', `/organization/beneficiaries/${beneficiaryId}/data-removal`, { note }),

  listEvents: (from: string, to: string) => request<ZeloEvent[]>('GET', `/events${query({ from, to })}`),
  getEvent: (id: number) => request<ZeloEvent>('GET', `/events/${id}`),
  createEvent: (body: EventRequest) => request<ZeloEvent>('POST', '/events', body),
  updateEvent: (id: number, body: EventRequest) => request<ZeloEvent>('PUT', `/events/${id}`, body),
  completeEvent: (id: number) => request<ZeloEvent>('POST', `/events/${id}/complete`),
  cancelEvent: (id: number) => request<ZeloEvent>('POST', `/events/${id}/cancel`),
  listParticipants: (eventId: number) => request<Participant[]>('GET', `/events/${eventId}/participants`),
  registerParticipant: (eventId: number, beneficiaryId: number) =>
    request<Participant>('POST', `/events/${eventId}/participants`, { beneficiaryId }),
  markParticipant: (eventId: number, beneficiaryId: number, attended: boolean) =>
    request<Participant>('PUT', `/events/${eventId}/participants/${beneficiaryId}`, { attended }),
  unregisterParticipant: (eventId: number, beneficiaryId: number) =>
    request<void>('DELETE', `/events/${eventId}/participants/${beneficiaryId}`),

  listItems: () => request<AssistanceItem[]>('GET', '/assistance/items'),
  createItem: (name: string, description: string | null) =>
    request<AssistanceItem>('POST', '/assistance/items', { name, description }),
  updateItem: (id: number, name: string, description: string | null, active: boolean) =>
    request<AssistanceItem>('PUT', `/assistance/items/${id}`, { name, description, active }),
  listReceipts: (from: string, to: string) =>
    request<Receipt[]>('GET', `/assistance/receipts${query({ from, to })}`),
  createReceipt: (body: Omit<Receipt, 'id' | 'itemName' | 'recordedBy'>) =>
    request<Receipt>('POST', '/assistance/receipts', body),
  listDeliveries: (from: string, to: string, filters: { itemId?: number; beneficiaryName?: string } = {}) =>
    request<Delivery[]>('GET', `/assistance/deliveries${query({ from, to, ...filters })}`),
  listBeneficiaryDeliveries: (beneficiaryId: number) =>
    request<Delivery[]>('GET', `/assistance/deliveries${query({ beneficiaryId })}`),
  createDelivery: (body: Omit<Delivery, 'id' | 'itemName' | 'beneficiaryName' | 'recordedBy'>) =>
    request<Delivery>('POST', '/assistance/deliveries', body),
};
