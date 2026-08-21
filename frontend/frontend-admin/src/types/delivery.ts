/** type is "DELIVERY" or "RETURN_PICKUP". */
export interface AgentActiveWork {
  type: 'DELIVERY' | 'RETURN_PICKUP';
  orderNumber: string;
  status: string;
  destinationCity: string | null;
}

/** A delivery agent as listed in the admin dashboard, with their current in-progress work if any. */
export interface AdminAgent {
  userId: string;
  name: string;
  phone: string | null;
  onDuty: boolean;
  activeWork: AgentActiveWork | null;
}
