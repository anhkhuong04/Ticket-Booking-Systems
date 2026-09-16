import { apiClient } from '../../shared/api/apiClient'

export type CustomerProfile = { fullName: string; email: string; phone: string | null; birthDate: string | null }
export type BillingPreferences = { recipientType: 'PERSONAL' | 'BUSINESS'; recipientName: string; taxCode: string | null; address: string | null; email: string }

export async function getProfile(): Promise<CustomerProfile> {
  return (await apiClient.get<CustomerProfile>('/api/me/profile')).data
}

export async function updateProfile(value: Pick<CustomerProfile, 'fullName' | 'phone' | 'birthDate'>): Promise<CustomerProfile> {
  return (await apiClient.put<CustomerProfile>('/api/me/profile', value)).data
}

export async function getBillingPreferences(): Promise<BillingPreferences | null> {
  return (await apiClient.get<BillingPreferences | null>('/api/me/billing-preferences')).data
}

export async function updateBillingPreferences(value: BillingPreferences): Promise<BillingPreferences> {
  return (await apiClient.put<BillingPreferences>('/api/me/billing-preferences', value)).data
}
