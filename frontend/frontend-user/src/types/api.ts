/** A single field-level validation failure within an ErrorResponse. */
export interface FieldErrorItem {
  field: string;
  message: string;
}

/** The standard error body shape returned by every Readora backend service. */
export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  path: string;
  traceId: string | null;
  timestamp: string;
  fieldErrors: FieldErrorItem[] | null;
}

/** A paginated list response — different backend services key the array as `items` vs. `content` and the page number as `page` vs. `number`, so both are optional and callers read whichever the endpoint actually sends. */
export interface PageResponse<T> {
  items?: T[];
  content?: T[];
  page?: number;
  size?: number;
  totalElements: number;
  totalPages: number;
  number?: number;
}
