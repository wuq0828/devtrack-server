<template>
  <div class="mermaid-wrap">
    <div v-if="rendering" class="mermaid-state">
      <el-icon class="is-loading"><Loading /></el-icon> 正在渲染…
    </div>

    <!-- Rendered SVG -->
    <div v-show="!rendering && !error" ref="host" class="mermaid-host" />

    <!-- Fallback: invalid Mermaid syntax — show the raw source so nothing is lost -->
    <div v-if="error" class="mermaid-error">
      <div class="mermaid-error-tip">
        <el-icon><WarningFilled /></el-icon>
        图形渲染失败,已展示源码(可复制后手动修正)
      </div>
      <pre class="mermaid-source">{{ code }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps<{
  code: string
}>()

const host = ref<HTMLElement>()
const rendering = ref(false)
const error = ref(false)

// Monotonic id so each render targets a unique (valid) DOM id.
let seq = 0

// Mermaid is heavy — load it once, lazily, only when a diagram is actually shown.
let mermaidPromise: Promise<typeof import('mermaid').default> | null = null
function loadMermaid() {
  if (!mermaidPromise) {
    mermaidPromise = import('mermaid').then(({ default: mermaid }) => {
      mermaid.initialize({
        startOnLoad: false,
        theme: 'dark',
        securityLevel: 'loose',
        // 显式字体(测量与渲染同一字体),避免 'inherit' 导致中文宽度算错、节点文字偏移/溢出。
        fontFamily: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif',
        // SVG <text> labels (not <foreignObject>/HTML) so the diagram can be
        // rasterised to PNG reliably via canvas.
        htmlLabels: false,
        flowchart: {
          htmlLabels: false,
          nodeSpacing: 45,
          rankSpacing: 55,
          padding: 14,
          curve: 'basis',
          useMaxWidth: true,
        },
        themeVariables: {
          primaryColor: '#1b2236',
          primaryBorderColor: '#6366f1',
          primaryTextColor: '#e7e9f3',
          lineColor: '#818cf8',
          fontSize: '14px',
        },
      })
      return mermaid
    })
  }
  return mermaidPromise
}

async function render() {
  const code = props.code?.trim()
  if (!code) {
    error.value = false
    if (host.value) host.value.innerHTML = ''
    return
  }
  rendering.value = true
  error.value = false
  try {
    const mermaid = await loadMermaid()
    // Validate first so an invalid diagram doesn't throw into the DOM.
    const ok = await mermaid.parse(code, { suppressErrors: true })
    if (!ok) throw new Error('parse failed')
    const svg = await renderToSvg(mermaid, code)
    await nextTick()
    if (host.value) {
      host.value.innerHTML = svg
      centerMindmapLabels(host.value)
    }
  } catch {
    error.value = true
  } finally {
    rendering.value = false
  }
}

/**
 * Render with a collision-proof id and a one-shot retry. Mermaid injects a
 * temp <div id="d{id}"> while rendering; a stale leftover (e.g. after a dev
 * HMR reload) with the same id makes render throw. Unique ids + pre-cleanup
 * + retry keep rendering reliable.
 */
async function renderToSvg(
  mermaid: Awaited<ReturnType<typeof loadMermaid>>,
  code: string,
): Promise<string> {
  const attempt = async (): Promise<string> => {
    const id = `mmd-${seq++}-${Math.floor(Math.random() * 1e9)}`
    document.getElementById(id)?.remove()
    document.getElementById(`d${id}`)?.remove()
    const { svg } = await mermaid.render(id, code)
    return svg
  }
  try {
    return await attempt()
  } catch {
    // Single retry — clears transient state left by a previous failed render.
    return await attempt()
  }
}

/**
 * Mermaid mind-map mis-positions node labels (text sits right of / below the
 * box, and multi-line labels overlap). Measure each label's real bounding box
 * (getBBox) and translate the whole <text> so its centre matches the box rect's
 * centre — without touching internal line layout. Idempotent (transform is set,
 * not accumulated), so it's safe to re-run when the diagram becomes visible.
 */
function centerMindmapLabels(hostEl: HTMLElement): void {
  hostEl.querySelectorAll<SVGGElement>('g[class*="mindmap-node"]').forEach((g) => {
    const rect = Array.from(g.querySelectorAll<SVGRectElement>('rect')).find(
      (r) => r.width.baseVal.value > 0,
    )
    const text = g.querySelector<SVGTextElement>('text')
    if (!rect || !text) return
    let bb: DOMRect
    try {
      bb = text.getBBox()
    } catch {
      return
    }
    // Hidden (e.g. inactive tab) → getBBox is empty; the ResizeObserver re-runs
    // this once the diagram becomes visible.
    if (bb.width === 0 && bb.height === 0) return
    // Horizontal only: mermaid already vertically centres labels; a getBBox-based
    // vertical shift over-corrects (the glyph box includes descender space).
    const boxCx = rect.x.baseVal.value + rect.width.baseVal.value / 2
    const dx = boxCx - (bb.x + bb.width / 2)
    text.setAttribute('transform', `translate(${dx.toFixed(2)},0)`)
  })
}

// Re-centre labels when the diagram resizes or transitions hidden → visible
// (mind-map often lives in an inactive tab where getBBox is 0 at first render).
let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  if (typeof ResizeObserver !== 'undefined' && host.value) {
    resizeObserver = new ResizeObserver(() => {
      if (host.value) centerMindmapLabels(host.value)
    })
    resizeObserver.observe(host.value)
  }
})
onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

watch(() => props.code, render, { immediate: true })

/**
 * Rasterise the currently rendered SVG to a PNG and trigger a download.
 * Returns false when there's nothing to export (not rendered / errored).
 */
async function exportPng(filename = 'diagram'): Promise<boolean> {
  const svgEl = host.value?.querySelector('svg')
  if (!svgEl) return false

  // Size from the viewBox (works even when the tab is hidden, unlike getBBox).
  const vb = svgEl.viewBox?.baseVal
  let width = vb && vb.width ? vb.width : svgEl.getBoundingClientRect().width || 800
  let height = vb && vb.height ? vb.height : svgEl.getBoundingClientRect().height || 600

  // Clone with explicit pixel size so the serialized SVG rasterises predictably.
  const clone = svgEl.cloneNode(true) as SVGSVGElement
  clone.setAttribute('width', String(width))
  clone.setAttribute('height', String(height))
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')

  const xml = new XMLSerializer().serializeToString(clone)
  const svgUrl = URL.createObjectURL(new Blob([xml], { type: 'image/svg+xml;charset=utf-8' }))

  try {
    const img = new Image()
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('image load failed'))
      img.src = svgUrl
    })

    const scale = 2 // retina-crisp export
    const canvas = document.createElement('canvas')
    canvas.width = Math.ceil(width * scale)
    canvas.height = Math.ceil(height * scale)
    const ctx = canvas.getContext('2d')
    if (!ctx) return false
    // Solid dark backdrop — the diagram is light-on-dark, transparent PNG would
    // be unreadable on a white background.
    ctx.fillStyle = '#11162a'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.scale(scale, scale)
    ctx.drawImage(img, 0, 0, width, height)

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
    if (!blob) return false
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${filename}.png`
    a.click()
    URL.revokeObjectURL(a.href)
    return true
  } finally {
    URL.revokeObjectURL(svgUrl)
  }
}

defineExpose({ exportPng })
</script>

<style scoped>
.mermaid-wrap {
  width: 100%;
  min-height: 120px;
}

.mermaid-host {
  width: 100%;
  overflow-x: auto;
  display: flex;
  justify-content: center;
}

.mermaid-host :deep(svg) {
  max-width: 100%;
  height: auto;
}

.mermaid-state {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9298b4;
  padding: 32px 0;
  justify-content: center;
}

.mermaid-error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fbbf24;
  font-size: 13px;
  margin-bottom: 8px;
}

.mermaid-source {
  margin: 0;
  padding: 12px 14px;
  background: rgba(15, 20, 36, 0.6);
  border: 1px solid rgba(130, 140, 200, 0.18);
  border-radius: 10px;
  color: #c2c6da;
  font-family: monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
}
</style>
