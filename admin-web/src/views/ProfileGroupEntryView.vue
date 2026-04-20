<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NImage,
  NInput,
  NSpace,
  NSwitch,
  NUpload,
  useMessage,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
} from 'naive-ui'
import { uploadAdminFile } from '../api/admin/files'
import {
  createSystemConfig,
  fetchSystemConfigs,
  updateSystemConfig,
  type SaveSystemConfigPayload,
} from '../api/admin/system-configs'
import type { AdminSystemConfig } from '../mock/platform-center'

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const qrUploadLoading = ref(false)
const qrUploadFileList = ref<UploadFileInfo[]>([])

const configMap = reactive<Record<string, AdminSystemConfig>>({})
const formModel = reactive({
  groupEntryEnabled: false,
  groupEntryText: '加入群聊',
  qrcodeUrl: '',
  descEnabled: false,
  descText: '',
})

const CONFIGS = {
  groupEntryEnabled: {
    key: 'mall.profile.group_entry_enabled',
    name: '个人中心群聊入口开关',
    value_type_code: 'BOOLEAN' as const,
    defaultValue: 'false',
    remark: '关闭时小程序不显示加入群聊入口',
  },
  groupEntryText: {
    key: 'mall.profile.group_entry_text',
    name: '个人中心群聊入口文案',
    value_type_code: 'STRING' as const,
    defaultValue: '加入群聊',
    remark: '小程序个人中心群聊入口显示文案',
  },
  qrcodeUrl: {
    key: 'mall.profile.group_entry_qrcode_url',
    name: '个人中心群聊二维码地址',
    value_type_code: 'STRING' as const,
    defaultValue: '',
    remark: '点击加入群聊后弹窗展示的二维码图片地址',
  },
  descEnabled: {
    key: 'mall.profile.group_entry_desc_enabled',
    name: '个人中心群聊说明文案开关',
    value_type_code: 'BOOLEAN' as const,
    defaultValue: 'false',
    remark: '是否显示加入群聊下方说明文字',
  },
  descText: {
    key: 'mall.profile.group_entry_desc_text',
    name: '个人中心群聊说明文案',
    value_type_code: 'STRING' as const,
    defaultValue: '',
    remark: '显示在加入群聊下方的小字说明',
  },
}

const previewQr = computed(() => formModel.qrcodeUrl.trim())

function parseSwitchValue(value: unknown): boolean {
  const raw = String(value == null ? '' : value).trim().toLowerCase()
  return ['1', 'true', 'yes', 'on', 'enabled'].includes(raw)
}

function buildSavePayload(configKey: string, configValue: string): SaveSystemConfigPayload | null {
  const configMeta = Object.values(CONFIGS).find((item) => item.key === configKey)
  if (!configMeta) return null
  return {
    config_key: configMeta.key,
    config_name: configMeta.name,
    config_value: configValue,
    value_type_code: configMeta.value_type_code,
    group_code: 'MALL_PROFILE',
    status_code: 'ENABLED',
    remark: configMeta.remark,
  }
}

async function loadConfig() {
  loading.value = true
  try {
    const page = await fetchSystemConfigs({
      pageNo: 1,
      pageSize: 200,
      keyword: 'mall.profile.group_entry',
    })
    const list = page?.list || []
    Object.keys(configMap).forEach((key) => delete configMap[key])
    list.forEach((item) => {
      configMap[item.config_key] = item
    })

    formModel.groupEntryEnabled = parseSwitchValue(configMap[CONFIGS.groupEntryEnabled.key]?.config_value ?? CONFIGS.groupEntryEnabled.defaultValue)
    formModel.groupEntryText = String(configMap[CONFIGS.groupEntryText.key]?.config_value ?? CONFIGS.groupEntryText.defaultValue)
    formModel.qrcodeUrl = String(configMap[CONFIGS.qrcodeUrl.key]?.config_value ?? CONFIGS.qrcodeUrl.defaultValue)
    formModel.descEnabled = parseSwitchValue(configMap[CONFIGS.descEnabled.key]?.config_value ?? CONFIGS.descEnabled.defaultValue)
    formModel.descText = String(configMap[CONFIGS.descText.key]?.config_value ?? CONFIGS.descText.defaultValue)
  } finally {
    loading.value = false
  }
}

async function upsertConfig(configKey: string, value: string) {
  const payload = buildSavePayload(configKey, value)
  if (!payload) return
  const current = configMap[configKey]
  if (current?.id) {
    await updateSystemConfig(current.id, payload)
    configMap[configKey] = { ...current, ...payload, updated_at: current.updated_at }
  } else {
    await createSystemConfig(payload)
  }
}

async function onSave() {
  const groupEntryText = formModel.groupEntryText.trim()
  const qrcodeUrl = formModel.qrcodeUrl.trim()
  const descText = formModel.descText.trim()

  if (!groupEntryText) {
    message.warning('请填写加入群聊文案')
    return
  }
  if (formModel.groupEntryEnabled && !qrcodeUrl) {
    message.warning('已开启群聊入口，请先上传或填写二维码地址')
    return
  }

  saving.value = true
  try {
    await Promise.all([
      upsertConfig(CONFIGS.groupEntryEnabled.key, String(formModel.groupEntryEnabled)),
      upsertConfig(CONFIGS.groupEntryText.key, groupEntryText),
      upsertConfig(CONFIGS.qrcodeUrl.key, qrcodeUrl),
      upsertConfig(CONFIGS.descEnabled.key, String(formModel.descEnabled)),
      upsertConfig(CONFIGS.descText.key, descText),
    ])
    message.success('群聊入口设置已保存')
    await loadConfig()
  } finally {
    saving.value = false
  }
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function onUploadQrImage(options: UploadCustomRequestOptions) {
  const rawFile = options.file.file
  if (!(rawFile instanceof File)) {
    options.onError()
    message.error('请选择图片文件')
    return
  }
  if (!rawFile.type.startsWith('image/')) {
    options.onError()
    message.warning('仅支持图片文件')
    return
  }

  qrUploadLoading.value = true
  try {
    const dataUrl = await readFileAsDataUrl(rawFile)
    const file = await uploadAdminFile({
      file_name: rawFile.name,
      file_url: dataUrl,
      mime_type: rawFile.type || 'image/png',
      file_size_kb: Math.max(1, Math.round(rawFile.size / 1024)),
    })
    formModel.qrcodeUrl = file.file_url
    options.onFinish()
    message.success('二维码上传成功')
  } catch {
    options.onError()
    message.error('二维码上传失败')
  } finally {
    qrUploadLoading.value = false
  }
}

function onRemoveQrImage() {
  formModel.qrcodeUrl = ''
  return true
}

onMounted(() => {
  void loadConfig()
})
</script>

<template>
  <NCard title="加入群聊设置">
    <NForm label-placement="left" label-width="156">
      <NFormItem label="显示加入群聊入口">
        <NSwitch v-model:value="formModel.groupEntryEnabled" />
      </NFormItem>

      <NFormItem label="入口文案" required>
        <NInput
          v-model:value="formModel.groupEntryText"
          maxlength="24"
          placeholder="例如：加入群聊"
        />
      </NFormItem>

      <NFormItem label="群二维码">
        <NSpace vertical style="width: 100%">
          <NUpload
            v-model:file-list="qrUploadFileList"
            :max="1"
            accept="image/*"
            :custom-request="onUploadQrImage"
            :default-upload="true"
            @remove="onRemoveQrImage"
          >
            <NButton :loading="qrUploadLoading">上传二维码图片</NButton>
          </NUpload>
          <NInput
            v-model:value="formModel.qrcodeUrl"
            placeholder="也可直接粘贴二维码图片地址"
          />
          <NImage
            v-if="previewQr"
            :src="previewQr"
            width="180"
            height="180"
            object-fit="cover"
            preview-disabled
          />
        </NSpace>
      </NFormItem>

      <NFormItem label="显示下方说明文字">
        <NSwitch v-model:value="formModel.descEnabled" />
      </NFormItem>

      <NFormItem label="说明文字内容">
        <NInput
          v-model:value="formModel.descText"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
          placeholder="显示在小程序加入群聊下面的小字说明"
        />
      </NFormItem>

      <NFormItem>
        <NSpace>
          <NButton :loading="loading" @click="loadConfig">刷新</NButton>
          <NButton type="primary" :loading="saving" @click="onSave">保存设置</NButton>
        </NSpace>
      </NFormItem>
    </NForm>
  </NCard>
</template>
