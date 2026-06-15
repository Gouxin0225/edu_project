<template>
  <div class="code-editor-wrap">
    <div ref="editorContainer" class="code-editor"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { autocompletion, closeBrackets } from '@codemirror/autocomplete'
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { cpp } from '@codemirror/lang-cpp'
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { sql } from '@codemirror/lang-sql'
import {
  HighlightStyle,
  StreamLanguage,
  bracketMatching,
  defaultHighlightStyle,
  foldGutter,
  indentOnInput,
  indentUnit,
  syntaxHighlighting
} from '@codemirror/language'
import { Compartment, EditorState, type Extension } from '@codemirror/state'
import {
  EditorView,
  crosshairCursor,
  drawSelection,
  dropCursor,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers,
  rectangularSelection
} from '@codemirror/view'
import { tags } from '@lezer/highlight'
import { shell } from '@codemirror/legacy-modes/mode/shell'

const props = withDefaults(defineProps<{
  modelValue: string
  language?: string
  height?: number
  fontSize?: number
  readOnly?: boolean
}>(), {
  language: 'python',
  height: 360,
  fontSize: 14,
  readOnly: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: []
}>()

const editorContainer = ref<HTMLDivElement | null>(null)
let editor: EditorView | null = null
let updatingFromEditor = false
const languageCompartment = new Compartment()
const readOnlyCompartment = new Compartment()

const normalizedLanguage = computed(() => normalizeLanguage(props.language))

function normalizeLanguage(language?: string) {
  const value = (language || '').toLowerCase()
  if (value === 'ts' || value === 'typescript') return 'typescript'
  if (value === 'js' || value === 'node' || value === 'web') return 'javascript'
  if (value === 'mysql') return 'sql'
  if (value === 'bash' || value === 'linux' || value === 'shellscript') return 'shell'
  if (value === 'c++') return 'cpp'
  return value || 'python'
}

function languageExtension(language: string): Extension {
  if (language === 'javascript') return javascript()
  if (language === 'typescript') return javascript({ typescript: true })
  if (language === 'python') return python()
  if (language === 'java') return java()
  if (language === 'cpp' || language === 'c' || language === 'c++') return cpp()
  if (language === 'sql') return sql()
  if (language === 'shell') return StreamLanguage.define(shell)
  return []
}

function readOnlyExtensions(readOnly: boolean): Extension {
  return [
    EditorState.readOnly.of(readOnly),
    EditorView.editable.of(!readOnly)
  ]
}

const codeHighlightStyle = HighlightStyle.define([
  { tag: tags.comment, color: '#7f8c8d' },
  { tag: tags.keyword, color: '#00ffff' },
  { tag: [tags.string, tags.regexp], color: '#39ff14' },
  { tag: tags.number, color: '#ffb86c' },
  { tag: [tags.variableName, tags.propertyName], color: '#e6edf3' },
  { tag: [tags.function(tags.variableName), tags.definition(tags.function(tags.variableName))], color: '#8ab4ff' },
  { tag: [tags.operator, tags.punctuation], color: '#c9d1d9' }
])

const editorTheme = EditorView.theme({
  '&': {
    height: '100%',
    backgroundColor: '#07090d',
    color: '#e6edf3',
    fontSize: `${props.fontSize}px`
  },
  '.cm-scroller': {
    fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
    lineHeight: `${Math.round(props.fontSize * 1.6)}px`,
    overflow: 'auto'
  },
  '.cm-content': {
    minHeight: '100%',
    padding: '12px 0',
    caretColor: '#ff10f0'
  },
  '.cm-line': {
    padding: '0 16px 0 8px'
  },
  '.cm-gutters': {
    backgroundColor: '#07090d',
    color: '#4b5563',
    borderRight: '1px solid #1a1a2e'
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#10203088',
    color: '#00ffff'
  },
  '.cm-activeLine': {
    backgroundColor: '#10203088'
  },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground, ::selection': {
    backgroundColor: '#1f6feb66'
  },
  '.cm-cursor': {
    borderLeftColor: '#ff10f0'
  },
  '.cm-foldGutter span': {
    color: '#64748b'
  },
  '.cm-tooltip': {
    backgroundColor: '#0a0a0a',
    border: '1px solid #1a1a2e',
    color: '#e6edf3'
  },
  '.cm-tooltip-autocomplete ul li[aria-selected]': {
    backgroundColor: '#123447',
    color: '#ffffff'
  }
}, { dark: true })

function createExtensions() {
  return [
    lineNumbers(),
    highlightActiveLineGutter(),
    foldGutter(),
    history(),
    drawSelection(),
    dropCursor(),
    EditorState.allowMultipleSelections.of(true),
    indentOnInput(),
    indentUnit.of('    '),
    EditorState.tabSize.of(4),
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    syntaxHighlighting(codeHighlightStyle),
    bracketMatching(),
    closeBrackets(),
    autocompletion(),
    rectangularSelection(),
    crosshairCursor(),
    highlightActiveLine(),
    keymap.of([indentWithTab, ...defaultKeymap, ...historyKeymap]),
    EditorView.lineWrapping,
    editorTheme,
    languageCompartment.of(languageExtension(normalizedLanguage.value)),
    readOnlyCompartment.of(readOnlyExtensions(props.readOnly)),
    EditorView.updateListener.of(update => {
      if (!update.docChanged || !editor) return
      updatingFromEditor = true
      emit('update:modelValue', update.state.doc.toString())
      updatingFromEditor = false
    }),
    EditorView.domEventHandlers({
      blur() {
        emit('blur')
      }
    })
  ]
}

onMounted(async () => {
  await nextTick()
  if (!editorContainer.value) return

  editor = new EditorView({
    parent: editorContainer.value,
    state: EditorState.create({
      doc: props.modelValue || '',
      extensions: createExtensions()
    })
  })
})

watch(() => props.modelValue, value => {
  if (!editor || updatingFromEditor) return
  const nextValue = value || ''
  if (editor.state.doc.toString() !== nextValue) {
    editor.dispatch({
      changes: {
        from: 0,
        to: editor.state.doc.length,
        insert: nextValue
      }
    })
  }
})

watch(normalizedLanguage, language => {
  editor?.dispatch({
    effects: languageCompartment.reconfigure(languageExtension(language))
  })
})

watch(() => props.readOnly, readOnly => {
  editor?.dispatch({
    effects: readOnlyCompartment.reconfigure(readOnlyExtensions(readOnly))
  })
})

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})
</script>

<style scoped>
.code-editor-wrap {
  width: 100%;
  height: v-bind('`${props.height}px`');
  background: #07090d;
  border: 1px solid #1a1a2e;
  box-shadow: inset 0 0 20px rgba(0, 255, 255, 0.04), 0 0 14px rgba(0, 255, 255, 0.1);
  overflow: hidden;
}

.code-editor {
  width: 100%;
  height: 100%;
}

.code-editor-wrap:focus-within {
  border-color: #00ffff;
  box-shadow: inset 0 0 20px rgba(0, 255, 255, 0.04), 0 0 18px rgba(0, 255, 255, 0.25);
}
</style>
