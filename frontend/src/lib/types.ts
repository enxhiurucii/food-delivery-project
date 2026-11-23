// ---------- Back office ----------
export interface Me {
  subject: string;
  username: string;
  name: string | null;
  roles: string[];
  restaurantId: string | null;
}

export interface Restaurant {
  id: string;
  name: string;
  description: string | null;
  address: string;
  phone: string | null;
  imageUrl: string | null;
  timezone: string;
  acceptingOrders: boolean;
  pickupEnabled: boolean;
  deliveryEnabled: boolean;
  deliveryFee: number;
  minimumOrder: number;
  estimatedPrepMinutes: number;
  currency: string;
}

export interface MenuItem {
  id: string;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  imageUrl: string | null;
  available: boolean;
}

export interface Menu {
  id: string;
  name: string;
  description: string | null;
  position: number;
  items: MenuItem[];
}

export interface Address {
  street: string;
  city: string;
  postalCode: string | null;
  phone: string;
  instructions: string | null;
}

export interface StaffOrder {
  id: string;
  restaurantId: string;
  customerName: string;
  lines: { menuItemId: string; name: string; unitPrice: number; quantity: number; total: number }[];
  subtotal: number;
  deliveryFee: number;
  total: number;
  currency: string;
  fulfillmentType: "PICKUP" | "DELIVERY";
  deliveryAddress: Address | null;
  notes: string | null;
  paymentMethod: string;
  paymentTiming: string;
  paymentStatus: string;
  status: string;
  allowedNextStatuses: string[];
  statusHistory: { status: string; at: string }[];
  placedAt: string;
  closedAt: string | null;
}

export interface Breakdown {
  key: string;
  orders: number;
  revenue: number;
}

export interface DailyReport {
  date: string;
  timezone: string;
  orders: number;
  revenue: number;
  averageOrderValue: number;
  currency: string;
  byFulfillmentType: Breakdown[];
  byPaymentMethod: Breakdown[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StaffUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: "MANAGER" | "DRIVER";
  restaurantId: string | null;
}

// ---------- Webshop ----------
export interface ShopRestaurant {
  id: string;
  name: string;
  description: string | null;
  address: string;
  imageUrl: string | null;
  open: boolean;
  pickupAvailable: boolean;
  deliveryAvailable: boolean;
  deliveryFee: number;
  minimumOrder: number;
  estimatedPrepMinutes: number;
  currency: string;
}

export interface Cart {
  restaurantId: string | null;
  items: { menuItemId: string; name: string; unitPrice: number; quantity: number; total: number }[];
  subtotal: number;
  currency: string;
}

export interface TimelineStep {
  status: string;
  reached: boolean;
  current: boolean;
  at: string | null;
}

export interface CustomerOrder {
  id: string;
  restaurantId: string;
  restaurantName: string;
  lines: { name: string; unitPrice: number; quantity: number; total: number }[];
  subtotal: number;
  deliveryFee: number;
  total: number;
  currency: string;
  fulfillmentType: "PICKUP" | "DELIVERY";
  deliveryAddress: Address | null;
  notes: string | null;
  paymentMethod: string;
  paymentTiming: string;
  paymentStatus: string;
  status: string;
  finished: boolean;
  timeline: TimelineStep[];
  placedAt: string;
  estimatedReadyAt: string | null;
}

export interface DriverOrder {
  id: string;
  restaurantId: string;
  restaurantName: string;
  restaurantAddress: string;
  customerName: string;
  lines: { name: string; quantity: number; total: number }[];
  total: number;
  currency: string;
  deliveryAddress: Address;
  notes: string | null;
  paymentMethod: string;
  paymentTiming: string;
  paymentStatus: string;
  status: string;
  allowedNextStatuses: string[];
  driverId: string | null;
  driverName: string | null;
  placedAt: string;
}
