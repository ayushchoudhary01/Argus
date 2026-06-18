import axios from 'axios'
import type { AnalysisRequest, DivergenceReport } from '../types'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 300_000,
})

export const analyseEvent = async (request: AnalysisRequest): Promise<DivergenceReport> => {
  const { data } = await http.post<DivergenceReport>('/api/v1/analysis', request)
  return data
}