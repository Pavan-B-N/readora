/** type is "DELIVERY" or "RETURN_PICKUP". */
export interface AgentActiveWork {
  type: 'DELIVERY' | 'RETURN_PICKUP';
  orderNumber: string;
  status: string;
  destinationCity: string | null;
}

export interface AdminAgent {
  userId: string;
  name: string;
  phone: string | null;
  onDuty: boolean;
  activeWork: AgentActiveWork | null;
}
