import api from './api'

export interface KnowledgeBase {
  id?: number
  name: string
  description?: string
  employeeId?: number
  documentCount?: number
}

export interface KnowledgeDocument {
  id?: number
  kbId: number
  fileName: string
  fileType: string
  fileSize?: number
  status: string
  chunkCount: number
  createdAt?: string
}

export interface KnowledgeChunk {
  id?: number
  docId: number
  kbId: number
  content: string
  chunkIndex: number
  esDocId?: string
  embeddingStatus: string
}

export async function listKBs(): Promise<KnowledgeBase[]> {
  const res = await api.get<any, { code: number; message: string; data: KnowledgeBase[] }>('/knowledge-bases')
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function createKB(data: KnowledgeBase): Promise<KnowledgeBase> {
  const res = await api.post<any, { code: number; message: string; data: KnowledgeBase }>('/knowledge-bases', data)
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function uploadDoc(kbId: number, file: File): Promise<KnowledgeDocument> {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post<any, { code: number; message: string; data: KnowledgeDocument }>(
    `/knowledge-bases/${kbId}/documents`,
    form,
  )
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function listDocs(kbId: number): Promise<KnowledgeDocument[]> {
  const res = await api.get<any, { code: number; message: string; data: KnowledgeDocument[] }>(
    `/knowledge-bases/${kbId}/documents`,
  )
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}

export async function listChunks(kbId: number, docId: number): Promise<KnowledgeChunk[]> {
  const res = await api.get<any, { code: number; message: string; data: KnowledgeChunk[] }>(
    `/knowledge-bases/${kbId}/documents/${docId}/chunks`,
  )
  if (res.code !== 0) throw new Error(res.message)
  return res.data
}
