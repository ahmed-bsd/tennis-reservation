import client from './client'

export const createReservation  = data => client.post('/reservations', data)
export const cancelReservation  = id   => client.delete(`/reservations/${id}`)
export const myReservations     = ()   => client.get('/reservations/my')
export const allReservations     = ()   => client.get('/reservations/all')
export const confirmReservation = id   => client.put(`/reservations/${id}/confirm`)
