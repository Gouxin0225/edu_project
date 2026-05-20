import axios from 'axios'

export async function downloadAuthorizedFile(url: string, fallbackFileName: string) {
  const token = localStorage.getItem('token')
  const response = await axios.get(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    responseType: 'blob'
  })

  const fileName = getFileName(response.headers['content-disposition']) || fallbackFileName
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
}

function getFileName(disposition?: string) {
  if (!disposition) return ''
  const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] ? decodeURIComponent(plainMatch[1]) : ''
}
