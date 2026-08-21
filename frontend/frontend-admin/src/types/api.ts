/** A single field-level validation failure within an ErrorResponse. */
export interface FieldErrorItem {
  field: string;
  message: string;
}

/** The backend's standard error body shape, with optional per-field validation errors. */
export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  path: string;
  traceId: string | null;
  timestamp: string;
  fieldErrors: FieldErrorItem[] | null;
}

/** Response shape for create endpoints that return only the new resource's id. */
export interface IdResponse {
  id: string;
}
