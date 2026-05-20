<template>
  <div class="scsa-practice-page">
    <header class="practice-header">
      <div class="title-block">
        <div class="title-icon">
          <el-icon><Reading /></el-icon>
        </div>
        <div>
          <h1>SCSA-S 理论题练习答疑</h1>
          <p>单选、多选、判断题集中训练，围绕当前题目即时查看解析与答疑。</p>
          <p class="source-line">题目来源：SCSA-S_500道理论题_选项已打乱_答案已修正.md</p>
        </div>
      </div>

      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="resetCurrent">重做本题</el-button>
        <el-button :icon="Delete" @click="resetAll">清空进度</el-button>
        <el-button type="primary" :icon="Guide" @click="goNextUnanswered">继续练习</el-button>
      </div>
    </header>

    <section class="stats-strip">
      <div class="stat-item">
        <span>剩余题量</span>
        <strong>{{ activeQuestionBank.length }}</strong>
      </div>
      <div class="stat-item success">
        <span>正确</span>
        <strong>{{ progressStats.correct }}</strong>
      </div>
      <div class="stat-item danger">
        <span>错题</span>
        <strong>{{ progressStats.wrong }}</strong>
      </div>
      <div class="stat-item">
        <span>正确率</span>
        <strong>{{ progressStats.accuracy }}%</strong>
      </div>
      <div class="progress-bar">
        <span>完成度</span>
        <el-progress :percentage="progressStats.finishedRate" :stroke-width="8" :show-text="false" />
      </div>
    </section>

    <main class="practice-main">
      <aside class="panel question-bank-panel">
        <div class="panel-title">
          <h2>题目筛选</h2>
          <el-tag effect="dark">{{ filteredQuestions.length }} 题</el-tag>
        </div>

        <div class="filter-stack">
          <el-input v-model="filters.keyword" :prefix-icon="Search" placeholder="搜索题干或知识点" clearable />

          <el-segmented v-model="filters.type" :options="typeOptions" block />

          <el-select v-model="filters.knowledge" placeholder="知识点" clearable>
            <el-option label="全部知识点" value="ALL" />
            <el-option v-for="item in knowledgeOptions" :key="item" :label="item" :value="item" />
          </el-select>

          <el-select v-model="filters.difficulty" placeholder="难度" clearable>
            <el-option label="全部难度" value="ALL" />
            <el-option v-for="item in difficultyOptions" :key="item" :label="item" :value="item" />
          </el-select>

          <div class="switch-line">
            <span>只看错题</span>
            <el-switch v-model="filters.onlyWrong" />
          </div>
        </div>

        <div class="question-list">
          <button
            v-for="(item, index) in filteredQuestions"
            :key="item.id"
            class="question-index"
            :class="getQuestionIndexClass(item)"
            @click="goToQuestion(item.id)"
          >
            <span class="index-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="index-main">
              <span>{{ typeLabelMap[item.type] }}</span>
              <strong>{{ item.knowledge }}</strong>
            </span>
            <el-icon v-if="getAnswerState(item.id).starred"><StarFilled /></el-icon>
          </button>

          <el-empty v-if="filteredQuestions.length === 0" description="没有匹配的题目" :image-size="88" />
        </div>
      </aside>

      <section v-if="currentQuestion" class="panel question-panel">
        <div class="question-topline">
          <div class="question-tags">
            <el-tag>{{ typeLabelMap[currentQuestion.type] }}</el-tag>
            <el-tag type="warning">{{ currentQuestion.difficulty }}</el-tag>
            <el-tag type="success">{{ currentQuestion.knowledge }}</el-tag>
          </div>
          <button class="star-button" :class="{ active: currentState.starred }" @click="toggleStar(currentQuestion.id)">
            <el-icon><component :is="currentState.starred ? StarFilled : Star" /></el-icon>
            <span>收藏</span>
          </button>
        </div>

        <div class="question-title-row">
          <span class="question-order">第 {{ currentQuestionNumber }} 题</span>
          <h2>{{ currentQuestion.stem }}</h2>
        </div>

        <div class="option-list">
          <button
            v-for="option in currentQuestion.options"
            :key="option.key"
            class="option-button"
            :class="getOptionClass(currentQuestion, option.key)"
            @click="selectOption(option.key)"
          >
            <span class="option-key">{{ option.key }}</span>
            <span class="option-text">{{ option.text }}</span>
            <el-icon v-if="currentState.submitted && currentQuestion.answer.includes(option.key)" class="option-result">
              <Check />
            </el-icon>
            <el-icon v-else-if="currentState.submitted && currentState.selected.includes(option.key)" class="option-result wrong">
              <Close />
            </el-icon>
          </button>
        </div>

        <div class="answer-actions">
          <div class="selected-summary">
            已选：
            <strong>{{ currentState.selected.length ? currentState.selected.join('、') : '未选择' }}</strong>
          </div>
          <div class="action-buttons">
            <el-button :icon="Back" @click="previousQuestion">上一题</el-button>
            <el-button :icon="RefreshRight" @click="resetCurrent">重做</el-button>
            <el-button type="primary" :icon="CircleCheck" :disabled="!canSubmitCurrent" @click="submitCurrentAnswer">
              提交答案
            </el-button>
            <el-button :icon="Right" @click="nextQuestion">下一题</el-button>
          </div>
        </div>

        <section v-if="currentState.submitted" class="analysis-box" :class="{ correct: currentState.correct, wrong: !currentState.correct }">
          <div class="analysis-header">
            <div>
              <span class="result-pill">{{ currentState.correct ? '回答正确' : '需要复盘' }}</span>
              <strong>标准答案：{{ formatAnswer(currentQuestion.answer) }}</strong>
            </div>
            <el-button text :icon="ChatLineRound" @click="askTutor('为什么我的答案不对？')">问答疑助手</el-button>
          </div>
          <p>{{ currentQuestion.explanation }}</p>
          <div class="option-notes">
            <div v-for="option in currentQuestion.options" :key="option.key" class="note-item">
              <span>{{ option.key }}</span>
              <p>{{ currentQuestion.optionNotes[option.key] }}</p>
            </div>
          </div>
        </section>
      </section>

      <aside v-if="currentQuestion" class="panel tutor-panel">
        <div class="panel-title">
          <h2>在线答疑</h2>
          <el-tag type="success" effect="dark">当前题</el-tag>
        </div>

        <div class="knowledge-card">
          <span>当前考点</span>
          <strong>{{ currentQuestion.knowledge }}</strong>
          <p>{{ currentQuestion.askGuide }}</p>
        </div>

        <div class="quick-ask">
          <el-button v-for="item in quickAskItems" :key="item" size="small" @click="askTutor(item)">
            {{ item }}
          </el-button>
        </div>

        <div class="chat-window">
          <div
            v-for="message in chatMessages"
            :key="message.id"
            class="chat-message"
            :class="message.role"
          >
            <div class="message-label">{{ message.role === 'assistant' ? '答疑助手' : '我' }}</div>
            <p>{{ message.content }}</p>
          </div>
        </div>

        <div class="ask-box">
          <el-input
            v-model="askInput"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="例如：为什么这里不能选 B？"
            @keydown.ctrl.enter.prevent="askTutor()"
          />
          <el-button type="primary" :icon="Promotion" :disabled="!askInput.trim()" @click="askTutor()">发送</el-button>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  ChatLineRound,
  Check,
  CircleCheck,
  Close,
  Delete,
  Guide,
  Promotion,
  Reading,
  RefreshRight,
  Right,
  Search,
  Star,
  StarFilled
} from '@element-plus/icons-vue'
import scsaOutline from '@/data/scsa-s-choice-question-outline.md?raw'

type QuestionType = 'single' | 'multiple' | 'judge'
type Difficulty = '基础' | '进阶' | '易错'

interface PracticeOption {
  key: string
  text: string
}

interface PracticeQuestion {
  id: number
  type: QuestionType
  difficulty: Difficulty
  knowledge: string
  stem: string
  options: PracticeOption[]
  answer: string[]
  explanation: string
  optionNotes: Record<string, string>
  askGuide: string
}

interface AnswerState {
  selected: string[]
  submitted: boolean
  correct?: boolean
  starred?: boolean
  lastAt?: string
}

interface ChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

const STORAGE_KEY = 'scsa-s-practice-progress-500-v1'

const legacyQuestionBank: PracticeQuestion[] = [
  {
    id: 101,
    type: 'single',
    difficulty: '基础',
    knowledge: '访问控制',
    stem: '在安全网关的访问控制策略中，最常见、也最需要关注的策略匹配原则是？',
    options: [
      { key: 'A', text: '自上而下匹配，首次命中后执行对应动作' },
      { key: 'B', text: '所有策略全部匹配后，选择动作最严格的一条' },
      { key: 'C', text: '优先匹配最后创建的策略' },
      { key: 'D', text: '只根据目的端口判断是否放行' }
    ],
    answer: ['A'],
    explanation: '访问控制策略通常按顺序匹配，首条命中策略决定放行、拒绝或记录等动作。因此宽泛策略应放在下方，精确策略应放在上方。',
    optionNotes: {
      A: '正确。策略顺序会直接影响命中结果。',
      B: '错误。一般不会在全部策略中再做严格度排序。',
      C: '错误。创建时间不是通用匹配依据。',
      D: '错误。访问控制通常同时看源、目的、服务、应用、用户、时间等条件。'
    },
    askGuide: '重点看“策略顺序”和“首次命中”，排障时先确认流量命中的具体策略。'
  },
  {
    id: 102,
    type: 'multiple',
    difficulty: '进阶',
    knowledge: '边界防护',
    stem: '企业出口边界防护通常需要同时关注哪些能力？',
    options: [
      { key: 'A', text: '访问控制和最小权限放行' },
      { key: 'B', text: 'NAT 地址转换与公网暴露面收敛' },
      { key: 'C', text: '日志审计与告警追踪' },
      { key: 'D', text: '关闭所有安全日志以提升性能' }
    ],
    answer: ['A', 'B', 'C'],
    explanation: '边界防护不是单一开关，而是访问控制、地址转换、暴露面治理、威胁检测、日志审计等能力的组合。',
    optionNotes: {
      A: '正确。只开放确有业务需要的流量。',
      B: '正确。NAT 与端口映射会影响暴露面，需要纳入治理。',
      C: '正确。日志是溯源、复盘和合规的重要依据。',
      D: '错误。关闭日志会削弱检测和追溯能力。'
    },
    askGuide: '把出口边界理解为“能不能过、从哪里过、过了有没有记录”。'
  },
  {
    id: 103,
    type: 'judge',
    difficulty: '基础',
    knowledge: 'NAT',
    stem: '判断：NAT 本身等同于访问控制，只要配置了 NAT，就不再需要安全策略。',
    options: [
      { key: 'A', text: '正确' },
      { key: 'B', text: '错误' }
    ],
    answer: ['B'],
    explanation: 'NAT 解决地址转换问题，不等同于授权访问。实际部署中仍要通过安全策略限制来源、目的、服务和应用。',
    optionNotes: {
      A: '错误。NAT 不是完整的访问授权机制。',
      B: '正确。NAT 与安全策略需要配合使用。'
    },
    askGuide: '区分两个关键词：NAT 负责“地址怎么变”，安全策略负责“流量能不能过”。'
  },
  {
    id: 104,
    type: 'single',
    difficulty: '进阶',
    knowledge: 'IPsec VPN',
    stem: '站点到站点 IPsec VPN 主要用于解决哪类安全需求？',
    options: [
      { key: 'A', text: '在不可信网络上为两端内网通信提供加密隧道' },
      { key: 'B', text: '替代所有终端杀毒软件' },
      { key: 'C', text: '自动修复服务器漏洞' },
      { key: 'D', text: '隐藏所有业务日志' }
    ],
    answer: ['A'],
    explanation: 'IPsec VPN 的核心价值是通过身份认证、密钥协商和加密封装，让跨互联网的站点间通信具备机密性与完整性保护。',
    optionNotes: {
      A: '正确。站点互联和加密传输是典型场景。',
      B: '错误。VPN 不是终端防病毒能力。',
      C: '错误。漏洞修复属于资产和补丁管理。',
      D: '错误。安全建设通常要求保留必要日志。'
    },
    askGuide: '看到 IPsec VPN，优先联想到“站点互联、身份认证、加密、完整性”。'
  },
  {
    id: 105,
    type: 'multiple',
    difficulty: '易错',
    knowledge: '身份认证',
    stem: '以下哪些措施可以增强账号身份认证安全？',
    options: [
      { key: 'A', text: '启用多因素认证' },
      { key: 'B', text: '设置复杂度和过期策略，并禁用默认弱口令' },
      { key: 'C', text: '按岗位最小化授权' },
      { key: 'D', text: '多人共用一个管理员账号，方便交接' }
    ],
    answer: ['A', 'B', 'C'],
    explanation: '账号安全应覆盖认证强度、口令治理、权限最小化和操作可追责。共用账号会破坏审计闭环。',
    optionNotes: {
      A: '正确。多因素认证可以降低口令泄露风险。',
      B: '正确。默认弱口令是常见突破口。',
      C: '正确。认证和授权需要配套治理。',
      D: '错误。共用账号会导致责任不清和审计失效。'
    },
    askGuide: '身份认证题常考“三件事”：强认证、最小权限、可审计。'
  },
  {
    id: 106,
    type: 'judge',
    difficulty: '基础',
    knowledge: '入侵防御',
    stem: '判断：IPS 开启后一定不会产生误报，因此可以无条件阻断所有命中流量。',
    options: [
      { key: 'A', text: '正确' },
      { key: 'B', text: '错误' }
    ],
    answer: ['B'],
    explanation: 'IPS 可能基于特征、协议异常或行为检测发现攻击，但任何检测机制都可能存在误报。上线阻断前应结合业务验证、日志观察和策略调优。',
    optionNotes: {
      A: '错误。安全检测需要考虑误报、漏报和业务影响。',
      B: '正确。阻断策略应分阶段验证。'
    },
    askGuide: 'IPS 相关判断题里，“一定、完全、无条件”通常需要警惕。'
  },
  {
    id: 107,
    type: 'single',
    difficulty: '易错',
    knowledge: 'Web 防护',
    stem: '针对 SQL 注入风险，以下哪项防护思路最合理？',
    options: [
      { key: 'A', text: '仅在前端页面限制输入长度即可' },
      { key: 'B', text: '应用侧参数化查询，并结合 WAF、日志监测和漏洞修复' },
      { key: 'C', text: '把数据库端口改成非默认端口后无需其他措施' },
      { key: 'D', text: '关闭所有错误提示就可以彻底消除注入风险' }
    ],
    answer: ['B'],
    explanation: 'SQL 注入治理应以应用代码安全为核心，参数化查询是关键措施，边界 WAF、日志监测和补丁修复用于降低风险和提升发现能力。',
    optionNotes: {
      A: '错误。前端校验可以绕过，不能作为唯一防线。',
      B: '正确。代码治理与安全设备联动更完整。',
      C: '错误。改端口只能降低暴露特征，不能修复注入。',
      D: '错误。关闭报错不能消除根因。'
    },
    askGuide: 'Web 安全题优先找“修根因”的选项，再看边界检测和审计。'
  },
  {
    id: 108,
    type: 'multiple',
    difficulty: '基础',
    knowledge: '日志审计',
    stem: '安全日志审计中，哪些信息通常有助于事件追踪？',
    options: [
      { key: 'A', text: '源地址、目的地址、端口和协议' },
      { key: 'B', text: '命中的安全策略或规则名称' },
      { key: 'C', text: '事件时间、动作结果和严重级别' },
      { key: 'D', text: '只记录“有攻击”四个字即可' }
    ],
    answer: ['A', 'B', 'C'],
    explanation: '可追踪的日志需要包含时间、主体、对象、动作、结果和规则上下文。信息过少会导致无法定位范围和责任。',
    optionNotes: {
      A: '正确。五元组是网络侧分析基础。',
      B: '正确。命中规则能帮助定位策略配置。',
      C: '正确。时间线和严重级别用于研判处置。',
      D: '错误。缺少上下文无法支撑溯源。'
    },
    askGuide: '审计题按“谁、对谁、做了什么、结果如何、何时发生”拆解。'
  },
  {
    id: 109,
    type: 'judge',
    difficulty: '进阶',
    knowledge: '零信任',
    stem: '判断：零信任的核心思想是不默认信任任何主体，访问应持续验证并按最小权限授权。',
    options: [
      { key: 'A', text: '正确' },
      { key: 'B', text: '错误' }
    ],
    answer: ['A'],
    explanation: '零信任强调以身份为中心、持续评估、动态授权和最小权限，不因为用户位于内网就默认可信。',
    optionNotes: {
      A: '正确。这是零信任的核心表述。',
      B: '错误。把内网天然可信视为默认前提，与零信任思想相反。'
    },
    askGuide: '零信任题常围绕“持续验证、最小权限、动态访问控制”。'
  },
  {
    id: 110,
    type: 'single',
    difficulty: '基础',
    knowledge: '风险处置',
    stem: '发现互联网暴露的高危漏洞资产后，较合理的处置优先级是？',
    options: [
      { key: 'A', text: '先评估暴露面和影响范围，再采取临时缓解、修复和复测' },
      { key: 'B', text: '只记录到文档，不做任何处理' },
      { key: 'C', text: '直接删除所有业务系统' },
      { key: 'D', text: '等待攻击发生后再分析' }
    ],
    answer: ['A'],
    explanation: '漏洞处置要兼顾业务连续性和风险控制：确认范围、临时缓解、补丁修复、验证复测和复盘改进。',
    optionNotes: {
      A: '正确。符合风险处置闭环。',
      B: '错误。记录不等于治理。',
      C: '错误。处置应可控，避免扩大业务影响。',
      D: '错误。高危暴露资产需要主动治理。'
    },
    askGuide: '风险处置题按“发现、评估、缓解、修复、验证、复盘”排序。'
  },
  {
    id: 111,
    type: 'multiple',
    difficulty: '进阶',
    knowledge: '安全运营',
    stem: '一次告警研判中，哪些动作有助于确认是否为真实攻击？',
    options: [
      { key: 'A', text: '关联同一源地址、同一资产的多类日志' },
      { key: 'B', text: '检查命中规则、载荷特征和业务上下文' },
      { key: 'C', text: '确认受影响资产是否存在对应漏洞或暴露服务' },
      { key: 'D', text: '只看告警标题，不看原始日志' }
    ],
    answer: ['A', 'B', 'C'],
    explanation: '告警研判需要从多源日志、规则上下文、攻击载荷、资产状态和业务行为综合判断，不能只看标题。',
    optionNotes: {
      A: '正确。关联分析可以减少孤立告警误判。',
      B: '正确。规则和载荷能说明告警依据。',
      C: '正确。资产暴露和漏洞状态影响可信度。',
      D: '错误。只看标题容易误判。'
    },
    askGuide: '运营题强调“关联、验证、上下文”，不要只凭单点告警下结论。'
  },
  {
    id: 112,
    type: 'judge',
    difficulty: '易错',
    knowledge: 'WAF',
    stem: '判断：WAF 只能串联部署，不能通过反向代理、旁路镜像或云端接入等方式参与 Web 防护。',
    options: [
      { key: 'A', text: '正确' },
      { key: 'B', text: '错误' }
    ],
    answer: ['B'],
    explanation: 'WAF 的部署形态可以有多种，常见包括反向代理、透明串联、旁路检测、云 WAF 等。具体方式取决于业务架构和防护目标。',
    optionNotes: {
      A: '错误。WAF 不只有一种部署形态。',
      B: '正确。实际部署需要结合网络和业务架构选择。'
    },
    askGuide: '看到“只能、必须、唯一”这类绝对化描述，要结合实际部署形态判断。'
  }
]

const questionBank: PracticeQuestion[] = buildQuestionBankFromMarkdown(scsaOutline, legacyQuestionBank)

interface MarkdownQuestionDraft {
  id: number
  type: QuestionType
  knowledge: string
  stem: string
  options: PracticeOption[]
  answer: string[]
  explanationLines: string[]
}

interface OutlineModule {
  title: string
  points: string[]
  asks: string[]
  pitfalls: string[]
  judgeStatements: string[]
}

interface OptionDraft {
  text: string
  correct: boolean
}

function buildQuestionBankFromMarkdown(markdown: string, fallback: PracticeQuestion[]) {
  const parsed = parseQuestionMarkdown(markdown)
  if (parsed.length) return parsed
  return buildQuestionBankFromOutline(markdown, fallback)
}

function parseQuestionMarkdown(markdown: string): PracticeQuestion[] {
  const questions: PracticeQuestion[] = []
  let sectionType: QuestionType = 'single'
  let current: MarkdownQuestionDraft | null = null
  let readingExplanation = false

  const pushCurrent = () => {
    if (!current || !current.options.length || !current.answer.length) return

    const optionKeys = new Set(current.options.map((option) => option.key))
    const answer = current.answer.filter((key) => optionKeys.has(key))
    if (!answer.length) return

    const type: QuestionType = current.type === 'multiple' || answer.length > 1 ? 'multiple' : 'single'
    const explanation = current.explanationLines.join(' ').trim() || '本题来自 SCSA-S 理论复习题库。'

    questions.push({
      id: current.id,
      type,
      difficulty: inferQuestionDifficulty(current.knowledge, type, current.id),
      knowledge: current.knowledge,
      stem: current.stem,
      options: current.options,
      answer,
      explanation,
      optionNotes: buildOptionNotes(current.options, answer, explanation),
      askGuide: `本题来自「${current.knowledge}」模块。先判断题干限定条件，再对照标准答案理解每个选项的边界。`
    })
  }

  for (const rawLine of markdown.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line) continue

    const sectionMatch = line.match(/^##\s*(单选题|多选题)/)
    if (sectionMatch) {
      sectionType = sectionMatch[1] === '多选题' ? 'multiple' : 'single'
      continue
    }

    const questionMatch = line.match(/^###\s+(\d+)\.\s+(?:【([^】]+)】)?(.+)$/)
    if (questionMatch) {
      pushCurrent()
      current = {
        id: Number(questionMatch[1]),
        type: sectionType,
        knowledge: questionMatch[2]?.trim() || '综合练习',
        stem: questionMatch[3].trim(),
        options: [],
        answer: [],
        explanationLines: []
      }
      readingExplanation = false
      continue
    }

    if (!current) continue

    const optionMatch = line.match(/^([A-Z])\.\s*(.+)$/)
    if (optionMatch) {
      current.options.push({ key: optionMatch[1], text: optionMatch[2].trim() })
      readingExplanation = false
      continue
    }

    const answerMatch = line.match(/^\*\*答案[:：]\s*([A-Z、，,\s]+)\*\*$/)
    if (answerMatch) {
      current.answer = parseAnswerKeys(answerMatch[1])
      readingExplanation = false
      continue
    }

    const explanationMatch = line.match(/^\*\*解析[:：]\*\*\s*(.*)$/)
    if (explanationMatch) {
      readingExplanation = true
      if (explanationMatch[1]) current.explanationLines.push(explanationMatch[1].trim())
      continue
    }

    if (readingExplanation) {
      current.explanationLines.push(line)
    }
  }

  pushCurrent()
  return questions.sort((a, b) => a.id - b.id)
}

function parseAnswerKeys(raw: string) {
  return Array.from(new Set(raw.replace(/[^A-Z]/g, '').split('').filter(Boolean)))
}

function inferQuestionDifficulty(knowledge: string, type: QuestionType, id: number): Difficulty {
  if (/综合|应急|运营|渗透|漏洞|上传|命令|越权|场景/.test(knowledge)) {
    return type === 'multiple' ? '易错' : '进阶'
  }
  if (type === 'multiple') return '进阶'
  return pickDifficulty(id)
}

function buildQuestionBankFromOutline(markdown: string, fallback: PracticeQuestion[]) {
  const modules = parseOutlineModules(markdown)
  const generated: PracticeQuestion[] = []
  let id = 1000

  modules.forEach((module, moduleIndex) => {
    module.asks.forEach((ask, askIndex) => {
      generated.push(createAskQuestion(id++, module, ask, askIndex))
    })

    chunk(module.points, 3).forEach((points, pointIndex) => {
      if (points.length >= 2) {
        generated.push(createMultipleQuestion(id++, module, points, pointIndex))
      } else if (points[0]) {
        generated.push(createPointQuestion(id++, module, points[0], pointIndex))
      }
    })

    module.pitfalls.slice(0, 3).forEach((pitfall, pitfallIndex) => {
      generated.push(createJudgeQuestion(id++, module, pitfall, true, moduleIndex + pitfallIndex))
    })

    module.judgeStatements.forEach((statement, statementIndex) => {
      generated.push(createJudgeQuestion(id++, module, statement, inferJudgeAnswer(statement), moduleIndex + statementIndex))
    })
  })

  return generated.length ? generated : fallback
}

function parseOutlineModules(markdown: string) {
  const modules: OutlineModule[] = []
  let current: OutlineModule | null = null
  let subsection = ''

  for (const line of markdown.split(/\r?\n/)) {
    const moduleMatch = line.match(/^##\s+\d+\.\s+(.+)$/)
    if (moduleMatch) {
      current = {
        title: moduleMatch[1].trim(),
        points: [],
        asks: [],
        pitfalls: [],
        judgeStatements: []
      }
      modules.push(current)
      subsection = ''
      continue
    }

    const subsectionMatch = line.match(/^###\s+(.+)$/)
    if (subsectionMatch) {
      subsection = subsectionMatch[1].trim()
      continue
    }

    const bulletMatch = line.match(/^- (.+)$/)
    if (!current || !bulletMatch) continue

    const text = normalizeOutlineText(bulletMatch[1])
    if (!text) continue

    if (subsection.includes('可能考察点')) current.points.push(text)
    if (subsection.includes('常见选择题问法')) current.asks.push(text)
    if (subsection.includes('易错点')) current.pitfalls.push(text)
    if (subsection.includes('判断题适合考')) current.judgeStatements.push(text)
  }

  return modules.filter((item) =>
    item.points.length || item.asks.length || item.pitfalls.length || item.judgeStatements.length
  )
}

function normalizeOutlineText(text: string) {
  return text
    .trim()
    .replace(/^["“]/, '')
    .replace(/["”。]$/, '')
    .replace(/`/g, '')
}

function createAskQuestion(id: number, module: OutlineModule, ask: string, index: number): PracticeQuestion {
  const known = createKnownAskOptions(ask)
  const type = known.type
  const options = toPracticeOptions(known.options)
  return {
    id,
    type,
    difficulty: pickDifficulty(index),
    knowledge: module.title,
    stem: ask,
    options,
    answer: known.answer,
    explanation: known.explanation,
    optionNotes: buildOptionNotes(options, known.answer, known.explanation),
    askGuide: `本题来自《${module.title}》章节的常见选择题问法，优先按大纲中的核心概念和易错点排除干扰项。`
  }
}

function createKnownAskOptions(ask: string): { type: QuestionType; options: string[]; answer: string[]; explanation: string } {
  if (ask.includes('面向连接')) {
    return knownSingle(['TCP', 'UDP', 'IP', 'ICMP'], 'A', 'TCP 是面向连接的传输层协议，通信前需要建立连接；UDP 不建立连接。')
  }
  if (ask.includes('三次握手第二次')) {
    return knownSingle(['SYN', 'SYN + ACK', 'FIN + ACK', 'RST'], 'B', '三次握手第二次由服务端返回 SYN + ACK，表示确认客户端请求并发起自己的同步请求。')
  }
  if (ask.includes('403 与 401')) {
    return knownSingle(['401 表示未认证或认证失败，403 表示服务器拒绝访问', '401 表示资源不存在，403 表示请求成功', '401 只用于服务端错误，403 只用于重定向', '二者含义完全相同'], 'A', '401 侧重身份认证，403 侧重已理解请求但拒绝访问。')
  }
  if (ask.includes('MySQL 服务')) {
    return knownSingle(['3306', '6379', '3389', '21'], 'A', 'MySQL 默认端口通常是 3306。')
  }
  if (ask.includes('SQL 注入最有效')) {
    return knownSingle(['参数化查询或预编译', '只过滤单引号', '只隐藏错误信息', '把数据库端口改为非默认端口'], 'A', 'SQL 注入治理的核心是让 SQL 结构与用户输入分离，参数化查询或预编译是关键措施。')
  }
  if (ask.includes('存储型 XSS')) {
    return knownSingle(['攻击脚本被保存到服务端，用户访问相关页面时触发', '攻击只出现在当前 URL 参数中，不落库', '攻击只发生在浏览器本地 DOM 中，与服务端无关', '攻击只能通过邮件附件触发'], 'A', '存储型 XSS 的主要特征是恶意内容被服务端持久化，影响访问该内容的其他用户。')
  }
  if (ask.includes('只校验文件后缀')) {
    return knownSingle(['后缀、MIME 和内容都可能被绕过，还要限制解析执行和存储位置', '后缀由服务器强制生成，无法伪造', '只要后缀正确，文件内容一定安全', '上传目录允许脚本执行可以提升安全性'], 'A', '上传防护要结合扩展名、MIME、文件内容、随机文件名、目录权限和禁止脚本执行等措施。')
  }
  if (ask.includes('Cookie 属性')) {
    return knownSingle(['HttpOnly', 'Expires', 'Path', 'Domain'], 'A', 'HttpOnly 可以阻止客户端脚本读取 Cookie，从而降低 XSS 窃取 Cookie 的风险。')
  }
  if (ask.includes('水平越权')) {
    return knownSingle(['水平越权是同权限用户间越界访问，垂直越权是低权限获得高权限能力', '水平越权只发生在网络层，垂直越权只发生在数据库层', '水平越权一定比垂直越权危害更高', '二者没有本质区别'], 'A', '水平越权关注同级对象越界，垂直越权关注权限等级提升。')
  }
  if (ask.includes('本地账号信息')) {
    return knownSingle(['/etc/passwd', '/etc/shadow', '/var/log/secure', '/etc/hosts'], 'A', '/etc/passwd 通常保存本地账号基础信息，密码哈希一般在 /etc/shadow。')
  }
  if (ask.includes('SSH 暴力破解')) {
    return knownSingle(['/var/log/secure 或 /var/log/auth.log', '/var/log/nginx/access.log', '/etc/passwd', '/tmp'], 'A', 'SSH 登录成功、失败和认证相关记录通常在 /var/log/secure 或 /var/log/auth.log 中查看。')
  }
  if (ask.includes('上传目录最小权限')) {
    return knownSingle(['允许必要写入，禁止脚本执行，并限制访问范围', '允许所有用户读写执行', '只要目录可访问就应允许执行脚本', '给 Web 目录全部设置 777'], 'A', '上传目录应满足业务写入需求，但不能让上传文件获得脚本执行能力。')
  }
  if (ask.includes('Nginx') && ask.includes('版本号')) {
    return knownSingle(['server_tokens off', 'autoindex on', 'root /', 'proxy_pass off'], 'A', 'Nginx 隐藏版本号常用配置是 server_tokens off。')
  }
  if (ask.includes('远程桌面默认端口')) {
    return knownSingle(['3389', '3306', '6379', '8080'], 'A', 'Windows 远程桌面 RDP 默认端口通常是 3389。')
  }
  if (ask.includes('Windows 登录失败')) {
    return knownSingle(['安全日志', '应用日志', 'DNS 缓存', '浏览器历史记录'], 'A', 'Windows 登录成功/失败事件应优先查看安全日志。')
  }
  if (ask.includes('共享权限和 NTFS')) {
    return knownSingle(['综合两类权限后取更受限制的有效权限', '只看共享权限，忽略 NTFS 权限', '只看 NTFS 权限，忽略共享权限', '权限会自动全部放大为管理员权限'], 'A', '共享权限与 NTFS 权限同时存在时，实际访问效果通常受两者共同限制。')
  }
  if (ask.includes('WebShell 后第一步')) {
    return knownSingle(['保护现场并确认影响范围，必要时隔离主机后取证', '立即删除文件并结束处置', '公开攻击细节', '关闭所有日志'], 'A', '发现 WebShell 后应先保护证据、确认入口和影响范围，再根除与恢复。')
  }
  if (ask.includes('Web 请求参数')) {
    return knownSingle(['Web 访问日志', 'BIOS 日志', '显卡驱动日志', '打印机队列'], 'A', 'Web 请求路径、参数、来源 IP、状态码等通常最可能出现在 Web 访问日志中。')
  }
  if (ask.includes('不建议直接删除可疑文件')) {
    return knownSingle(['会破坏证据，且无法确认入口、影响范围和持久化方式', '删除文件会自动修复所有漏洞', '删除文件会增加磁盘空间', '可疑文件一定不是攻击证据'], 'A', '应急响应需要先取证和定位根因，否则容易重复入侵。')
  }
  if (ask.includes('攻击时间线')) {
    return knownMultiple(['Web 访问日志和错误日志', '文件修改时间与可疑上传记录', '进程、网络连接和登录记录', '只记录最终结论，不保留过程证据'], ['A', 'B', 'C'], '攻击时间线通常由多源日志、文件时间、进程网络行为和登录记录共同还原。')
  }
  if (ask.includes('Redis 未授权')) {
    return knownSingle(['数据泄露、写入持久化文件或进一步控制主机的风险', '只能导致页面样式异常', '只能影响 DNS 解析', '不会带来安全影响'], 'A', 'Redis 未授权访问可能造成数据泄露、写文件、写 SSH key 或其他高风险后果。')
  }
  if (ask.includes('MySQL 不应直接暴露')) {
    return knownSingle(['会扩大暴力破解、弱口令利用、漏洞攻击和数据泄露风险', '公网访问一定能提升数据库性能', '暴露公网可以自动加密数据', '只要端口开放就无需账号权限'], 'A', '数据库应限制来源、最小授权并避免不必要公网暴露。')
  }
  if (ask.includes('Tomcat 管理后台弱口令')) {
    return knownSingle(['可能导致后台登录、WAR 上传和服务器控制风险', '只能修改页面字体', '只影响客户端缓存', '会自动修复应用漏洞'], 'A', 'Tomcat 管理后台弱口令可能被用于部署恶意 WAR 包并获取服务器权限。')
  }
  if (ask.includes('SQL 注入修复后')) {
    return knownSingle(['使用原验证路径复测不可再利用，并确认业务功能正常', '只删除测试记录，不再验证', '只修改页面提示文案', '关闭数据库后直接上线'], 'A', '复测应基于原漏洞验证路径，确认漏洞不可利用且业务未受影响。')
  }
  if (ask.includes('上传漏洞修复')) {
    return knownMultiple(['后缀、MIME 和文件内容校验', '上传目录禁止脚本执行', '随机文件名、路径隔离和权限控制', '只依赖客户端 JavaScript 校验'], ['A', 'B', 'C'], '上传漏洞复测需要同时验证校验链、存储位置、解析策略和权限控制。')
  }
  if (ask.includes('整改报告')) {
    return knownMultiple(['漏洞描述、影响范围和风险等级', '修复建议、责任人和整改时间', '复测结果与闭环记录', '只写一句“已修复”即可'], ['A', 'B', 'C'], '整改报告要体现发现、确认、整改、复测和记录的闭环。')
  }
  if (ask.includes('渗透测试前')) {
    return knownMultiple(['授权范围', '测试时间和目标', '禁止动作与应急联系人', '第三方未授权资产列表'], ['A', 'B', 'C'], '渗透测试前必须确认授权边界、范围、时间、目标、禁止动作和联系人。')
  }
  if (ask.includes('超出授权范围')) {
    return knownSingle(['停止对该资产测试并及时沟通确认授权边界', '继续深入测试直到拿到数据', '公开目标信息', '把资产加入扫描字典继续跑'], 'A', '发现超范围资产时应停止测试并向项目联系人确认，不能擅自扩展范围。')
  }
  if (ask.includes('敏感数据')) {
    return knownSingle(['最小化展示并脱敏，保留必要证据链', '完整公开账号、密码和身份证号', '删除所有证据', '把敏感数据放到报告标题'], 'A', '报告涉及敏感数据时应遵循最小必要和脱敏原则。')
  }

  return knownSingle(
    [`围绕「${ask}」回到大纲章节定位概念、场景和易错点`, '只背选项字母，不理解概念', '不看授权边界直接动手验证', '把单一工具输出当作最终结论'],
    'A',
    `该题来自 Markdown 大纲的常见问法。当前临时题库按「${ask}」生成练习项，答题时应回到对应章节的考察点和易错点。`
  )
}

function knownSingle(options: string[], answer: string, explanation: string) {
  return { type: 'single' as QuestionType, options, answer: [answer], explanation }
}

function knownMultiple(options: string[], answer: string[], explanation: string) {
  return { type: 'multiple' as QuestionType, options, answer, explanation }
}

function createMultipleQuestion(id: number, module: OutlineModule, points: string[], index: number): PracticeQuestion {
  const drafts: OptionDraft[] = [
    ...points.map((point) => ({ text: point, correct: true })),
    { text: createDistractor(module.title, index), correct: false }
  ]
  const options = arrangeOptions(drafts, id)
  const answer = options.filter((option) => drafts.find((draft) => draft.text === option.text)?.correct).map((option) => option.key)
  const explanation = `这些选项来自 Markdown 中「${module.title}」的可能考察点。错误选项通常表现为绝对化、跳过授权边界或用单一动作替代完整闭环。`

  return {
    id,
    type: 'multiple',
    difficulty: pickDifficulty(index + 1),
    knowledge: module.title,
    stem: `以下哪些内容属于「${module.title}」章节的重点考察范围？`,
    options,
    answer,
    explanation,
    optionNotes: buildOptionNotes(options, answer, explanation),
    askGuide: `多选题优先找大纲中明确列出的考察点，同时排除“只要、一定、无需记录”等绝对化表述。`
  }
}

function createPointQuestion(id: number, module: OutlineModule, point: string, index: number): PracticeQuestion {
  const options = toPracticeOptions([point, '只需要记住工具名称，不需要理解适用场景', '所有风险都可以通过关闭日志解决', '未经授权也可以直接扫描验证'])
  const explanation = `该考点来自 Markdown 中「${module.title}」的可能考察点：${point}`
  return {
    id,
    type: 'single',
    difficulty: pickDifficulty(index),
    knowledge: module.title,
    stem: `以下哪项最符合「${module.title}」章节的备考要求？`,
    options,
    answer: ['A'],
    explanation,
    optionNotes: buildOptionNotes(options, ['A'], explanation),
    askGuide: `先确认题干是否在问概念、流程、工具用途还是安全边界，再排除过度简化的选项。`
  }
}

function createJudgeQuestion(id: number, module: OutlineModule, statement: string, correct: boolean, index: number): PracticeQuestion {
  const answer = correct ? ['A'] : ['B']
  const options = toPracticeOptions(['正确', '错误'])
  const explanation = correct
    ? `该说法符合 Markdown 中「${module.title}」章节的复习口径：${statement}`
    : `该说法属于需要识别的误区。应结合「${module.title}」章节的考察点，不要用单一动作替代完整防护或处置闭环。`

  return {
    id,
    type: 'judge',
    difficulty: correct ? '基础' : '易错',
    knowledge: module.title,
    stem: `判断：${statement}`,
    options,
    answer,
    explanation,
    optionNotes: buildOptionNotes(options, answer, explanation),
    askGuide: `判断题重点识别“只要、一定、彻底、无需”等绝对化表达，并回到大纲中的安全目标判断。`
  }
}

function inferJudgeAnswer(statement: string) {
  if (/只要|即可彻底|只要删除|一定比|可以替代/.test(statement)) return false
  return true
}

function toPracticeOptions(options: string[]): PracticeOption[] {
  return options.map((text, index) => ({ key: String.fromCharCode(65 + index), text }))
}

function arrangeOptions(drafts: OptionDraft[], seed: number): PracticeOption[] {
  const shift = seed % drafts.length
  return [...drafts.slice(shift), ...drafts.slice(0, shift)].map((item, index) => ({
    key: String.fromCharCode(65 + index),
    text: item.text
  }))
}

function buildOptionNotes(options: PracticeOption[], answer: string[], explanation: string) {
  return options.reduce<Record<string, string>>((notes, option) => {
    notes[option.key] = answer.includes(option.key)
      ? `正确。${option.text}`
      : `错误。该项不符合当前题目要求。${explanation}`
    return notes
  }, {})
}

function createDistractor(moduleTitle: string, index: number) {
  const pool = [
    `只要记住「${moduleTitle}」相关工具名称，就不需要验证风险场景`,
    `所有安全问题都可以通过关闭日志或隐藏报错一次性解决`,
    `未经授权也可以扩大测试范围，以便获得更完整结论`,
    `扫描器输出可以直接作为最终漏洞结论，无需人工确认`
  ]
  return pool[index % pool.length]
}

function pickDifficulty(index: number): Difficulty {
  const list: Difficulty[] = ['基础', '进阶', '易错']
  return list[index % list.length]
}

function chunk<T>(items: T[], size: number) {
  const result: T[][] = []
  for (let index = 0; index < items.length; index += size) {
    result.push(items.slice(index, index + size))
  }
  return result
}

const typeLabelMap: Record<QuestionType, string> = {
  single: '单选题',
  multiple: '多选题',
  judge: '判断题'
}

const typeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '单选', value: 'single' },
  { label: '多选', value: 'multiple' },
  { label: '判断', value: 'judge' }
]

const difficultyOptions: Difficulty[] = ['基础', '进阶', '易错']

const filters = reactive({
  keyword: '',
  type: 'ALL',
  knowledge: 'ALL',
  difficulty: 'ALL',
  onlyWrong: false
})

const answerBook = ref<Record<number, AnswerState>>(loadAnswerBook())
const activeQuestionBank = computed(() => questionBank.filter((item) => !isAnsweredCorrect(item.id)))
const currentQuestionId = ref(activeQuestionBank.value[0]?.id ?? questionBank[0]?.id ?? 0)
const askInput = ref('')
const chatMessages = ref<ChatMessage[]>([])
const messageSeq = ref(1)

const knowledgeOptions = computed(() => Array.from(new Set(activeQuestionBank.value.map((item) => item.knowledge))))

const filteredQuestions = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return activeQuestionBank.value.filter((item) => {
    const state = getAnswerState(item.id)
    const matchKeyword = !keyword || `${item.stem} ${item.knowledge}`.toLowerCase().includes(keyword)
    const matchType = filters.type === 'ALL' || item.type === filters.type
    const matchKnowledge = filters.knowledge === 'ALL' || item.knowledge === filters.knowledge
    const matchDifficulty = filters.difficulty === 'ALL' || item.difficulty === filters.difficulty
    const matchWrong = !filters.onlyWrong || (state.submitted && state.correct === false)
    return matchKeyword && matchType && matchKnowledge && matchDifficulty && matchWrong
  })
})

const currentQuestion = computed(() => activeQuestionBank.value.find((item) => item.id === currentQuestionId.value))
const currentState = computed(() => currentQuestion.value ? getAnswerState(currentQuestion.value.id) : createEmptyState())
const currentQuestionNumber = computed(() => {
  const index = filteredQuestions.value.findIndex((item) => item.id === currentQuestionId.value)
  return index >= 0 ? index + 1 : 1
})
const canSubmitCurrent = computed(() => {
  return !!currentQuestion.value && currentState.value.selected.length > 0 && !currentState.value.submitted
})

const progressStats = computed(() => {
  const answered = questionBank.filter((item) => getAnswerState(item.id).submitted)
  const correct = answered.filter((item) => getAnswerState(item.id).correct).length
  const wrong = answered.length - correct
  return {
    answered: answered.length,
    correct,
    wrong,
    accuracy: answered.length ? Math.round((correct / answered.length) * 100) : 0,
    finishedRate: Math.round((correct / questionBank.length) * 100)
  }
})

const quickAskItems = ['解释本题考点', '逐项分析选项', '为什么我的答案不对', '给我记忆口诀']

watch(answerBook, () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(answerBook.value))
}, { deep: true })

watch(filteredQuestions, (list) => {
  if (!list.some((item) => item.id === currentQuestionId.value) && list.length > 0) {
    currentQuestionId.value = list[0].id
  }
})

watch(currentQuestionId, () => {
  seedTutorMessage()
})

onMounted(() => {
  seedTutorMessage()
})

function createEmptyState(): AnswerState {
  return { selected: [], submitted: false, starred: false }
}

function loadAnswerBook(): Record<number, AnswerState> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function getAnswerState(id: number): AnswerState {
  return answerBook.value[id] ?? createEmptyState()
}

function isAnsweredCorrect(id: number) {
  const state = getAnswerState(id)
  return state.submitted && state.correct === true
}

function setAnswerState(id: number, patch: Partial<AnswerState>) {
  const previous = getAnswerState(id)
  answerBook.value = {
    ...answerBook.value,
    [id]: {
      ...previous,
      ...patch,
      selected: patch.selected ?? previous.selected
    }
  }
}

function isSameAnswer(selected: string[], answer: string[]) {
  return [...selected].sort().join('|') === [...answer].sort().join('|')
}

function selectOption(key: string) {
  const question = currentQuestion.value
  if (!question || currentState.value.submitted) return

  if (question.type === 'multiple') {
    const selected = currentState.value.selected.includes(key)
      ? currentState.value.selected.filter((item) => item !== key)
      : [...currentState.value.selected, key]
    setAnswerState(question.id, { selected })
    return
  }

  setAnswerState(question.id, { selected: [key] })
}

function submitCurrentAnswer() {
  const question = currentQuestion.value
  if (!question || !canSubmitCurrent.value) return

  const correct = isSameAnswer(currentState.value.selected, question.answer)
  const nextQuestionId = correct ? getNextActiveQuestionId(question.id) : undefined
  setAnswerState(question.id, {
    submitted: true,
    correct,
    lastAt: new Date().toISOString()
  })
  ElMessage[correct ? 'success' : 'warning'](correct ? '回答正确' : '已显示解析，请复盘本题')
  if (correct && nextQuestionId !== undefined) {
    currentQuestionId.value = nextQuestionId
  }
}

function resetCurrent() {
  const question = currentQuestion.value
  if (!question) return
  const starred = currentState.value.starred
  setAnswerState(question.id, {
    selected: [],
    submitted: false,
    correct: undefined,
    lastAt: undefined,
    starred
  })
}

async function resetAll() {
  try {
    await ElMessageBox.confirm('确定清空本页面的本地练习记录吗？', '清空进度', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    answerBook.value = {}
    currentQuestionId.value = activeQuestionBank.value[0]?.id ?? questionBank[0]?.id ?? 0
    seedTutorMessage()
    ElMessage.success('练习进度已清空')
  } catch {
    // 用户取消时不需要提示。
  }
}

function toggleStar(id: number) {
  const state = getAnswerState(id)
  setAnswerState(id, { starred: !state.starred })
}

function goToQuestion(id: number) {
  currentQuestionId.value = id
}

function previousQuestion() {
  const list = filteredQuestions.value
  if (!list.length) return
  const index = list.findIndex((item) => item.id === currentQuestionId.value)
  const targetIndex = index <= 0 ? list.length - 1 : index - 1
  currentQuestionId.value = list[targetIndex].id
}

function nextQuestion() {
  const list = filteredQuestions.value
  if (!list.length) return
  const index = list.findIndex((item) => item.id === currentQuestionId.value)
  const targetIndex = index < 0 || index === list.length - 1 ? 0 : index + 1
  currentQuestionId.value = list[targetIndex].id
}

function goNextUnanswered() {
  const list = filteredQuestions.value.length ? filteredQuestions.value : activeQuestionBank.value
  if (!list.length) return
  const currentIndex = list.findIndex((item) => item.id === currentQuestionId.value)
  const rotated = [...list.slice(currentIndex + 1), ...list.slice(0, currentIndex + 1)]
  const target = rotated.find((item) => !getAnswerState(item.id).submitted) ?? rotated[0]
  currentQuestionId.value = target.id
}

function getNextActiveQuestionId(currentId: number) {
  const list = filteredQuestions.value.filter((item) => item.id !== currentId)
  const fallbackList = activeQuestionBank.value.filter((item) => item.id !== currentId)
  const candidates = list.length ? list : fallbackList
  if (!candidates.length) return undefined

  const currentIndex = activeQuestionBank.value.findIndex((item) => item.id === currentId)
  const next = candidates.find((item) => {
    const itemIndex = activeQuestionBank.value.findIndex((activeItem) => activeItem.id === item.id)
    return itemIndex > currentIndex
  })
  return (next ?? candidates[0]).id
}

function getOptionClass(question: PracticeQuestion, key: string) {
  const state = getAnswerState(question.id)
  return {
    selected: state.selected.includes(key),
    submitted: state.submitted,
    correct: state.submitted && question.answer.includes(key),
    wrong: state.submitted && state.selected.includes(key) && !question.answer.includes(key)
  }
}

function getQuestionIndexClass(question: PracticeQuestion) {
  const state = getAnswerState(question.id)
  return {
    active: question.id === currentQuestionId.value,
    done: state.submitted && state.correct,
    wrong: state.submitted && state.correct === false,
    pending: !state.submitted && state.selected.length > 0
  }
}

function formatAnswer(answer: string[]) {
  return [...answer].sort().join('、')
}

function seedTutorMessage() {
  const question = currentQuestion.value
  if (!question) return
  messageSeq.value = 1
  chatMessages.value = [
    {
      id: messageSeq.value++,
      role: 'assistant',
      content: `当前题目考点是「${question.knowledge}」。可以先作答再看解析，也可以直接问我选项差异、解题思路或记忆方法。`
    }
  ]
}

function askTutor(prompt?: string) {
  const question = currentQuestion.value
  if (!question) return

  const text = (prompt ?? askInput.value).trim()
  if (!text) return

  chatMessages.value.push({ id: messageSeq.value++, role: 'user', content: text })
  chatMessages.value.push({
    id: messageSeq.value++,
    role: 'assistant',
    content: buildTutorAnswer(question, text)
  })
  askInput.value = ''
}

function buildTutorAnswer(question: PracticeQuestion, text: string) {
  const state = getAnswerState(question.id)
  const selectedText = state.selected.length ? `你当前选择的是 ${formatAnswer(state.selected)}。` : '你当前还没有选择答案。'
  const answerText = `标准答案是 ${formatAnswer(question.answer)}。`
  const optionSummary = question.options
    .map((option) => `${option.key}. ${question.optionNotes[option.key]}`)
    .join('\n')

  if (/逐项|选项|A|B|C|D/.test(text)) {
    return `${answerText}\n${optionSummary}`
  }

  if (/错|不对|为什么|答案/.test(text)) {
    return `${selectedText}${answerText}\n本题关键点：${question.explanation}\n复盘时先判断题目问的是「${question.knowledge}」，再排除绝对化、偷换概念或只解决局部问题的选项。`
  }

  if (/口诀|记忆|记/.test(text)) {
    return `记忆方式：${question.askGuide}\n做题时先圈出题干里的限定词，再把选项分成“解决根因”“辅助措施”“明显绝对化”三类。`
  }

  if (/考点|知识|原理/.test(text)) {
    return `本题考点是「${question.knowledge}」。${question.explanation}\n如果考试中遇到相似题，优先找能覆盖安全目标、业务影响和审计闭环的选项。`
  }

  return `${answerText}\n${question.explanation}\n${selectedText}如果你想继续追问，可以问“为什么不能选某个选项”或“这个考点怎么记”。`
}
</script>

<style scoped>
.scsa-practice-page {
  position: relative;
  z-index: 1;
  height: 100vh;
  overflow: auto;
  color: #e8edf5;
  padding: 22px;
}

.practice-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  margin-bottom: 16px;
}

.title-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.title-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #7ee787;
  background: rgba(46, 160, 67, 0.14);
  border: 1px solid rgba(126, 231, 135, 0.28);
  font-size: 24px;
  flex: 0 0 auto;
}

.title-block h1 {
  margin: 0 0 7px;
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: 0;
}

.title-block p {
  margin: 0;
  color: #a9b6c8;
  font-size: 13px;
}

.title-block .source-line {
  margin-top: 5px;
  color: #7ee787;
  font-size: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(96px, 1fr)) minmax(220px, 1.5fr);
  gap: 10px;
  margin-bottom: 16px;
}

.stat-item,
.progress-bar,
.panel {
  background: rgba(9, 18, 35, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.stat-item,
.progress-bar {
  min-height: 72px;
  padding: 13px 15px;
}

.stat-item span,
.progress-bar span {
  display: block;
  color: #9fb0c3;
  font-size: 12px;
  margin-bottom: 8px;
}

.stat-item strong {
  font-size: 25px;
  line-height: 1;
}

.stat-item.success strong {
  color: #7ee787;
}

.stat-item.danger strong {
  color: #ff7b72;
}

.progress-bar {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.practice-main {
  display: grid;
  grid-template-columns: minmax(250px, 285px) minmax(420px, 1fr) minmax(300px, 360px);
  gap: 16px;
  align-items: start;
  min-height: 0;
}

.panel {
  padding: 16px;
  min-width: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.panel-title h2 {
  font-size: 16px;
  margin: 0;
  line-height: 1.3;
}

.filter-stack {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.switch-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #c8d4e4;
  font-size: 13px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.question-list {
  display: grid;
  gap: 8px;
  max-height: calc(100vh - 356px);
  overflow: auto;
  padding-right: 2px;
}

.question-index {
  width: 100%;
  display: grid;
  grid-template-columns: 38px 1fr 18px;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  color: #cbd8e7;
  text-align: left;
  min-height: 54px;
  padding: 8px 10px;
  cursor: pointer;
}

.question-index:hover,
.question-index.active {
  border-color: rgba(88, 166, 255, 0.55);
  background: rgba(88, 166, 255, 0.12);
}

.question-index.done {
  border-color: rgba(126, 231, 135, 0.28);
}

.question-index.wrong {
  border-color: rgba(255, 123, 114, 0.38);
}

.question-index.pending {
  border-color: rgba(240, 185, 11, 0.35);
}

.index-number {
  font-family: var(--font-mono);
  font-size: 13px;
  color: #8ea2ba;
}

.index-main {
  min-width: 0;
}

.index-main span,
.index-main strong {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.index-main span {
  font-size: 12px;
  color: #91a2b8;
  margin-bottom: 3px;
}

.index-main strong {
  font-size: 13px;
  color: #edf3fb;
}

.question-panel {
  min-height: 620px;
}

.question-topline,
.answer-actions,
.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.star-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: #aebbd0;
  padding: 8px 11px;
  cursor: pointer;
}

.star-button.active {
  color: #f0b90b;
  border-color: rgba(240, 185, 11, 0.42);
  background: rgba(240, 185, 11, 0.12);
}

.question-title-row {
  margin: 24px 0 18px;
}

.question-order {
  display: inline-block;
  color: #8ea2ba;
  font-size: 13px;
  margin-bottom: 10px;
}

.question-title-row h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.55;
  letter-spacing: 0;
}

.option-list {
  display: grid;
  gap: 12px;
}

.option-button {
  width: 100%;
  display: grid;
  grid-template-columns: 38px 1fr 22px;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  color: #edf3fb;
  text-align: left;
  padding: 12px 14px;
  cursor: pointer;
}

.option-button:hover {
  border-color: rgba(88, 166, 255, 0.45);
  background: rgba(88, 166, 255, 0.09);
}

.option-button.selected {
  border-color: rgba(88, 166, 255, 0.75);
  background: rgba(88, 166, 255, 0.16);
}

.option-button.correct {
  border-color: rgba(126, 231, 135, 0.65);
  background: rgba(46, 160, 67, 0.15);
}

.option-button.wrong {
  border-color: rgba(255, 123, 114, 0.72);
  background: rgba(248, 81, 73, 0.13);
}

.option-button.submitted {
  cursor: default;
}

.option-key {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.08);
  color: #c8d4e4;
  font-weight: 700;
}

.option-text {
  min-width: 0;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.option-result {
  color: #7ee787;
  font-size: 18px;
}

.option-result.wrong {
  color: #ff7b72;
}

.answer-actions {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.selected-summary {
  color: #9fb0c3;
  font-size: 13px;
}

.selected-summary strong {
  color: #edf3fb;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.analysis-box {
  margin-top: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.04);
}

.analysis-box.correct {
  border-color: rgba(126, 231, 135, 0.35);
}

.analysis-box.wrong {
  border-color: rgba(255, 123, 114, 0.38);
}

.analysis-header {
  margin-bottom: 10px;
}

.analysis-header strong {
  display: block;
  margin-top: 6px;
}

.result-pill {
  display: inline-block;
  font-size: 12px;
  color: #0d1117;
  background: #7ee787;
  border-radius: 999px;
  padding: 2px 9px;
  font-weight: 700;
}

.analysis-box.wrong .result-pill {
  background: #ffb4ac;
}

.analysis-box p {
  color: #cbd8e7;
  line-height: 1.75;
}

.option-notes {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.note-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 8px;
  align-items: start;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.16);
}

.note-item span {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.08);
  font-weight: 700;
}

.note-item p {
  margin: 0;
  line-height: 1.6;
}

.knowledge-card {
  border: 1px solid rgba(126, 231, 135, 0.22);
  border-radius: 8px;
  background: rgba(46, 160, 67, 0.1);
  padding: 13px;
  margin-bottom: 12px;
}

.knowledge-card span {
  color: #a9b6c8;
  font-size: 12px;
}

.knowledge-card strong {
  display: block;
  margin: 6px 0;
  font-size: 17px;
}

.knowledge-card p {
  margin: 0;
  color: #c9d6e8;
  line-height: 1.6;
  font-size: 13px;
}

.quick-ask {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-window {
  display: grid;
  gap: 10px;
  max-height: calc(100vh - 456px);
  min-height: 260px;
  overflow: auto;
  padding-right: 2px;
  margin-bottom: 12px;
}

.chat-message {
  border-radius: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.chat-message.user {
  background: rgba(88, 166, 255, 0.13);
  border-color: rgba(88, 166, 255, 0.25);
}

.message-label {
  color: #8ea2ba;
  font-size: 12px;
  margin-bottom: 6px;
}

.chat-message p {
  margin: 0;
  color: #e8edf5;
  line-height: 1.65;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.ask-box {
  display: grid;
  gap: 10px;
}

@media (max-width: 1280px) {
  .practice-main {
    grid-template-columns: 270px minmax(420px, 1fr);
  }

  .tutor-panel {
    grid-column: 1 / -1;
  }

  .chat-window {
    max-height: 360px;
  }
}

@media (max-width: 900px) {
  .scsa-practice-page {
    padding: 14px;
  }

  .practice-header,
  .answer-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .action-buttons {
    justify-content: flex-start;
  }

  .stats-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .progress-bar {
    grid-column: 1 / -1;
  }

  .practice-main {
    grid-template-columns: 1fr;
  }

  .question-list,
  .chat-window {
    max-height: none;
  }

  .question-panel {
    min-height: 0;
  }
}

@media (max-width: 560px) {
  .title-block {
    align-items: flex-start;
  }

  .title-block h1 {
    font-size: 20px;
  }

  .question-title-row h2 {
    font-size: 18px;
  }

  .stats-strip {
    grid-template-columns: 1fr;
  }

  .option-button {
    grid-template-columns: 34px 1fr 20px;
    padding: 11px;
  }
}
</style>
