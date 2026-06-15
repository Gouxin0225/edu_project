import { ElMessage } from 'element-plus/es/components/message/index'

export interface ExportColumn<T extends Record<string, any>> {
  label: string
  prop: keyof T | ((row: T) => any)
}

export async function exportRowsToXlsx<T extends Record<string, any>>(
  filename: string,
  columns: ExportColumn<T>[],
  rows: T[]
) {
  if (!rows.length) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  const XLSX = await import('xlsx')
  const data = rows.map(row => {
    const item: Record<string, any> = {}
    columns.forEach(column => {
      item[column.label] = typeof column.prop === 'function' ? column.prop(row) : row[column.prop]
    })
    return item
  })
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '名单')
  XLSX.writeFile(wb, filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`)
}
