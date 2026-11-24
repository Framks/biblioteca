<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { request, send } from './api'

const emit = defineEmits(['saved', 'cancel'])
const readers = ref([])
const books = ref([])
const copies = ref([])
const selectedCopies = ref([])
const readerId = ref('')
const bookId = ref('')
const days = ref('')
const error = ref('')
const loadingOptions = ref(true)
const loadingCopies = ref(false)
const submitting = ref(false)
const optionsRequest = new AbortController()

const book = computed(() => books.value.find(item => item.id === bookId.value))
const selectedForBook = computed(() => selectedCopies.value.find(copy => copy.livroId === bookId.value))

onMounted(async () => {
  const { signal } = optionsRequest
  try {
    const [readerPage, bookPage] = await Promise.all([
      request('/readers?size=100', { signal }),
      request('/books?size=100', { signal }),
    ])
    readers.value = readerPage.content
    books.value = bookPage.content
  } catch (e) {
    if (!signal.aborted) error.value = e.message
  } finally {
    loadingOptions.value = false
  }
})
onUnmounted(() => optionsRequest.abort())

watch(bookId, async (id, _, onCleanup) => {
  error.value = ''
  copies.value = []
  loadingCopies.value = Boolean(id)
  if (!id) return

  const controller = new AbortController()
  onCleanup(() => controller.abort())
  const { signal } = controller
  try {
    const currentCopies = await request(`/books/${id}/copies`, { signal })
    if (!signal.aborted) copies.value = currentCopies.filter(copy => copy.status === 'DISPONIVEL')
  } catch (e) {
    if (!signal.aborted) error.value = e.message
  } finally {
    if (!signal.aborted) loadingCopies.value = false
  }
})

function removeCopy(id) {
  selectedCopies.value = selectedCopies.value.filter(copy => copy.id !== id)
}

function toggleCopy(copy, checked) {
  if (!checked) {
    removeCopy(copy.id)
    return
  }
  if (selectedForBook.value || !book.value) return
  selectedCopies.value.push({ ...copy, titulo: book.value.titulo })
}

async function submit() {
  if (submitting.value || !readerId.value || !selectedCopies.value.length) return
  submitting.value = true
  error.value = ''
  try {
    await send('/loans', 'POST', {
      leitorId: readerId.value,
      exemplarIds: selectedCopies.value.map(copy => copy.id),
      prazoDias: days.value || null,
    })
    emit('saved')
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="form-stack" @submit.prevent="submit">
    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <label>
      Leitor
      <select v-model="readerId" :disabled="loadingOptions || submitting" required>
        <option value="" disabled>Selecione um leitor</option>
        <option v-for="reader in readers" :key="reader.id" :value="reader.id">{{ reader.nome }}</option>
      </select>
    </label>
    <label>
      Livro
      <select v-model="bookId" :disabled="loadingOptions || submitting">
        <option value="" disabled>Selecione um livro</option>
        <option v-for="item in books" :key="item.id" :value="item.id">{{ item.titulo }}</option>
      </select>
    </label>
    <fieldset>
      <legend>Exemplares disponíveis</legend>
      <p v-if="loadingCopies" role="status">Carregando exemplares...</p>
      <p v-else-if="!bookId">Selecione um livro.</p>
      <p v-else-if="!copies.length && !error">Nenhum exemplar disponível para este livro.</p>
      <label v-for="copy in copies" :key="copy.id" class="inline-check">
        <input
          type="checkbox"
          :checked="selectedForBook?.id === copy.id"
          :disabled="submitting || Boolean(selectedForBook && selectedForBook.id !== copy.id)"
          @change="toggleCopy(copy, $event.target.checked)"
        >
        {{ copy.codigo }}
      </label>
    </fieldset>
    <fieldset>
      <legend>Exemplares selecionados</legend>
      <p v-if="!selectedCopies.length">Nenhum exemplar selecionado.</p>
      <ul v-else class="copy-list">
        <li v-for="copy in selectedCopies" :key="copy.id">
          <div><strong>{{ copy.titulo }}</strong><br><span>{{ copy.codigo }}</span></div>
          <button type="button" class="text-btn" :disabled="submitting" :aria-label="'Remover ' + copy.codigo" @click="removeCopy(copy.id)">Remover</button>
        </li>
      </ul>
    </fieldset>
    <label>Prazo em dias (opcional)<input v-model.number="days" type="number" min="1" :disabled="submitting"></label>
    <div class="modal-actions">
      <button type="button" class="btn btn-ghost" :disabled="submitting" @click="emit('cancel')">Cancelar</button>
      <button class="btn btn-primary" :disabled="submitting || !selectedCopies.length">{{ submitting ? 'Salvando...' : 'Salvar' }}</button>
    </div>
  </form>
</template>
