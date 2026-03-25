export type Role = 'PASSENGER' | 'DISPATCHER' | 'ADMIN';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  username: string;
  role: Role;
}

export interface SearchResult {
  routeNumber: string;
  stopName: string;
  frequencyPriority: number;
  stopPopularity: number;
  rankingScore: number;
  matchType: string;
}

export interface SearchResponse {
  query: string;
  suggestions: string[];
  results: SearchResult[];
}

export interface NotificationPreferences {
  username: string;
  arrivalReminderEnabled: boolean;
  reservationSuccessEnabled: boolean;
  reminderLeadMinutes: number;
  dndEnabled: boolean;
  dndStart: string | null;
  dndEnd: string | null;
  dndActiveNow: boolean;
}

export interface NotificationPreferencesRequest {
  arrivalReminderEnabled: boolean;
  reservationSuccessEnabled: boolean;
  reminderLeadMinutes: number;
  dndEnabled: boolean;
  dndStart: string | null;
  dndEnd: string | null;
}

export type WorkflowType =
  | 'ROUTE_DATA_CHANGE'
  | 'REMINDER_CONFIGURATION'
  | 'ABNORMAL_DATA_REVIEW';

export type WorkflowMode = 'CONDITIONAL' | 'JOINT' | 'PARALLEL';

export type WorkflowStatus =
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'RETURNED';

export interface WorkflowTask {
  id: number;
  type: WorkflowType;
  mode: WorkflowMode;
  status: WorkflowStatus;
  title: string;
  payload: string;
  submittedBy: string;
  currentStep: number;
  totalSteps: number;
  requiredApprovals: number;
  receivedApprovals: number;
  escalated: boolean;
  timeoutWarning: boolean;
  lastActionAt: string;
  createdAt: string;
}

export interface WorkflowProgressStep {
  label: string;
  completed: boolean;
  active: boolean;
}

export interface WorkflowDetails {
  task: WorkflowTask;
  progress: WorkflowProgressStep[];
}

export type UnifiedMessageType =
  | 'RESERVATION_SUCCESS'
  | 'ARRIVAL_REMINDER'
  | 'MISSED_CHECK_IN';

export interface UnifiedMessage {
  id: number;
  type: UnifiedMessageType;
  title: string;
  content: string;
  read: boolean;
  masked: boolean;
  createdAt: string;
}

export interface RuleWeights {
  relevanceWeight: number;
  frequencyWeight: number;
  popularityWeight: number;
  rankingMode: 'BLENDED' | 'STRICT_FREQUENCY_POPULARITY';
}

export interface TemplateConfig {
  id: number;
  templateKey: string;
  subject: string;
  body: string;
}

export interface DictionaryItem {
  id: number;
  category: string;
  code: string;
  value: string;
  enabled: boolean;
}

export interface ObservabilitySnapshot {
  queueBacklog: number;
  apiP95Ms: number;
  searchP95Ms: number;
  parsingP95Ms: number;
  queueP95Ms: number;
  healthEndpoint: string;
  metricsEndpoint: string;
}

export interface AlertDiagnostic {
  id: number;
  alertType: string;
  severity: string;
  message: string;
  createdAt: string;
}
