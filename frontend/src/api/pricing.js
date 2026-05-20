import client from './client'

export const getPendingProposals = ()  => client.get('/pricing/proposals')
export const getAllProposals      = ()  => client.get('/pricing/proposals/all')
export const approveProposal     = id  => client.put(`/pricing/proposals/${id}/approve`)
export const rejectProposal      = id  => client.put(`/pricing/proposals/${id}/reject`)
