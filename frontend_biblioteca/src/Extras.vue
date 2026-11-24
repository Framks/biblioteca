<script setup>
import { ref, onMounted } from 'vue'
import { request, send } from './api'
const props = defineProps({kind:String, data:Object})
const emit = defineEmits(['close','changed'])
const error = ref(''), rows = ref([]), roles = ref([]), permissions = ref([]), selectedRole = ref(null), form = ref({}), report = ref('loans-per-month'), reportFilter = ref({}), reportRows = ref([]), historyPage = ref(0), historyPages = ref(0)
const labels = {genres:'Gêneros literários',history:'Histórico de empréstimos',access:'Habilitar acesso',copy:'Editar exemplar',user:'Editar usuário',userSessions:'Sessões do usuário',roles:'Perfis e permissões',reports:'Relatórios detalhados'}
const condition = ['NOVO','EXCELENTE','CONSERVADO','ACEITAVEL','MAL_CONSERVADO','DANIFICADO']
const statuses = ['DISPONIVEL','MANUTENCAO','BAIXADO']
async function run(fn) { error.value = ''; try { await fn() } catch(e) { error.value = e.message || 'Não foi possível conectar ao servidor.' } }
async function loadHistory(index = 0) { const result = await request(`/readers/${props.data.id}/loans?page=${index}&size=20`); rows.value = result.content; historyPage.value = result.page; historyPages.value = result.totalPages }
async function load() { await run(async () => {
 if (props.kind === 'genres') rows.value = await request('/genres')
 if (props.kind === 'history') await loadHistory()
 if (props.kind === 'userSessions') rows.value = await request(`/users/${props.data.id}/sessions`)
 if (props.kind === 'roles') { [roles.value,permissions.value] = await Promise.all([request('/roles'),request('/permissions')]); selectRole(roles.value[0]) }
 if (props.kind === 'reports') await loadReport()
 if (props.kind === 'copy') form.value = {codigo:props.data.codigo,condicao:props.data.condicao,status:props.data.status}
 if (props.kind === 'user') form.value = {nome:props.data.nome,email:props.data.email,telefone:props.data.telefone,perfil:props.data.perfil}
}) }
function selectRole(role) { selectedRole.value = role; form.value = {permissoes:[...(role?.permissoes || [])]} }
function params() { const p = new URLSearchParams(); for (const [key,val] of Object.entries(reportFilter.value)) if (val) p.set(key,val); return p.toString() }
async function loadReport() { reportRows.value = await request(`/reports/${report.value}?${params()}`) }
async function save() { await run(async () => {
 if (props.kind === 'genres') { await send(form.value.id ? `/genres/${form.value.id}` : '/genres',form.value.id ? 'PUT':'POST',{nome:form.value.nome}); form.value = {}; rows.value = await request('/genres') }
 if (props.kind === 'access') { await send(`/readers/${props.data.id}/access`,'POST',{senha:form.value.senha}); emit('close') }
 if (props.kind === 'copy') { const data = {...form.value}; if (data.status === 'EMPRESTADO') delete data.status; await send(`/copies/${props.data.id}`,'PATCH',data); emit('close') }
 if (props.kind === 'user') { await send(`/users/${props.data.id}`,'PUT',form.value); emit('close') }
 if (props.kind === 'roles') { await send(`/roles/${selectedRole.value.id}/permissions`,'PUT',form.value); await load() }
 emit('changed')
}) }
async function revoke(id) { await run(async () => { await send(`/auth/sessions/${id}`,'DELETE'); rows.value = await request(`/users/${props.data.id}/sessions`); emit('changed') }) }
async function download() { await run(async () => { const blob = await request(`/reports/${report.value}/export?${params()}`,{raw:true}); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `${report.value}.csv`; a.click(); URL.revokeObjectURL(url) }) }
onMounted(load)
</script>
<template>
 <div class="modal-backdrop extras-backdrop" @click.self="emit('close')"><section class="modal extras-modal" role="dialog" aria-modal="true" :aria-label="labels[kind]"><div class="modal-head"><div><span class="eyebrow">BIBLIOTECA COMUNITÁRIA</span><h2>{{ labels[kind] }}</h2></div><button class="icon-btn" aria-label="Fechar" @click="emit('close')">×</button></div>
 <div v-if="error" class="alert" role="alert">{{ error }}</div>
 <template v-if="kind === 'genres'"><ul class="copy-list"><li v-for="g in rows" :key="g.id">{{ g.nome }} <button class="text-btn" @click="form = {...g}">Editar</button></li></ul><form class="form-stack" @submit.prevent="save"><label>Nome do gênero<input v-model="form.nome" required maxlength="100"></label><div><button class="btn btn-primary">{{ form.id ? 'Atualizar' : 'Adicionar' }}</button> <button v-if="form.id" type="button" class="btn btn-ghost" @click="form = {}">Cancelar edição</button></div></form></template>
 <template v-if="kind === 'history'"><p>{{ data.nome }}</p><div v-for="loan in rows" :key="loan.id" class="extra-item"><strong>Empréstimo #{{ loan.id }} · {{ loan.situacao }}</strong><p v-for="item in loan.itens" :key="item.id">{{ item.tituloLivro }} — {{ item.situacao }} — {{ item.dataDevolucaoPrevista }}</p></div><p v-if="!rows.length">Nenhum empréstimo encontrado.</p><div v-if="historyPages > 1" class="pager"><button class="btn btn-outline" :disabled="historyPage === 0" @click="run(() => loadHistory(historyPage-1))">Anterior</button>{{ historyPage + 1 }} / {{ historyPages }}<button class="btn btn-outline" :disabled="historyPage + 1 >= historyPages" @click="run(() => loadHistory(historyPage+1))">Próxima</button></div></template>
 <form v-if="kind === 'access'" class="form-stack" @submit.prevent="save"><p>Defina uma senha para {{ data.nome }}.</p><label>Senha<input v-model="form.senha" type="password" minlength="8" maxlength="72" required></label><button class="btn btn-primary">Salvar acesso</button></form>
 <form v-if="kind === 'copy'" class="form-stack" @submit.prevent="save"><label>Código<input v-model="form.codigo" required maxlength="30"></label><label>Condição<select v-model="form.condicao"><option v-for="c in condition" :key="c">{{ c }}</option></select></label><label>Status<select v-model="form.status" :disabled="data.status === 'EMPRESTADO' || data.status === 'BAIXADO'"><option v-if="data.status === 'EMPRESTADO'">EMPRESTADO</option><option v-for="s in statuses" :key="s">{{ s }}</option></select></label><button class="btn btn-primary">Salvar</button></form>
 <form v-if="kind === 'user'" class="form-stack" @submit.prevent="save"><label>Nome<input v-model="form.nome" required></label><label>E-mail<input v-model="form.email" type="email" required></label><label>Telefone<input v-model="form.telefone"></label><label>Perfil<select v-model="form.perfil"><option value="FUNCIONARIO">Funcionário</option><option value="ADMIN">Administrador</option></select></label><button class="btn btn-primary">Salvar</button></form>
 <template v-if="kind === 'userSessions'"><p>{{ data.nome }}</p><div v-for="s in rows" :key="s.id" class="extra-item"><strong>{{ s.userAgent }}</strong><p>{{ s.ip }} · {{ s.ultimoAcesso }}</p><button class="text-btn" @click="revoke(s.id)">Encerrar sessão</button></div><p v-if="!rows.length">Nenhuma sessão ativa.</p></template>
 <template v-if="kind === 'roles'"><label>Perfil <select :value="selectedRole?.id" @change="selectRole(roles.find(r => r.id === Number($event.target.value)))"><option v-for="r in roles" :key="r.id" :value="r.id">{{ r.nome }}</option></select></label><form v-if="selectedRole" class="form-stack" @submit.prevent="save"><p v-if="!selectedRole.editavel">Este perfil não pode ser alterado.</p><label v-for="p in permissions" :key="p.codigo" class="inline-check"><input v-model="form.permissoes" type="checkbox" :value="p.codigo" :disabled="!selectedRole.editavel"> {{ p.descricao }} ({{ p.codigo }})</label><button v-if="selectedRole.editavel" class="btn btn-primary">Salvar permissões</button></form></template>
 <template v-if="kind === 'reports'"><form class="form-stack" @submit.prevent="run(loadReport)"><label>Relatório<select v-model="report" @change="reportFilter = {}; run(loadReport)"><option value="loans-per-month">Empréstimos por mês</option><option value="top-books">Livros mais emprestados</option><option value="overdue-readers">Leitores em atraso</option></select></label><div v-if="report === 'loans-per-month'" class="form-grid"><label>De<input v-model="reportFilter.inicio" type="month"></label><label>Até<input v-model="reportFilter.fim" type="month"></label></div><div v-if="report === 'top-books'" class="form-grid"><label>De<input v-model="reportFilter.de" type="date"></label><label>Até<input v-model="reportFilter.ate" type="date"></label><label>Limite<input v-model="reportFilter.limite" type="number" min="1" max="100"></label></div><div><button class="btn btn-primary">Consultar</button> <button type="button" class="btn btn-outline" @click="download">Exportar CSV</button></div></form><div v-for="(row,i) in reportRows" :key="i" class="extra-item"><p v-for="(value,key) in row" :key="key"><strong>{{ key }}:</strong> {{ Array.isArray(value) ? value.join(', ') : value ?? '—' }}</p></div><p v-if="!reportRows.length">Nenhum resultado encontrado.</p></template>
 </section></div>
</template>
