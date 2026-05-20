import client from './client'

export const getBalance = ()           => client.get('/jetons/balance')
export const topup      = (userId, amount) => client.post('/jetons/topup', { userId, amount })
