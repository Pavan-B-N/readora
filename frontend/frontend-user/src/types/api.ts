export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  path: string;
  traceId: string | null;
  timestamp: string;
  fieldErrors: FieldErrorItem[] | null;
}

export interface PageResponse<T> {
  items?: T[];
  content?: T[];
  page?: number;
  size?: number;
  totalElements: number;
  totalPages: number;
  number?: number;
}
