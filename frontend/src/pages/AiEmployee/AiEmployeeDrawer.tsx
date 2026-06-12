import { useEffect } from 'react'
import { Drawer, Form, Input, Radio, TimePicker, InputNumber, Checkbox, Button, Space, message, Divider } from 'antd'
import dayjs from 'dayjs'
import { createEmployee, updateEmployee } from '../../services/aiEmployee'
import type { AiEmployee } from '../../services/aiEmployee'

const { TextArea } = Input

interface Props {
  open: boolean
  employee: AiEmployee | null
  onClose: () => void
  onSuccess: () => void
}

const WEEKDAY_OPTIONS = [
  { label: '一', value: '1' }, { label: '二', value: '2' }, { label: '三', value: '3' },
  { label: '四', value: '4' }, { label: '五', value: '5' }, { label: '六', value: '6' }, { label: '日', value: '7' },
]

export default function AiEmployeeDrawer({ open, employee, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const isEdit = !!employee?.id

  useEffect(() => {
    if (open) {
      if (employee?.id) {
        form.setFieldsValue({
          ...employee,
          serviceTimeStart: employee.serviceTimeStart ? dayjs(employee.serviceTimeStart, 'HH:mm:ss') : null,
          serviceTimeEnd: employee.serviceTimeEnd ? dayjs(employee.serviceTimeEnd, 'HH:mm:ss') : null,
          weekdays: employee.weekdays ? employee.weekdays.split(',') : [],
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          style: 'PROFESSIONAL', replyLength: 'MEDIUM',
          aggregateInterval: 3, delayInterval: 2,
        })
      }
    }
  }, [open, employee, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const data: any = { ...values }
      if (data.serviceTimeStart) data.serviceTimeStart = data.serviceTimeStart.format('HH:mm:ss')
      if (data.serviceTimeEnd) data.serviceTimeEnd = data.serviceTimeEnd.format('HH:mm:ss')
      if (Array.isArray(data.weekdays)) data.weekdays = data.weekdays.join(',')
      if (isEdit) {
        await updateEmployee(employee!.id!, data)
        message.success('更新成功')
      } else {
        await createEmployee(data)
        message.success('创建成功')
      }
      onSuccess()
      onClose()
    } catch (e: any) {
      if (e?.errorFields) return // Form validation error, Ant Design handles display
      message.error(e?.message ?? '保存失败')
    }
  }

  return (
    <Drawer title={isEdit ? '编辑 AI 员工' : '新建 AI 员工'} open={open} onClose={onClose} width={560}
      extra={<Space><Button onClick={onClose}>取消</Button><Button type="primary" onClick={handleSubmit}>保存</Button></Space>}>
      <Form form={form} layout="vertical" size="middle">
        <Form.Item name="name" label="员工名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="如：小通" />
        </Form.Item>
        <Form.Item name="style" label="接待风格" rules={[{ required: true }]}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value="PROFESSIONAL">专业</Radio.Button>
            <Radio.Button value="WARM">温暖</Radio.Button>
            <Radio.Button value="ENTHUSIASTIC">热情</Radio.Button>
            <Radio.Button value="RELIABLE">可靠</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="replyLength" label="回复字数" rules={[{ required: true }]}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value="SHORT">简短</Radio.Button>
            <Radio.Button value="MEDIUM">中等</Radio.Button>
            <Radio.Button value="DETAIL">详细</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="greetingMsg" label="开场白">
          <TextArea rows={2} placeholder="您好，请问有什么可以帮您？" />
        </Form.Item>
        <Form.Item name="companyIntro" label="公司介绍" rules={[{ required: true }]}>
          <TextArea rows={2} placeholder="公司主营业务描述..." />
        </Form.Item>
        <Form.Item name="productIntro" label="产品/服务介绍" rules={[{ required: true }]}>
          <TextArea rows={2} placeholder="主要产品和服务..." />
        </Form.Item>
        <Form.Item name="serviceScope" label="服务对象（选填）">
          <Input placeholder="仅接待XX行业客户" />
        </Form.Item>
        <Divider>服务配置</Divider>
        <Form.Item name="weekdays" label="服务日">
          <Checkbox.Group options={WEEKDAY_OPTIONS} />
        </Form.Item>
        <Space>
          <Form.Item name="serviceTimeStart" label="开始时间">
            <TimePicker format="HH:mm" />
          </Form.Item>
          <Form.Item name="serviceTimeEnd" label="结束时间">
            <TimePicker format="HH:mm" />
          </Form.Item>
        </Space>
        <Space>
          <Form.Item name="aggregateInterval" label="聚合间隔(秒)">
            <InputNumber min={1} max={10} />
          </Form.Item>
          <Form.Item name="delayInterval" label="延迟回复(秒)">
            <InputNumber min={1} max={10} />
          </Form.Item>
        </Space>
        <Form.Item name="contentCheck" label="敏感词规则 (JSON)" extra='如: {"微信":"{V}","QQ":"{Q}"}'>
          <TextArea rows={2} style={{ fontFamily: 'monospace' }} />
        </Form.Item>
      </Form>
    </Drawer>
  )
}
