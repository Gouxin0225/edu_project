<template>
  <div class="page">
    <el-card shadow="never" class="form-card cyber-card">
      <el-form :model="examForm" :rules="formRules" ref="examFormRef" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="考试标题" prop="title">
              <el-input v-model="examForm.title" placeholder="请输入考试标题" clearable class="cyber-input" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标班级" prop="targetClassIds">
              <el-select
                v-model="examForm.targetClassIds"
                multiple
                collapse-tags
                placeholder="请选择班级"
                style="width:100%"
                class="cyber-select"
              >
                <el-option
                  v-for="c in classList"
                  :key="c.id"
                  :label="c.className"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number v-model="examForm.duration" :min="1" :max="300" style="width:100%" class="cyber-input-number" />
              <span class="unit-label">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="examForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width:100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="cyber-date-picker"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="截止时间" prop="endTime">
              <el-date-picker
                v-model="examForm.endTime"
                type="datetime"
                placeholder="选择截止时间"
                style="width:100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="cyber-date-picker"
                @change="handleExamEndTimeChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分数" prop="passScore">
              <el-input-number v-model="examForm.passScore" :min="1" :max="examForm.totalScore || 100" style="width:100%" class="cyber-input-number" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="交卷问卷">
              <el-switch
                v-model="examForm.requireSurveyBeforeSubmit"
                active-text="提交前必填问卷"
                inactive-text="不要求"
                @change="handleSurveyRequirementChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16" v-if="examForm.requireSurveyBeforeSubmit">
            <el-form-item label="必填问卷" prop="requiredSurveyId">
              <el-select
                v-model="examForm.requiredSurveyId"
                placeholder="请选择已发布问卷"
                clearable
                filterable
                style="width:100%"
                class="cyber-select"
              >
                <el-option
                  v-for="survey in publishedSurveyList"
                  :key="survey.surveyId"
                  :label="getSurveyOptionLabel(survey)"
                  :value="survey.surveyId"
                  :disabled="!isSurveySelectable(survey)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="ai-paper-strip">
        <div>
          <div class="ai-paper-title">AI 一键组卷</div>
          <div class="ai-paper-desc">优先从现有题库抽题，不足时自动 AI 生成补齐。</div>
        </div>
        <el-button type="success" :icon="MagicStick" :loading="aiPaperLoading" @click="openAiPaperDialog" class="cyber-btn ai-paper-btn">
          一键组卷
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="cyber-card">
          <template #header>
            <span class="cyber-section-title">题库浏览</span>
          </template>
          <div class="filter-bar">
            <el-select
              v-model="filter.courseCategory"
              placeholder="题目类型"
              clearable
              style="width:140px"
              @change="handleSearch"
              class="cyber-select"
            >
              <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-select v-model="filter.type" placeholder="全部题型" clearable style="width:120px" @change="handleSearch" class="cyber-select">
              <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-select v-model="filter.difficulty" placeholder="全部难度" clearable style="width:120px" @change="handleSearch" class="cyber-select">
              <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
            <el-select v-model="filter.creatorId" placeholder="全部创建人" clearable style="width:140px" @change="handleSearch" class="cyber-select">
              <el-option v-for="u in creatorList" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="handleSearch" class="cyber-btn">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset" class="cyber-btn-secondary">重置</el-button>
          </div>

          <div class="batch-bar">
            <span class="batch-hint">已勾选 {{ tableSelection.length }} 题</span>
            <el-button
              type="primary"
              size="small"
              :disabled="tableSelection.length === 0"
              @click="addSelectedQuestions"
              class="cyber-btn-small"
            >批量加入试卷</el-button>
          </div>

          <el-table
            ref="questionTableRef"
            :data="tableData"
            v-loading="tableLoading"
            stripe
            border
            style="width:100%"
            @selection-change="handleSelectionChange"
            class="cyber-table"
          >
            <el-table-column type="selection" width="40" :selectable="isSelectable" />
            <el-table-column label="题目内容" min-width="260">
              <template #default="{ row }">
                <div class="question-preview">
                  <div class="question-content-text">{{ formatQuestionContent(row.content) }}</div>
                  <div v-if="formatOptionPreview(row.optionsJson)" class="question-option-snippet">
                    {{ formatOptionPreview(row.optionsJson) }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="knowledgePoint" label="知识点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="courseCategory" label="课程方向" width="100" show-overflow-tooltip />
            <el-table-column label="题型" width="80">
              <template #default="{ row }">
                <el-tag :type="typeTagType(row.type)" size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="70">
              <template #default="{ row }">
                <el-tag :type="diffTagType(row.difficulty)" size="small" class="cyber-tag">{{ diffLabel(row.difficulty) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="!selectedIds.has(row.id)"
                  size="small"
                  type="primary"
                  @click="addQuestion(row)"
                  class="cyber-btn-text"
                >加入试卷</el-button>
                <el-tag v-else type="success" size="small" class="cyber-tag">已添加</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchQuestions"
              @size-change="(s: number) => { pagination.size = s; pagination.page = 1; fetchQuestions() }"
              class="cyber-pagination"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <div class="cart-container">
          <el-card shadow="never" class="cart-card cyber-card-accent">
            <template #header>
              <div class="cart-header">
                <div class="cart-title-group">
                  <span class="cyber-section-title">已选题目</span>
                  <el-tag type="info" class="cyber-tag">{{ selectedQuestions.length }} 题</el-tag>
                </div>
                <el-button
                  size="small"
                  type="primary"
                  :icon="FullScreen"
                  :disabled="selectedQuestions.length === 0"
                  @click="openSelectedQuestionDialog()"
                  class="cyber-btn-small"
                >
                  放大查看
                </el-button>
              </div>
            </template>

            <div class="cart-stats">
              <span class="stats-label">总分：</span>
              <span :class="totalScore === 100 ? 'score-ok' : 'score-warning'">{{ totalScore }}</span>
              <span class="stats-unit">/ 100 分</span>
              <el-tag v-if="totalScore !== 100" type="danger" size="small" style="margin-left:8px" class="cyber-tag-danger">总分需为100</el-tag>
            </div>

            <el-table
              v-if="selectedQuestions.length > 0"
              :data="selectedQuestions"
              stripe
              size="small"
              style="width:100%"
              max-height="420"
              class="cyber-table"
            >
              <el-table-column label="序号" width="50" align="center">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="题目内容" min-width="180">
                <template #default="{ row }">
                  <div class="selected-question-preview">
                    {{ formatQuestionContent(row.content) }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="题型" width="70">
                <template #default="{ row }">
                  <el-tag :type="typeTagType(row.type)" size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="难度" width="60">
                <template #default="{ row }">
                  <el-tag :type="diffTagType(row.difficulty)" size="small" class="cyber-tag">{{ diffLabel(row.difficulty) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="120">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.score"
                    :min="0"
                    :max="100"
                    size="small"
                    controls-position="right"
                    class="cyber-input-number-small"
                    @change="(val: number) => handleScoreChange(row.id, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="96" align="center">
                <template #default="{ row }">
                  <div class="selected-actions">
                    <el-tooltip content="放大查看" placement="top">
                      <el-button
                        size="small"
                        type="primary"
                        text
                        :icon="View"
                        @click="openSelectedQuestionDialog(row.id)"
                        class="cyber-btn-view"
                      />
                    </el-tooltip>
                    <el-tooltip content="删除并 AI 补一题" placement="top">
                      <el-button
                        size="small"
                        type="warning"
                        text
                        :icon="MagicStick"
                        :loading="regeneratingIds.has(row.id)"
                        @click="regenerateQuestion(row)"
                        class="cyber-btn-regenerate"
                      />
                    </el-tooltip>
                    <el-tooltip content="删除题目" placement="top">
                      <el-button
                        size="small"
                        type="danger"
                        text
                        :icon="Delete"
                        @click="removeQuestion(row.id)"
                        class="cyber-btn-delete"
                      />
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-else description="请从左侧题库添加题目" :image-size="60" class="cyber-empty" />

            <div class="cart-actions">
              <el-button
                type="warning"
                :icon="MagicStick"
                :disabled="selectedQuestions.length === 0 || !examId"
                :loading="autoScoreLoading"
                @click="handleAutoScore"
                class="cyber-btn-warning"
              >
                一键分配100分
              </el-button>
              <el-button
                type="primary"
                :icon="Promotion"
                :disabled="!canPublish"
                :loading="publishLoading"
                @click="handlePublish"
                class="cyber-btn"
              >
                发布考试
              </el-button>
              <el-button
                :disabled="selectedQuestions.length === 0"
                :loading="savingTemplate"
                @click="showSaveTemplateDialog = true"
                class="cyber-btn-save-template"
              >
                保存为模板
              </el-button>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <div class="bottom-bar cyber-bottom-bar">
      <el-button @click="handleBack" class="cyber-btn-secondary">返回列表</el-button>
      <el-button type="primary" :disabled="!canSaveExam" :loading="createLoading" @click="handleSaveExam" class="cyber-btn">
        {{ examId ? '保存基础信息' : '创建考试' }}
      </el-button>
    </div>

    <el-dialog v-model="showSaveTemplateDialog" title="保存为模板" width="450px" class="cyber-dialog">
      <el-form :model="templateForm" label-width="90px" class="cyber-form">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="3" placeholder="请输入模板描述（可选）" class="cyber-textarea" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="templateForm.courseName" placeholder="请输入课程名称（可选）" class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveTemplateDialog = false" class="cyber-btn-secondary">取消</el-button>
        <el-button type="primary" @click="handleSaveTemplate" :loading="savingTemplate" class="cyber-btn">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showSelectedQuestionDialog"
      title="已选题目与分值设置"
      width="92vw"
      top="4vh"
      class="cyber-dialog selected-question-dialog"
    >
      <div class="selected-dialog-summary">
        <div class="selected-dialog-stat">
          <span>已选 {{ selectedQuestions.length }} 题</span>
          <span :class="totalScore === 100 ? 'score-ok' : 'score-warning'">总分 {{ totalScore }} / 100</span>
        </div>
        <el-button
          type="warning"
          :icon="MagicStick"
          :disabled="selectedQuestions.length === 0 || !examId"
          :loading="autoScoreLoading"
          @click="handleAutoScore"
          class="cyber-btn-warning"
        >
          一键分配100分
        </el-button>
      </div>

      <div v-if="selectedQuestions.length > 0" class="selected-question-board">
        <article
          v-for="(question, index) in selectedQuestions"
          :key="question.id"
          class="selected-question-detail-card"
          :class="{ active: selectedQuestionDialogFocusId === question.id }"
        >
          <header class="selected-question-detail-header">
            <div class="selected-question-heading">
              <span class="selected-question-index">第 {{ index + 1 }} 题</span>
              <el-tag :type="typeTagType(question.type)" size="small" class="cyber-tag">{{ typeLabel(question.type) }}</el-tag>
              <el-tag :type="diffTagType(question.difficulty)" size="small" class="cyber-tag">{{ diffLabel(question.difficulty) }}</el-tag>
              <span class="selected-question-knowledge">{{ question.knowledgePoint || '未标注知识点' }}</span>
            </div>
            <div class="selected-question-score-editor">
              <span>分值</span>
              <el-input-number
                v-model="question.score"
                :min="0"
                :max="100"
                size="small"
                controls-position="right"
                class="cyber-input-number-small"
                @change="(val: number) => handleScoreChange(question.id, val)"
              />
            </div>
          </header>

          <div class="selected-question-full-content">
            {{ formatQuestionContent(question.content) }}
          </div>

          <div v-if="parseOptions(question.optionsJson).length > 0" class="selected-question-option-list">
            <div
              v-for="(option, optionIndex) in parseOptions(question.optionsJson)"
              :key="`${question.id}-${optionIndex}`"
              class="selected-question-option-item"
            >
              <span>{{ optionLetter(optionIndex) }}.</span>
              <p>{{ option }}</p>
            </div>
          </div>

          <div class="selected-question-reference-grid">
            <div class="selected-question-reference-block">
              <span>参考答案</span>
              <p>{{ formatReferenceText(question.standardAnswer, '未填写参考答案') }}</p>
            </div>
            <div v-if="question.analysis" class="selected-question-reference-block">
              <span>解析</span>
              <p>{{ formatReferenceText(question.analysis, '暂无解析') }}</p>
            </div>
          </div>

          <footer class="selected-question-detail-actions">
            <el-button
              type="warning"
              text
              :icon="MagicStick"
              :loading="regeneratingIds.has(question.id)"
              @click="openRegenerateDialog(question)"
              class="cyber-btn-regenerate"
            >
              AI 补一题
            </el-button>
            <el-button
              type="danger"
              text
              :icon="Delete"
              @click="removeQuestion(question.id)"
              class="cyber-btn-delete"
            >
              删除
            </el-button>
          </footer>
        </article>
      </div>
      <el-empty v-else description="请先从题库添加题目" :image-size="80" class="cyber-empty" />

      <template #footer>
        <el-button @click="showSelectedQuestionDialog = false" class="cyber-btn-secondary">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAiRegenerateDialog" title="AI 补一题" width="520px" :close-on-click-modal="false" class="cyber-dialog">
      <el-form :model="aiRegenerateForm" label-width="90px" class="cyber-form">
        <el-form-item label="题目类型" required>
          <el-select v-model="aiRegenerateForm.courseCategory" placeholder="请选择题目类型" style="width:100%" class="cyber-select">
            <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="aiRegenerateForm.knowledgePoint" placeholder="可选，如：异常处理、SQL 查询、路由协议" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="aiRegenerateForm.type" placeholder="请选择题型" style="width:100%" class="cyber-select">
            <el-option v-for="t in QUESTION_TYPES_REF" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="aiRegenerateForm.difficulty" placeholder="请选择难度" style="width:100%" class="cyber-select">
            <el-option v-for="d in DIFFICULTIES_REF" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充要求">
          <el-input
            v-model="aiRegenerateForm.context"
            type="textarea"
            :rows="3"
            placeholder="可选。比如题目场景、考察侧重点、避免和原题重复等"
            class="cyber-textarea"
          />
        </el-form-item>
        <div class="ai-paper-note">
          <el-tag type="warning" size="small" class="cyber-tag">替换当前题</el-tag>
          <span>生成成功后会删除当前题，并保留当前题原分值。</span>
        </div>
      </el-form>

      <template #footer>
        <el-button :disabled="aiRegenerateLoading" @click="showAiRegenerateDialog = false" class="cyber-btn-secondary">取消</el-button>
        <el-button type="warning" :icon="MagicStick" :loading="aiRegenerateLoading" @click="handleRegenerateQuestion" class="cyber-btn">
          {{ aiRegenerateLoading ? '补题中...' : '开始补题' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAiPaperDialog" title="AI 一键组卷" width="560px" :close-on-click-modal="false" class="cyber-dialog">
      <el-form :model="aiPaperForm" label-width="100px" class="cyber-form">
        <el-form-item label="题目类型" required>
          <el-select v-model="aiPaperForm.courseCategory" placeholder="请选择题目类型" style="width:100%" class="cyber-select">
            <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="aiPaperForm.knowledgePoint" placeholder="可选，如：异常处理、SQL 查询、路由协议" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="aiPaperForm.types" multiple collapse-tags placeholder="请选择题型" style="width:100%" class="cyber-select">
            <el-option v-for="t in QUESTION_TYPES_REF" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="aiPaperForm.difficulty" style="width:100%" class="cyber-select">
            <el-option v-for="d in DIFFICULTIES_REF" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目数量" required>
          <el-input-number v-model="aiPaperForm.count" :min="1" :max="100" :step="1" controls-position="right" class="cyber-input-number" />
        </el-form-item>
        <el-form-item label="补充要求">
          <el-input
            v-model="aiPaperForm.context"
            type="textarea"
            :rows="3"
            placeholder="可选。比如覆盖范围、场景背景、题目风格等"
            class="cyber-textarea"
          />
        </el-form-item>
        <el-form-item label="当前题目">
          <el-switch
            v-model="aiPaperForm.replaceCurrent"
            active-text="替换当前已选"
            inactive-text="追加到当前已选"
          />
        </el-form-item>
        <div class="ai-paper-note">
          <el-tag type="success" size="small" class="cyber-tag">题库优先</el-tag>
          <span>系统会先抽取符合条件且你可见的题库题目；数量不够时再 AI 生成并保存到题库。</span>
        </div>
      </el-form>

      <template #footer>
        <el-button :disabled="aiPaperLoading" @click="showAiPaperDialog = false" class="cyber-btn-secondary">取消</el-button>
        <el-button type="success" :icon="MagicStick" :loading="aiPaperLoading" @click="handleAiPaperGenerate" class="cyber-btn">
          {{ aiPaperLoading ? '组卷中...' : '开始组卷' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Search, Refresh, Delete, MagicStick, Promotion, FullScreen, View } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { QUESTION_TYPES, DIFFICULTIES, COURSE_CATEGORIES, getQuestionCreators, generateAiPaper } from '@/api/question'
import type { QuestionRecord, QuestionType, Difficulty, CourseCategory } from '@/api/question'
import {
  getMyClasses, getQuestionList, createExam, updateExam, getExamDetail,
  addExamQuestions, removeExamQuestion, autoScore as autoScoreApi,
  updateQuestionScore, publishExam, getExamQuestions
} from '@/api/exam'
import type { ClassInfo, ExamQuestionItem } from '@/api/exam'
import { createTemplate } from '@/api/template'
import { getSurveyList, type SurveyListItem } from '@/api/survey'

const router = useRouter()
const route = useRoute()

const QUESTION_TYPES_REF = QUESTION_TYPES
const DIFFICULTIES_REF = DIFFICULTIES

const examFormRef = ref()
const examForm = reactive({
  title: '',
  startTime: '',
  endTime: '',
  targetClassIds: [] as number[],
  duration: 90,
  totalScore: 100,
  passScore: 60,
  requireSurveyBeforeSubmit: false,
  requiredSurveyId: null as number | null
})

const formRules = {
  title: [{ required: true, message: '请输入考试标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  targetClassIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个班级', trigger: 'change' }],
  requiredSurveyId: [
    {
      validator: (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
        if (examForm.requireSurveyBeforeSubmit && !value) {
          callback(new Error('请选择必填问卷'))
          return
        }
        const selectedSurvey = surveyList.value.find(survey => survey.surveyId === value)
        if (examForm.requireSurveyBeforeSubmit && selectedSurvey && !isSurveySelectable(selectedSurvey)) {
          callback(new Error('必填问卷截止时间不能早于考试截止时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const classList = ref<ClassInfo[]>([])
const surveyList = ref<SurveyListItem[]>([])
const publishedSurveyList = computed(() => surveyList.value.filter(survey => survey.status === 1))

const filter = reactive({ courseCategory: '', type: '', difficulty: '', creatorId: '' })
const tableData = ref<QuestionRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const questionTableRef = ref()
const tableSelection = ref<QuestionRecord[]>([])
const creatorList = ref<{ id: number; realName: string; role: string }[]>([])

async function fetchQuestions() {
  tableLoading.value = true
  try {
    const res = await getQuestionList({
      page: pagination.page,
      size: pagination.size,
      courseCategory: filter.courseCategory || undefined,
      type: filter.type || undefined,
      difficulty: filter.difficulty || undefined,
      creatorId: filter.creatorId || undefined
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchQuestions()
}

function handleReset() {
  filter.courseCategory = ''
  filter.type = ''
  filter.difficulty = ''
  filter.creatorId = ''
  handleSearch()
}

function handleSelectionChange(rows: QuestionRecord[]) {
  tableSelection.value = rows
}

function isSelectable(row: QuestionRecord) {
  return !selectedIds.value.has(row.id)
}

async function addSelectedQuestions() {
  const toAdd = tableSelection.value.filter(row => !selectedIds.value.has(row.id))
  if (toAdd.length === 0) return

  try {
    if (examId.value) {
      await addExamQuestions(examId.value, toAdd.map(row => row.id))
      await fetchExamQuestions()
    } else {
      toAdd.forEach(row => {
        selectedQuestions.value.push({ ...row, score: 0 })
      })
    }
    questionTableRef.value?.clearSelection()
    ElMessage.success(`已添加 ${toAdd.length} 道题目`)
  } catch {
    ElMessage.error('添加题目失败')
  }
}

const selectedQuestions = ref<ExamQuestionItem[]>([])
const selectedIds = computed(() => new Set(selectedQuestions.value.map(q => q.id)))

async function addQuestion(row: QuestionRecord) {
  if (selectedIds.value.has(row.id)) return
  try {
    if (examId.value) {
      await addExamQuestions(examId.value, [row.id])
      await fetchExamQuestions()
    } else {
      selectedQuestions.value.push({ ...row, score: 0 })
    }
  } catch {
    ElMessage.error('添加题目失败')
  }
}

async function removeQuestion(questionId: number) {
  try {
    if (examId.value) {
      await removeExamQuestion(examId.value, questionId)
    }
    const remainingQuestions = selectedQuestions.value.filter(q => q.id !== questionId)
    selectedQuestions.value = remainingQuestions
    if (selectedQuestionDialogFocusId.value === questionId) {
      selectedQuestionDialogFocusId.value = remainingQuestions[0]?.id ?? null
    }
  } catch {
    ElMessage.error('移除题目失败')
  }
}

function setRegenerating(questionId: number, loading: boolean) {
  const next = new Set(regeneratingIds.value)
  if (loading) {
    next.add(questionId)
  } else {
    next.delete(questionId)
  }
  regeneratingIds.value = next
}

async function regenerateQuestion(row: ExamQuestionItem) {
  if (!aiRegenerateForm.courseCategory || !aiRegenerateForm.type || !aiRegenerateForm.difficulty) {
    ElMessage.warning('请完整选择题目类型、题型和难度')
    return false
  }

  setRegenerating(row.id, true)
  try {
    const res = await generateAiPaper({
      courseCategory: aiRegenerateForm.courseCategory,
      knowledgePoint: aiRegenerateForm.knowledgePoint || undefined,
      context: aiRegenerateForm.context || undefined,
      types: [aiRegenerateForm.type],
      difficulty: aiRegenerateForm.difficulty,
      count: 1,
      excludeQuestionIds: selectedQuestions.value.map(q => q.id),
      preferExisting: false
    })
    const replacement = res.data.questions?.[0]
    if (!replacement) {
      ElMessage.warning('AI没有生成新的题目')
      return false
    }

    const nextQuestion = toExamQuestionItem(replacement, row.score || 0)
    if (examId.value) {
      await addExamQuestions(examId.value, [nextQuestion.id])
      if ((nextQuestion.score || 0) > 0) {
        await updateQuestionScore(examId.value, nextQuestion.id, nextQuestion.score || 0)
      }
      await removeExamQuestion(examId.value, row.id)
    }
    selectedQuestions.value = selectedQuestions.value.map(q => q.id === row.id ? nextQuestion : q)
    selectedQuestionDialogFocusId.value = nextQuestion.id
    ElMessage.success('已删除原题并由AI补充新题')
    fetchQuestions()
    return true
  } catch {
    ElMessage.error('AI补题失败')
    return false
  } finally {
    setRegenerating(row.id, false)
  }
}

function openRegenerateDialog(row: ExamQuestionItem) {
  aiRegenerateForm.questionId = row.id
  aiRegenerateForm.courseCategory = (row.courseCategory as CourseCategory) || (filter.courseCategory as CourseCategory) || COURSE_CATEGORIES[0].value
  aiRegenerateForm.knowledgePoint = row.knowledgePoint || ''
  aiRegenerateForm.type = (row.type as QuestionType) || (filter.type as QuestionType) || 'SINGLE'
  aiRegenerateForm.difficulty = (row.difficulty as Difficulty) || (filter.difficulty as Difficulty) || 'MEDIUM'
  aiRegenerateForm.context = ''
  showAiRegenerateDialog.value = true
}

async function handleRegenerateQuestion() {
  const questionId = aiRegenerateForm.questionId
  const row = selectedQuestions.value.find(q => q.id === questionId)
  if (!row) {
    ElMessage.warning('当前题目不存在，请刷新后重试')
    return
  }
  aiRegenerateLoading.value = true
  try {
    const success = await regenerateQuestion(row)
    if (success) {
      showAiRegenerateDialog.value = false
    }
  } finally {
    aiRegenerateLoading.value = false
  }
}

async function handleScoreChange(questionId: number, score: number) {
  if (!examId.value) return
  try {
    await updateQuestionScore(examId.value, questionId, score)
  } catch {
    ElMessage.error('分值更新失败')
  }
}

const totalScore = computed(() =>
  selectedQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
)

const examId = ref<number | null>(null)
const createLoading = ref(false)
const autoScoreLoading = ref(false)
const publishLoading = ref(false)
const savingTemplate = ref(false)
const showSaveTemplateDialog = ref(false)
const templateForm = reactive({
  name: '',
  description: '',
  courseName: ''
})
const showAiPaperDialog = ref(false)
const showAiRegenerateDialog = ref(false)
const showSelectedQuestionDialog = ref(false)
const selectedQuestionDialogFocusId = ref<number | null>(null)
const aiPaperLoading = ref(false)
const aiRegenerateLoading = ref(false)
const regeneratingIds = ref<Set<number>>(new Set())
const aiPaperForm = reactive({
  courseCategory: '' as CourseCategory | '',
  knowledgePoint: '',
  types: ['SINGLE', 'MULTIPLE', 'JUDGE'] as QuestionType[],
  difficulty: 'MEDIUM' as Difficulty,
  count: 10,
  context: '',
  replaceCurrent: true
})
const aiRegenerateForm = reactive({
  questionId: null as number | null,
  courseCategory: '' as CourseCategory | '',
  knowledgePoint: '',
  type: '' as QuestionType | '',
  difficulty: '' as Difficulty | '',
  context: ''
})

const canSaveExam = computed(() =>
  examForm.title &&
  examForm.startTime &&
  examForm.endTime &&
  examForm.targetClassIds.length > 0 &&
  selectedQuestions.value.length > 0 &&
  (!examForm.requireSurveyBeforeSubmit || !!examForm.requiredSurveyId)
)

const canPublish = computed(() =>
  examId.value &&
  selectedQuestions.value.length > 0 &&
  totalScore.value === 100
)

function openSelectedQuestionDialog(questionId?: number) {
  selectedQuestionDialogFocusId.value = questionId ?? selectedQuestions.value[0]?.id ?? null
  showSelectedQuestionDialog.value = true
}

function handleSurveyRequirementChange(value: boolean | string | number) {
  if (!value) {
    examForm.requiredSurveyId = null
  }
}

function handleExamEndTimeChange() {
  if (!examForm.requiredSurveyId) return
  const selectedSurvey = surveyList.value.find(survey => survey.surveyId === examForm.requiredSurveyId)
  if (selectedSurvey && !isSurveySelectable(selectedSurvey)) {
    examForm.requiredSurveyId = null
    ElMessage.warning('已清空截止时间早于考试截止时间的问卷')
  }
}

function isSurveySelectable(survey: SurveyListItem) {
  if (survey.status !== 1) return false
  if (!examForm.endTime) return true
  const examEnd = parseDateTime(examForm.endTime)
  const surveyEnd = parseDateTime(survey.endTime)
  if (!Number.isFinite(examEnd) || !Number.isFinite(surveyEnd)) return true
  return surveyEnd >= examEnd
}

function getSurveyOptionLabel(survey: SurveyListItem) {
  const suffix = isSurveySelectable(survey) ? '' : '，截止早于考试'
  return `${survey.title}（${formatTime(survey.endTime)}截止${suffix}）`
}

function parseDateTime(value?: string | null) {
  return value ? Date.parse(value.replace(' ', 'T')) : Number.NaN
}

function buildExamPayload() {
  return {
    title: examForm.title,
    startTime: examForm.startTime,
    endTime: examForm.endTime,
    targetClassIds: examForm.targetClassIds,
    duration: examForm.duration,
    totalScore: 100,
    passScore: examForm.passScore,
    requireSurveyBeforeSubmit: examForm.requireSurveyBeforeSubmit,
    requiredSurveyId: examForm.requireSurveyBeforeSubmit ? examForm.requiredSurveyId : null
  }
}

function openAiPaperDialog() {
  aiPaperForm.courseCategory = (filter.courseCategory as CourseCategory) || aiPaperForm.courseCategory || COURSE_CATEGORIES[0].value
  aiPaperForm.types = filter.type ? [filter.type as QuestionType] : (aiPaperForm.types.length ? aiPaperForm.types : ['SINGLE', 'MULTIPLE', 'JUDGE'])
  aiPaperForm.difficulty = (filter.difficulty as Difficulty) || aiPaperForm.difficulty || 'MEDIUM'
  aiPaperForm.count = selectedQuestions.value.length || aiPaperForm.count || 10
  showAiPaperDialog.value = true
}

function validateAiPaperForm() {
  if (!aiPaperForm.courseCategory) {
    ElMessage.warning('请选择题目类型')
    return false
  }
  if (aiPaperForm.types.length === 0) {
    ElMessage.warning('请至少选择一种题型')
    return false
  }
  if (!aiPaperForm.difficulty) {
    ElMessage.warning('请选择难度')
    return false
  }
  if (!aiPaperForm.count || aiPaperForm.count < 1) {
    ElMessage.warning('题目数量必须大于0')
    return false
  }
  return true
}

function toExamQuestionItem(question: QuestionRecord, score = 0): ExamQuestionItem {
  return {
    ...question,
    id: question.id,
    questionId: question.id,
    score
  }
}

function distributeQuestionScores(questions: ExamQuestionItem[]) {
  if (questions.length === 0) return
  const minimumScore = questions.length <= 100 ? 1 : 0
  const distributableScore = 100 - minimumScore * questions.length
  const weightedQuestions = questions.map((question, index) => {
    const weight = getQuestionScoreWeight(question)
    const priority = getQuestionPriority(question)
    return { question, index, weight, priority, score: minimumScore, remainder: 0 }
  })
  const totalWeight = weightedQuestions.reduce((sum, item) => sum + item.weight, 0) || questions.length
  let allocatedScore = 0
  weightedQuestions.forEach(item => {
    const exactScore = minimumScore + distributableScore * (item.weight / totalWeight)
    item.score = Math.floor(exactScore)
    item.remainder = exactScore - item.score
    allocatedScore += item.score
  })
  let remainingScore = 100 - allocatedScore
  weightedQuestions
    .slice()
    .sort((left, right) =>
      right.remainder - left.remainder ||
      right.priority - left.priority ||
      right.weight - left.weight ||
      left.index - right.index
    )
    .forEach(item => {
      if (remainingScore <= 0) return
      item.score += 1
      remainingScore -= 1
    })
  weightedQuestions
    .sort((left, right) => left.index - right.index)
    .forEach(item => {
      item.question.score = item.score
    })
}

function getQuestionScoreWeight(question: ExamQuestionItem) {
  const typeWeight: Record<string, number> = {
    SINGLE: 1,
    JUDGE: 1,
    MULTIPLE: 1.25,
    SHORT: 1.8,
    CODE: 2.2
  }
  const difficultyWeight: Record<string, number> = {
    EASY: 0.9,
    MEDIUM: 1,
    HARD: 1.25
  }
  return (typeWeight[question.type] ?? 1) * (difficultyWeight[question.difficulty] ?? 1)
}

function getQuestionPriority(question: ExamQuestionItem) {
  const typePriority: Record<string, number> = {
    SINGLE: 1,
    JUDGE: 1,
    MULTIPLE: 2,
    SHORT: 3,
    CODE: 4
  }
  const difficultyPriority: Record<string, number> = {
    EASY: 1,
    MEDIUM: 2,
    HARD: 3
  }
  return (typePriority[question.type] ?? 1) * 10 + (difficultyPriority[question.difficulty] ?? 1)
}

async function syncGeneratedQuestions(nextQuestions: ExamQuestionItem[], previousIds: number[]) {
  if (!examId.value) {
    selectedQuestions.value = nextQuestions
    return
  }

  const nextIds = nextQuestions.map(q => q.id)
  const idsToAdd = nextIds.filter(id => !previousIds.includes(id))
  if (idsToAdd.length > 0) {
    await addExamQuestions(examId.value, idsToAdd)
  }

  await Promise.all(nextQuestions.map(q => updateQuestionScore(examId.value!, q.id, q.score || 0)))

  const idsToRemove = previousIds.filter(id => !nextIds.includes(id))
  await Promise.all(idsToRemove.map(id => removeExamQuestion(examId.value!, id)))
  selectedQuestions.value = nextQuestions
  await fetchExamQuestions()
}

async function handleAiPaperGenerate() {
  if (!validateAiPaperForm()) return

  aiPaperLoading.value = true
  const previousIds = selectedQuestions.value.map(q => q.id)
  try {
    const res = await generateAiPaper({
      courseCategory: aiPaperForm.courseCategory,
      knowledgePoint: aiPaperForm.knowledgePoint || undefined,
      context: aiPaperForm.context || undefined,
      types: aiPaperForm.types,
      difficulty: aiPaperForm.difficulty,
      count: aiPaperForm.count,
      excludeQuestionIds: aiPaperForm.replaceCurrent ? [] : previousIds,
      preferExisting: true
    })

    const generatedQuestions = (res.data.questions || []).map(q => toExamQuestionItem(q))
    if (generatedQuestions.length === 0) {
      ElMessage.warning('没有生成可加入试卷的题目')
      return
    }

    const nextQuestions = aiPaperForm.replaceCurrent
      ? generatedQuestions
      : [
          ...selectedQuestions.value,
          ...generatedQuestions.filter(q => !selectedIds.value.has(q.id))
        ]
    distributeQuestionScores(nextQuestions)
    await syncGeneratedQuestions(nextQuestions, previousIds)
    showAiPaperDialog.value = false
    ElMessage.success(res.data.message || `已完成组卷，共 ${generatedQuestions.length} 题`)
    fetchQuestions()
  } catch {
    ElMessage.error('AI组卷失败')
  } finally {
    aiPaperLoading.value = false
  }
}

async function handleSaveExam() {
  const valid = await examFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }

  createLoading.value = true
  try {
    if (examId.value) {
      await updateExam(examId.value, buildExamPayload())
      ElMessage.success('考试基础信息已保存')
      return
    }

    const res = await createExam(buildExamPayload())
    examId.value = res.data.id
    ElMessage.success('考试创建成功')

    const qIds = selectedQuestions.value.map(q => q.id)
    await addExamQuestions(examId.value, qIds)
    const scoredQuestions = selectedQuestions.value.filter(q => (q.score || 0) > 0)
    await Promise.all(scoredQuestions.map(q => updateQuestionScore(examId.value!, q.id, q.score || 0)))
    await fetchExamQuestions()
    ElMessage.success('题目已添加到试卷')
  } catch {
    ElMessage.error('创建考试失败')
  } finally {
    createLoading.value = false
  }
}

async function handleAutoScore() {
  if (!examId.value) {
    ElMessage.warning('请先创建考试')
    return
  }
  autoScoreLoading.value = true
  try {
    await autoScoreApi(examId.value)
    ElMessage.success('100分智能分配完成')
    await fetchExamQuestions()
  } catch {
    ElMessage.error('智能分配失败')
  } finally {
    autoScoreLoading.value = false
  }
}

async function fetchExamQuestions() {
  if (!examId.value) return
  const res = await getExamQuestions(examId.value)
  const existingMap = new Map(selectedQuestions.value.map(q => [q.id, q]))
  selectedQuestions.value = (res.data || []).map((q) => {
    const existing = existingMap.get(q.questionId || q.id)
    return {
      ...(existing || {}),
      ...q,
      id: q.questionId || q.id,
      questionId: q.questionId || q.id,
      score: q.score || 0
    }
  })
}

async function handlePublish() {
  const valid = await examFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }

  if (totalScore.value !== 100) {
    ElMessage.warning('总分必须为100分才能发布')
    return
  }
  if (!examId.value) {
    ElMessage.warning('请先创建考试')
    return
  }

  publishLoading.value = true
  try {
    await updateExam(examId.value, buildExamPayload())
    await publishExam(examId.value)
    ElMessage.success('考试信息已保存并发布')
    router.push('/teacher/exams')
  } catch {
    ElMessage.error('发布失败')
  } finally {
    publishLoading.value = false
  }
}

function handleBack() {
  router.push('/teacher/exams')
}

async function handleSaveTemplate() {
  if (!templateForm.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }
  
  savingTemplate.value = true
  try {
    await createTemplate({
      name: templateForm.name,
      description: templateForm.description,
      courseName: templateForm.courseName,
      questionIds: selectedQuestions.value.map(q => q.id),
      scores: selectedQuestions.value.map(q => q.score || 0),
      totalScore: totalScore.value
    })
    ElMessage.success('模板保存成功')
    showSaveTemplateDialog.value = false
    templateForm.name = ''
    templateForm.description = ''
    templateForm.courseName = ''
  } catch {
    ElMessage.error('保存模板失败')
  } finally {
    savingTemplate.value = false
  }
}

const typeLabel = (v: string) => QUESTION_TYPES.find(t => t.value === v)?.label ?? v
const diffLabel = (v: string) => DIFFICULTIES.find(d => d.value === v)?.label ?? v
const typeTagType = (v: string): any =>
  ({ SINGLE: '', MULTIPLE: 'success', JUDGE: 'warning', SHORT: 'info', CODE: 'danger' }[v] ?? '')
const diffTagType = (v: string): any =>
  ({ EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[v] ?? '')

function formatQuestionContent(content?: string | null) {
  return content?.trim() || '未填写题目内容'
}

function optionLetter(index: number) {
  return String.fromCharCode(65 + index)
}

function parseOptions(optionsJson?: string | null): string[] {
  if (!optionsJson) return []
  try {
    const parsed = JSON.parse(optionsJson) as unknown
    const optionList = Array.isArray(parsed)
      ? parsed
      : (parsed && typeof parsed === 'object' ? Object.values(parsed as Record<string, unknown>) : [])
    return optionList
      .map((option) => {
        if (typeof option === 'string') return option
        if (option && typeof option === 'object') {
          const record = option as Record<string, unknown>
          return String(record.content ?? record.text ?? record.label ?? record.value ?? '')
        }
        return String(option ?? '')
      })
      .map(option => option.trim())
      .filter(Boolean)
  } catch {
    return []
  }
}

function formatReferenceText(value?: string | null, fallback = '-') {
  return value?.trim() || fallback
}

function formatTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatOptionPreview(optionsJson?: string | null) {
  const options = parseOptions(optionsJson)
  if (options.length === 0) return ''
  return options
    .slice(0, 4)
    .map((option, index) => `${optionLetter(index)}. ${option}`)
    .join(' / ')
}

function normalizeDateTime(value?: string | null) {
  return value ? value.replace(' ', 'T').slice(0, 19) : ''
}

async function loadExistingExam(id: number) {
  examId.value = id
  const res = await getExamDetail(id)
  const exam = res.data
  examForm.title = exam.title || ''
  examForm.startTime = normalizeDateTime(exam.startTime)
  examForm.endTime = normalizeDateTime(exam.endTime)
  examForm.targetClassIds = exam.targetClassIds || []
  examForm.duration = exam.duration || 90
  examForm.passScore = exam.passScore || 60
  examForm.totalScore = exam.totalScore || 100
  examForm.requireSurveyBeforeSubmit = !!exam.requireSurveyBeforeSubmit
  examForm.requiredSurveyId = exam.requiredSurveyId || null
  await fetchExamQuestions()
}

onMounted(async () => {
  const routeExamId = Number(route.params.id)
  fetchQuestions()
  try {
    const res = await getMyClasses()
    classList.value = res.data || []
  } catch {
    ElMessage.error('获取班级列表失败')
  }
  const creatorRes = await getQuestionCreators()
  creatorList.value = creatorRes.data || []
  try {
    const surveyRes = await getSurveyList()
    surveyList.value = surveyRes.data || []
  } catch {
    ElMessage.error('获取问卷列表失败')
  }
  if (Number.isFinite(routeExamId) && routeExamId > 0) {
    try {
      await loadExistingExam(routeExamId)
    } catch {
      ElMessage.error('加载考试草稿失败')
      router.push('/teacher/exams')
    }
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 80px;
  background: var(--bg-surface) !important;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace !important;
}

.form-card :deep(.el-card__body) {
  padding-bottom: 10px;
}

.ai-paper-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-top: 4px;
  border: 1px solid rgba(57, 255, 20, 0.22);
  background: linear-gradient(135deg, rgba(57, 255, 20, 0.08), rgba(0, 255, 255, 0.05));
  clip-path: polygon(10px 0, 100% 0, 100% calc(100% - 10px), calc(100% - 10px) 100%, 0 100%, 0 10px);
}

.ai-paper-title {
  color: #39ff14;
  font-size: 14px;
  font-weight: 700;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.45);
}

.ai-paper-desc,
.ai-paper-note {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.ai-paper-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.22);
}

.ai-paper-btn {
  flex-shrink: 0;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, transparent 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent {
  background: var(--bg-surface) !important;
  border: 1px solid #ff10f0 !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.15), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.15) 0%, transparent 100%) !important;
  border-bottom: 1px solid #ff10f0 !important;
  padding: 16px 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-section-title {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-card-accent .cyber-section-title {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-table {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.15) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  color: #00ffff !important;
  border-bottom: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  text-transform: uppercase !important;
  font-size: 12px !important;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__body-wrapper .el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-checkbox__inner) {
  background: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
}

.cyber-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-table :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #00ffff !important;
}

.question-preview {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.question-content-text,
.selected-question-preview {
  color: #f4fbff;
  font-size: 13px;
  line-height: 1.55;
  white-space: normal;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.selected-question-preview {
  font-size: 12px;
  -webkit-line-clamp: 3;
}

.question-option-snippet {
  color: rgba(57, 255, 20, 0.78);
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.24);
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 0, 0, 0.5) !important;
  border: 1px solid currentColor !important;
  box-shadow: 0 0 8px currentColor !important;
}

.cyber-tag-danger {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(255, 16, 240, 0.1) !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn:hover {
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-secondary {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-weight: 600 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-secondary:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-btn-small {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 600 !important;
  font-size: 12px !important;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-small:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-1px) !important;
}

.cyber-btn-small:disabled {
  background: var(--bg-surface) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
}

.cyber-btn-warning {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #ff9800 0%, #cc7a00 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(255, 152, 0, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-warning:hover:not(:disabled) {
  box-shadow: 0 0 25px rgba(255, 152, 0, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-warning:disabled {
  background: var(--bg-surface) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  color: #00ffff !important;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5) !important;
  transition: all 0.3s ease !important;
  font-size: 12px !important;
}

.cyber-btn-text:hover {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.8) !important;
}

.cyber-btn-delete {
  font-family: 'JetBrains Mono', monospace !important;
  color: #ff10f0 !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-delete:hover {
  color: #ff4444 !important;
  text-shadow: 0 0 10px rgba(255, 68, 68, 0.8) !important;
}

.selected-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.cyber-btn-view {
  font-family: 'JetBrains Mono', monospace !important;
  color: #00ffff !important;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-view:hover {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.8) !important;
}

.cyber-btn-regenerate {
  font-family: 'JetBrains Mono', monospace !important;
  color: #39ff14 !important;
  text-shadow: 0 0 5px rgba(57, 255, 20, 0.5) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-regenerate:hover {
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8) !important;
}

.cyber-btn-save-template {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #ff10f0 0%, #cc0dc0 100%) !important;
  border: none !important;
  color: var(--text-primary)fff !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-save-template:hover:not(:disabled) {
  box-shadow: 0 0 25px rgba(255, 16, 240, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-save-template:disabled {
  background: var(--bg-surface) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.5) !important;
}

.cyber-pagination :deep(.btn-prev), .cyber-pagination :deep(.btn-next) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.btn-prev:hover), .cyber-pagination :deep(.btn-next:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.batch-hint {
  font-size: 13px;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cart-container {
  position: sticky;
  top: 16px;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.cart-title-group {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.cart-stats {
  padding: 12px 0;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #ff10f0 !important;
  margin-bottom: 12px;
  font-family: 'JetBrains Mono', monospace !important;
}

.stats-label {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.stats-unit {
  color: #909399 !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 14px !important;
}

.score-ok {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.score-warning {
  color: #ff9800 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.5) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.unit-label {
  margin-left: 8px;
  color: #ff10f0 !important;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.cart-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  justify-content: center;
}

.selected-question-dialog {
  max-width: calc(100vw - 32px);
}

.selected-question-dialog :deep(.el-dialog__body) {
  max-height: 76vh;
  overflow: hidden;
  padding: 18px 20px !important;
}

.selected-dialog-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border: 1px solid rgba(0, 255, 255, 0.22);
  background: rgba(0, 255, 255, 0.05);
}

.selected-dialog-stat {
  display: flex;
  align-items: center;
  gap: 18px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace !important;
}

.selected-question-board {
  display: grid;
  gap: 14px;
  max-height: 62vh;
  padding-right: 6px;
  overflow-y: auto;
}

.selected-question-detail-card {
  border: 1px solid var(--border);
  background: rgba(10, 10, 10, 0.96);
  padding: 16px;
  box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.35);
}

.selected-question-detail-card.active {
  border-color: #00ffff;
  box-shadow: 0 0 18px rgba(0, 255, 255, 0.2), inset 0 0 40px rgba(0, 0, 0, 0.35);
}

.selected-question-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.selected-question-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.selected-question-index {
  color: #00ffff;
  font-weight: 700;
  font-size: 14px;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.4);
}

.selected-question-knowledge {
  color: rgba(224, 224, 224, 0.72);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.selected-question-score-editor {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  color: #ff10f0;
  font-size: 13px;
  font-weight: 700;
}

.selected-question-full-content {
  color: #f4fbff;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.selected-question-option-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.selected-question-option-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(57, 255, 20, 0.18);
  background: rgba(57, 255, 20, 0.04);
  color: #dfffe0;
}

.selected-question-option-item span {
  color: #39ff14;
  font-weight: 700;
}

.selected-question-option-item p,
.selected-question-reference-block p {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.selected-question-reference-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.selected-question-reference-block {
  padding: 12px;
  border: 1px solid rgba(255, 16, 240, 0.18);
  background: rgba(255, 16, 240, 0.04);
  color: var(--text-primary);
}

.selected-question-reference-block span {
  display: inline-block;
  margin-bottom: 6px;
  color: #ff10f0;
  font-size: 12px;
  font-weight: 700;
}

.selected-question-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .selected-dialog-summary,
  .selected-question-detail-header {
    align-items: stretch;
    flex-direction: column;
  }

  .selected-dialog-stat {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .selected-question-score-editor {
    justify-content: space-between;
  }

  .selected-question-option-list,
  .selected-question-reference-grid {
    grid-template-columns: 1fr;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(10, 10, 10, 0.95) 0%, rgba(3, 3, 3, 0.98) 100%) !important;
  border-top: 1px solid var(--border) !important;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  z-index: 100;
  box-shadow: 0 -5px 20px rgba(0, 255, 255, 0.1), 0 -5px 40px rgba(255, 16, 240, 0.05) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-empty :deep(.el-empty__description) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
}

.cyber-empty :deep(.el-empty__image svg) {
  fill: #39ff14 !important;
  filter: drop-shadow(0 0 10px rgba(57, 255, 20, 0.5)) !important;
}

.cyber-input :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  transition: all 0.3s ease !important;
}

.cyber-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-input :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-select :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  transition: all 0.3s ease !important;
}

.cyber-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-select :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-select :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-select :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(255, 16, 240, 0.2) !important;
  color: #ff10f0 !important;
}

.cyber-input-number :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  transition: all 0.3s ease !important;
}

.cyber-input-number :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-input-number :deep(.el-input-number__decrease),
.cyber-input-number :deep(.el-input-number__increase) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  border-color: var(--border-subtle) !important;
}

.cyber-input-number :deep(.el-input-number__decrease:hover),
.cyber-input-number :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.cyber-input-number :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input-number-small :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  transition: all 0.3s ease !important;
  padding: 0 5px !important;
}

.cyber-input-number-small :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 12px !important;
}

.cyber-input-number-small :deep(.el-input-number__decrease),
.cyber-input-number-small :deep(.el-input-number__increase) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  border-color: var(--border-subtle) !important;
  width: 18px !important;
  font-size: 10px !important;
}

.cyber-input-number-small :deep(.el-input-number__decrease:hover),
.cyber-input-number-small :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.cyber-date-picker :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  transition: all 0.3s ease !important;
}

.cyber-date-picker :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-date-picker :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-date-picker :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-picker-panel) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-picker__header-label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-table th) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-table td.available:hover .el-date-table-cell__text) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-date-picker :deep(.el-date-table td.current:not(.disabled) .el-date-table-cell__text) {
  background: #00ffff !important;
  color: #0a0a0a !important;
}

.cyber-date-picker :deep(.el-picker-panel__icon-btn) {
  color: #00ffff !important;
}

.cyber-date-picker :deep(.el-time-panel) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-time-spinner__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-time-spinner__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-date-picker :deep(.el-time-spinner__item.is-active) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-textarea :deep(.el-textarea__inner) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) inset !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  transition: all 0.3s ease !important;
}

.cyber-textarea :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-card :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
}

.cyber-card :deep(.el-form-item__error) {
  color: #ff9800 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tag--primary) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-tag--success) {
  background: rgba(57, 255, 20, 0.1) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

.cyber-card :deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.1) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3) !important;
}

.cyber-card :deep(.el-tag--danger) {
  background: rgba(255, 16, 240, 0.1) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-card :deep(.el-tag--info) {
  background: rgba(224, 224, 224, 0.1) !important;
  border-color: var(--text-primary) !important;
  color: var(--text-primary) !important;
  box-shadow: 0 0 8px rgba(224, 224, 224, 0.2) !important;
}

.cyber-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.cyber-card :deep(.el-loading-spinner .el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-loading-spinner .path) {
  stroke: #00ffff !important;
}

.cyber-card :deep(.el-message-box) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__content) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-overlay) {
  background: rgba(3, 3, 3, 0.85) !important;
}

.cyber-card :deep(.el-dialog) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
}

.cyber-card :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 20px !important;
}

.cyber-card :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 24px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-dialog__footer) {
  background: var(--bg-surface) !important;
  border-top: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

.cyber-card :deep(.el-popover.el-popper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-popover.el-popper.is-light) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

.cyber-card :deep(.el-popover__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tooltip__popper.is-dark) {
  background: linear-gradient(135deg, var(--bg-surface) 0%, var(--bg-surface) 100%) !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-slider__runway) {
  background: var(--bg-surface) !important;
}

.cyber-card :deep(.el-slider__bar) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-slider__button) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-progress__text) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-progress-bar__outer) {
  background: var(--bg-surface) !important;
}

.cyber-card :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}
</style>
