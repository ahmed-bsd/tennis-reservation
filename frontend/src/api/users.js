import client from './client'


export const fetchUsers   = () => client.get('/users')