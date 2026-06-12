import { useState, useEffect, useCallback } from 'react'
import {
  Typography, Layout, List, Button, Table, Input, Tag, Space, Modal,
  Upload, Steps, Drawer, Card, message, Spin, Empty, Tooltip, Popconfirm,
} from 'antd'
import {
  BookOutlined, PlusOutlined, UploadOutlined, InboxOutlined,
  FileOutlined, FileTextOutlined, FileImageOutlined,
  FilePdfOutlined, FileWordOutlined, FileExcelOutlined,
  SearchOutlined, DeleteOutlined, EyeOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import {
  listKBs, createKB, uploadDoc, listDocs, listChunks,
  KnowledgeBase, KnowledgeDocument, KnowledgeChunk,
} from '../../services/knowledge'

const { Title, Text } = Typography
const { Dragger } = Upload
const { Sider, Content } = Layout

/* ───────── Status helpers ───────── */

const DOC_STATUS_MAP: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'default', label: '待处理' },
  PROCESSING: { color: 'processing', label: '处理中' },
  COMPLETED: { color: 'success', label: '已完成' },
  FAILED: { color: 'error', label: '失败' },
}

const EMBEDDING_STATUS_MAP: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'default', label: '待向量化' },
  PROCESSING: { color: 'processing', label: '向量化中' },
  COMPLETED: { color: 'success', label: '已向量化' },
  FAILED: { color: 'error', label: '失败' },
}

function fileTypeIcon(fileType: string) {
  const t = fileType?.toLowerCase()
  if (t === 'pdf') return <FilePdfOutlined style={{ color: '#f5222d' }} />
  if (['doc', 'docx'].includes(t)) return <FileWordOutlined style={{ color: '#1677ff' }} />
  if (['xls', 'xlsx'].includes(t)) return <FileExcelOutlined style={{ color: '#52c41a' }} />
  if (['png', 'jpg', 'jpeg', 'gif', 'svg'].includes(t)) return <FileImageOutlined style={{ color: '#722ed1' }} />
  if (['txt'].includes(t)) return <FileTextOutlined style={{ color: '#fa8c16' }} />
  return <FileOutlined />
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const UPLOAD_STEPS = ['解析', '分片', '向量化', '索引']

/* ───────── Main Component ───────── */

export default function KnowledgePage() {
  /* State */
  const [kbList, setKbList] = useState<KnowledgeBase[]>([])
  const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null)
  const [kbLoading, setKbLoading] = useState(false)
  const [docs, setDocs] = useState<KnowledgeDocument[]>([])
  const [docsLoading, setDocsLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [newKbName, setNewKbName] = useState('')
  const [newKbDesc, setNewKbDesc] = useState('')
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(-1) // -1=not started, 0..3 for steps
  const [chunkDrawerOpen, setChunkDrawerOpen] = useState(false)
  const [chunkDoc, setChunkDoc] = useState<KnowledgeDocument | null>(null)
  const [chunks, setChunks] = useState<KnowledgeChunk[]>([])
  const [chunksLoading, setChunksLoading] = useState(false)

  /* Fetch KB list */
  const fetchKBs = useCallback(async () => {
    setKbLoading(true)
    try {
      const data = await listKBs()
      setKbList(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取知识库列表失败')
    } finally {
      setKbLoading(false)
    }
  }, [])

  useEffect(() => { fetchKBs() }, [fetchKBs])

  /* Fetch documents when KB selected */
  const fetchDocs = useCallback(async (kbId: number) => {
    setDocsLoading(true)
    try {
      const data = await listDocs(kbId)
      setDocs(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取文档列表失败')
    } finally {
      setDocsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedKb?.id) {
      fetchDocs(selectedKb.id)
    } else {
      setDocs([])
    }
  }, [selectedKb, fetchDocs])

  /* Create KB */
  const handleCreateKB = async () => {
    if (!newKbName.trim()) {
      message.warning('请输入知识库名称')
      return
    }
    try {
      await createKB({ name: newKbName.trim(), description: newKbDesc.trim() })
      message.success('知识库创建成功')
      setCreateModalOpen(false)
      setNewKbName('')
      setNewKbDesc('')
      fetchKBs()
    } catch (e: any) {
      message.error(e?.message ?? '创建失败')
    }
  }

  /* Upload document */
  const handleUpload = async (file: File) => {
    if (!selectedKb?.id) return
    setUploading(true)
    setUploadProgress(0)
    try {
      await uploadDoc(selectedKb.id, file)
      // Simulate progress through processing stages
      setUploadProgress(1)
      await new Promise((r) => setTimeout(r, 600))
      setUploadProgress(2)
      await new Promise((r) => setTimeout(r, 600))
      setUploadProgress(3)
      await new Promise((r) => setTimeout(r, 600))
      message.success('上传成功，文档正在后台处理')
      setUploadModalOpen(false)
      setUploadProgress(-1)
      fetchDocs(selectedKb.id)
    } catch (e: any) {
      message.error(e?.message ?? '上传失败')
      setUploadProgress(-1)
    } finally {
      setUploading(false)
    }
  }

  /* View chunks */
  const handleViewChunks = async (doc: KnowledgeDocument) => {
    if (!selectedKb?.id) return
    setChunkDoc(doc)
    setChunkDrawerOpen(true)
    setChunksLoading(true)
    try {
      const data = await listChunks(selectedKb.id!, doc.id!)
      setChunks(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取分片失败')
      setChunks([])
    } finally {
      setChunksLoading(false)
    }
  }

  /* Upload modal file select callback */
  const handleUploadFile = (file: File) => {
    handleUpload(file)
    return false // Prevent default upload behavior
  }

  /* Filtered docs */
  const filteredDocs = docs.filter(
    (d) => !searchText || d.fileName?.toLowerCase().includes(searchText.toLowerCase()),
  )

  /* Document table columns */
  const docColumns: ColumnsType<KnowledgeDocument> = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      width: 280,
      render: (name: string, record: KnowledgeDocument) => (
        <Space>
          {fileTypeIcon(record.fileType)}
          <span style={{ wordBreak: 'break-all' }}>{name}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'fileType',
      width: 80,
      render: (t: string) => <Tag>{t?.toUpperCase()}</Tag>,
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 100,
      render: (s: number) => formatFileSize(s),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: string) => {
        const m = DOC_STATUS_MAP[s]
        return <Tag color={m?.color}>{m?.label ?? s}</Tag>
      },
    },
    {
      title: '分片数',
      dataIndex: 'chunkCount',
      width: 80,
      render: (n: number) => n ?? 0,
    },
    {
      title: '上传时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (t: string) => t ?? '-',
    },
    {
      title: '操作',
      width: 100,
      render: (_: any, record: KnowledgeDocument) => (
        <Space>
          <Tooltip title="查看分片">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewChunks(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ height: '100%' }}>
      <Title level={4}><BookOutlined /> 知识库管理</Title>
      <Layout style={{ background: 'transparent', minHeight: 'calc(100vh - 160px)' }}>
        {/* ─── Left Panel: KB List ─── */}
        <Sider
          width={280}
          style={{
            background: '#fff',
            borderRadius: 8,
            padding: 16,
            marginRight: 16,
            border: '1px solid #f0f0f0',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Text strong>知识库</Text>
            <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
              新建
            </Button>
          </div>
          <Spin spinning={kbLoading}>
            {kbList.length === 0 && !kbLoading ? (
              <Empty description="暂无知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <List
                dataSource={kbList}
                renderItem={(kb) => (
                  <List.Item
                    key={kb.id}
                    onClick={() => setSelectedKb(kb)}
                    style={{
                      cursor: 'pointer',
                      padding: '10px 12px',
                      borderRadius: 6,
                      background: selectedKb?.id === kb.id ? '#e6f4ff' : undefined,
                      border: selectedKb?.id === kb.id ? '1px solid #91caff' : '1px solid transparent',
                      marginBottom: 4,
                    }}
                  >
                    <List.Item.Meta
                      avatar={<BookOutlined style={{ fontSize: 18, color: '#1677ff' }} />}
                      title={<Text strong>{kb.name}</Text>}
                      description={
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {(kb.documentCount ?? 0)} 个文档
                          {kb.description ? ` | ${kb.description}` : ''}
                        </Text>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </Spin>
        </Sider>

        {/* ─── Right Panel: Document Table ─── */}
        <Content style={{ background: '#fff', borderRadius: 8, padding: 16, border: '1px solid #f0f0f0' }}>
          {!selectedKb ? (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300 }}>
              <Empty description="请从左侧选择一个知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          ) : (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <Space>
                  <Text strong style={{ fontSize: 16 }}>{selectedKb.name}</Text>
                  {selectedKb.description && (
                    <Text type="secondary">({selectedKb.description})</Text>
                  )}
                </Space>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
                <Input.Search
                  placeholder="搜索文件名..."
                  prefix={<SearchOutlined />}
                  onSearch={setSearchText}
                  style={{ width: 240 }}
                  allowClear
                />
                <Button type="primary" icon={<UploadOutlined />} onClick={() => setUploadModalOpen(true)}>
                  上传文档
                </Button>
              </div>
              <Table
                columns={docColumns}
                dataSource={filteredDocs}
                rowKey="id"
                loading={docsLoading}
                size="middle"
                locale={{ emptyText: <Empty description="暂无文档，点击上方按钮上传" /> }}
              />
            </>
          )}
        </Content>
      </Layout>

      {/* ─── Create KB Modal ─── */}
      <Modal
        title="新建知识库"
        open={createModalOpen}
        onOk={handleCreateKB}
        onCancel={() => { setCreateModalOpen(false); setNewKbName(''); setNewKbDesc('') }}
        okText="创建"
        cancelText="取消"
      >
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 12 }}>
            <Text strong>名称</Text>
            <Input
              placeholder="输入知识库名称"
              value={newKbName}
              onChange={(e) => setNewKbName(e.target.value)}
              style={{ marginTop: 4 }}
            />
          </div>
          <div>
            <Text strong>描述（选填）</Text>
            <Input.TextArea
              placeholder="输入知识库描述"
              value={newKbDesc}
              onChange={(e) => setNewKbDesc(e.target.value)}
              rows={3}
              style={{ marginTop: 4 }}
            />
          </div>
        </div>
      </Modal>

      {/* ─── Upload Modal ─── */}
      <Modal
        title="上传文档"
        open={uploadModalOpen}
        onCancel={() => { if (!uploading) { setUploadModalOpen(false); setUploadProgress(-1) } }}
        footer={null}
        destroyOnClose
        maskClosable={!uploading}
        closable={!uploading}
      >
        <div style={{ padding: '16px 0' }}>
          {uploadProgress === -1 ? (
            <Dragger
              accept=".pdf,.doc,.docx,.txt,.md,.csv,.xls,.xlsx,.png,.jpg,.jpeg,.gif"
              beforeUpload={handleUploadFile}
              showUploadList={false}
              disabled={uploading}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p className="ant-upload-hint">
                支持 PDF、Word、TXT、Markdown、Excel、图片等格式
              </p>
            </Dragger>
          ) : (
            <div style={{ textAlign: 'center' }}>
              <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 24 }}>
                文档上传中，正在后台处理...
              </Text>
              <Steps
                current={uploadProgress}
                direction="vertical"
                size="small"
                items={UPLOAD_STEPS.map((label) => ({ title: label }))}
                style={{ maxWidth: 200, margin: '0 auto' }}
              />
            </div>
          )}
        </div>
      </Modal>

      {/* ─── Chunk Drawer ─── */}
      <Drawer
        title={chunkDoc ? `分片详情 - ${chunkDoc.fileName}` : '分片详情'}
        open={chunkDrawerOpen}
        onClose={() => { setChunkDrawerOpen(false); setChunks([]) }}
        width={560}
      >
        <Spin spinning={chunksLoading}>
          {chunks.length === 0 && !chunksLoading ? (
            <Empty description="暂无分片数据" />
          ) : (
            chunks.map((chunk, idx) => (
              <Card
                key={chunk.id ?? idx}
                size="small"
                style={{ marginBottom: 12 }}
                title={
                  <Space>
                    <Text strong>分片 #{chunk.chunkIndex + 1}</Text>
                    <Tag color={EMBEDDING_STATUS_MAP[chunk.embeddingStatus]?.color}>
                      {EMBEDDING_STATUS_MAP[chunk.embeddingStatus]?.label ?? chunk.embeddingStatus}
                    </Tag>
                  </Space>
                }
                extra={
                  chunk.esDocId ? (
                    <Tooltip title={`ES ID: ${chunk.esDocId}`}>
                      <Text copyable={{ text: chunk.esDocId }} style={{ fontSize: 12, fontFamily: 'monospace' }}>
                        {chunk.esDocId.substring(0, 12)}...
                      </Text>
                    </Tooltip>
                  ) : null
                }
              >
                <div
                  style={{
                    background: '#fafafa',
                    padding: 8,
                    borderRadius: 4,
                    maxHeight: 120,
                    overflow: 'auto',
                    fontSize: 13,
                    lineHeight: 1.6,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                  }}
                >
                  {chunk.content}
                </div>
                <div style={{ marginTop: 8, textAlign: 'right' }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    约 {chunk.content?.length ?? 0} 字
                  </Text>
                </div>
              </Card>
            ))
          )}
        </Spin>
      </Drawer>
    </div>
  )
}
