import client from './client'

export const getCourts       = ()     => client.get('/courts')
export const getAvailability = (date) => client.get('/courts/availability', { params: { date } })
