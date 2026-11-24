<script setup>
import { ref, computed, onMounted } from 'vue'
import { request, send, setToken, restoreSession } from './api'
import Extras from './Extras.vue'
import LoanForm from './LoanForm.vue'

const user = ref(null), page = ref('catalogo'), menu = ref(false), loginOpen = ref(false), modal = ref('')
const busy = ref(false), error = ref(''), notice = ref(''), search = ref(''), available = ref(false)
const books = ref([]), readers = ref([]), loans = ref([]), genres = ref([]), copies = ref([]), sessions = ref([]), settings = ref([]), audit = ref([]), users = ref([]), summary = ref(null), selected = ref(null)
const loanStatus = ref('')
const extra = ref(''), extraData = ref(null), filters = ref({ field:'titulo', genre:'', status:'' })
const form = ref({}), credentials = ref({ email: '', senha: '' }), pagination = ref({page: 0, totalPages: 0})
const colors = ['#c85d3b','#d59a32','#536b59','#9b7048','#576b82','#7c5a76']
const nav = [
  {id:'catalogo', label:'Catálogo', icon:'▦'}, {id:'meus', label:'Meus empréstimos', icon:'◷', permission:'loan_read_own'},
  {id:'acervo', label:'Acervo', icon:'▤', permission:'copy_read'}, {id:'leitores', label:'Leitores', icon:'♙', permission:'reader_read'},
  {id:'circulacao', label:'Circulação', icon:'⇄', permission:'loan_read'}, {id:'relatorios', label:'Relatórios', icon:'▥', permission:'report_read'},
  {id:'equipe', label:'Equipe', icon:'♧', permission:'user_manage'}, {id:'config', label:'Configurações', icon:'⚙', permission:'config_manage'},
  {id:'auditoria', label:'Auditoria', icon:'☷', permission:'audit_read'}, {id:'sessoes', label:'Minhas sessões', icon:'◉', permission:'session_manage_own'},
]
const visibleNav = computed(() => nav.filter(n => !n.permission || user.value?.permissoes?.includes(n.permission)))
const title = computed(() => nav.find(n => n.id === page.value)?.label || 'Catálogo')
const can = permission => user.value?.permissoes?.includes(permission)
const items = value => Array.isArray(value) ? value : value?.content || []
const date = value => value ? new Intl.DateTimeFormat('pt-BR', {timeZone:'America/Fortaleza'}).format(new Date(value.length === 10 ? `${value}T12:00:00` : value)) : '—'
const genreNames = book => (book.generos || []).map(g => typeof g === 'string' ? g : g.nome)
function showError(e) { error.value = e.message || 'Não foi possível conectar ao servidor.' }
function notify(message) { notice.value = message; setTimeout(() => { notice.value = '' }, 3500) }
async function load(target = page.value, index = 0) {
  busy.value = true; error.value = ''
  try {
    if (target === 'catalogo' || target === 'acervo') {
      const params = new URLSearchParams({page: String(index), size:'20'})
      if (search.value.trim()) params.set(filters.value.field === 'isbn' && target === 'catalogo' ? 'titulo' : filters.value.field, search.value.trim())
      if (filters.value.genre) params.set('generoId', filters.value.genre)
      if (target === 'acervo' && filters.value.status) params.set('disponibilidade', filters.value.status)
      if (target === 'catalogo' && available.value) params.set('apenasDisponiveis','true')
      const result = await request(`/${target === 'catalogo' ? 'catalog/books' : 'books'}?${params}`)
      books.value = items(result); pagination.value = {page: result.page || 0, totalPages: result.totalPages || 0}
    } else if (['leitores','circulacao','meus','auditoria','equipe'].includes(target)) {
      const paths = {leitores:'/readers',circulacao:'/loans',meus:'/loans/me',auditoria:'/audit',equipe:'/users'}
      const query = new URLSearchParams({size:'20',page:String(index)}); if (['circulacao','meus'].includes(target) && loanStatus.value) query.set('situacao',loanStatus.value)
      const result = await request(`${paths[target]}?${query}`); const list = items(result)
      if (target === 'leitores') readers.value = list; if (target === 'circulacao' || target === 'meus') loans.value = list; if (target === 'auditoria') audit.value = list; if (target === 'equipe') users.value = list
      pagination.value = {page: result.page || 0, totalPages: result.totalPages || 0}
    }
    else if (target === 'circulacao' || target === 'meus') loans.value = items(await request(target === 'meus' ? '/loans/me?size=100' : '/loans?size=100'))
    else if (target === 'sessoes') sessions.value = items(await request('/auth/sessions'))
    else if (target === 'config') settings.value = items(await request('/settings'))
    else if (target === 'auditoria') audit.value = items(await request('/audit?size=100'))
    else if (target === 'equipe') users.value = items(await request('/users?size=100'))
    else if (target === 'relatorios') summary.value = await request('/reports/summary')
  } catch (e) { showError(e) } finally { busy.value = false }
}
function navigate(id) { page.value = id; menu.value = false; search.value = ''; load(id) }
async function login() {
  error.value = ''; busy.value = true
  try { const data = await send('/auth/login','POST',credentials.value); setToken(data.accessToken); user.value = data.usuario; credentials.value = {email:'',senha:''}; loginOpen.value = false; navigate(can('loan_read_own') ? 'meus' : 'catalogo'); notify('Bem-vindo(a) de volta!') } catch(e) { showError(e) } finally { busy.value = false }
}
function clearSession() {
  setToken(null)
  user.value = null
  sessions.value = []
  navigate('catalogo')
}
async function logout() {
  try {
    await send('/auth/logout', 'POST')
  } catch (e) {
    showError(e)
  } finally {
    clearSession()
  }
}
async function endSession(session) {
  const path = `/auth/sessions/${session.id}`
  if (!session.atual) {
    await action(path, 'DELETE', undefined, 'Sessão encerrada.')
    return
  }
  error.value = ''
  try {
    await send(path, 'DELETE')
    clearSession()
    notify('Sessão encerrada.')
  } catch (e) {
    showError(e)
  }
}
function openUserSessions(member) {
  if (member.id === user.value?.id && can('session_manage_own')) {
    navigate('sessoes')
  } else {
    extraData.value = member
    extra.value = 'userSessions'
  }
}
async function openBook(book) { selected.value = book; copies.value = []; modal.value = 'book'; if (can('copy_read')) try { copies.value = items(await request(`/books/${book.id}/copies`)) } catch(e) { showError(e) } }
async function openForm(type, value = null) {
  error.value = ''; form.value = value ? {...value} : {}; modal.value = type
  try {
    if (type === 'bookForm') { const [genreData, detail] = await Promise.all([request('/genres'), value ? request(`/books/${value.id}`) : null]); genres.value = items(genreData); if (detail) form.value = {...detail}; form.value.generoIds = detail ? detail.generos.map(g => g.id) : [] }
    if (type === 'readerForm' && !value) form.value.tipoDocumento = 'CPF'
  } catch(e) { showError(e) }
}
async function loanSaved() {
  modal.value = ''
  notify('Empréstimo registrado.')
  await load()
}
async function submitForm() {
  error.value = ''
  try {
    if (modal.value === 'bookForm') { const {id,titulo,autor,isbn,editora,anoPublicacao,generoIds} = form.value; await send(id ? `/books/${id}` : '/books',id ? 'PUT':'POST',{titulo,autor,isbn:isbn || null,editora:editora || null,anoPublicacao:anoPublicacao ? Number(anoPublicacao) : null,generoIds}) }
    if (modal.value === 'readerForm') { const {id,nome,tipoDocumento,documento,email,telefone,endereco} = form.value; await send(id ? `/readers/${id}` : '/readers',id ? 'PUT':'POST',{nome,tipoDocumento,documento,email:email || null,telefone:telefone || null,endereco:endereco || null}) }
    if (modal.value === 'return') await send(`/loans/${selected.value.loanId}/return`,'POST',{itens:[{itemId:selected.value.id,condicao:form.value.condicao}]})
    if (modal.value === 'userForm') await send('/users','POST',form.value)
    modal.value = ''; notify('Operação realizada com sucesso.'); await load()
  } catch(e) { showError(e) }
}
async function action(path, method, body, message) { error.value = ''; try { await send(path,method,body); notify(message); await load() } catch(e) { showError(e) } }
async function addCopies() { try { await send(`/books/${selected.value.id}/copies`,'POST',{quantidade:1,condicao:'NOVO'}); notify('Exemplar adicionado.'); await openBook(selected.value) } catch(e) { showError(e) } }
async function downloadReport() { try { const blob = await request('/reports/summary/export',{raw:true}); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'resumo-circulacao.csv'; a.click(); URL.revokeObjectURL(url) } catch(e) { showError(e) } }
onMounted(async () => { try { genres.value = items(await request('/genres')) } catch {} try { const data = await restoreSession(); user.value = data.usuario } catch { setToken(null) } await load() })
</script>

<template>
  <div class="app-shell">
    <aside :class="['sidebar', { 'sidebar-open': menu }]">
      <div class="brand"><div class="brand-mark">▤</div><div><strong>Estante Livre</strong><span>Biblioteca Comunitária</span></div></div>
      <div class="demo-label">NAVEGAÇÃO</div>
      <nav aria-label="Navegação principal"><button v-for="item in visibleNav" :key="item.id" :class="{active:page === item.id}" @click="navigate(item.id)"><span class="nav-icon">{{ item.icon }}</span><span>{{ item.label }}</span></button></nav>
      <div class="sidebar-foot"><div class="community-card"><span class="leaf">EL</span><div><strong>Uma biblioteca viva</strong><p>Leitura que aproxima a comunidade.</p></div></div></div>
    </aside>
    <button v-if="menu" class="mobile-overlay" aria-label="Fechar menu" @click="menu = false"></button>
    <main class="main">
      <header class="topbar"><div class="top-left"><button class="menu-btn" aria-label="Abrir menu" @click="menu = true">☰</button><div><span class="breadcrumb">Estante Livre / </span><strong>{{ title }}</strong></div></div><div class="top-actions"><template v-if="user"><span class="user-name">{{ user.nome }}</span><button class="btn btn-outline" @click="logout">Sair</button></template><button v-else class="btn btn-dark" @click="loginOpen = true; error = ''">Entrar</button></div></header>
      <div class="content">
        <div v-if="error" class="alert" role="alert">{{ error }} <button @click="error = ''" aria-label="Fechar aviso">×</button></div>
        <div class="page-head"><div><span class="eyebrow">BIBLIOTECA COMUNITÁRIA</span><h1>{{ title }}</h1><p>{{ {catalogo:'Histórias que conectam pessoas. Explore nosso acervo.',acervo:'Gerencie os livros e exemplares da biblioteca.',leitores:'Acompanhe as pessoas da nossa comunidade.',circulacao:'Acompanhe empréstimos e devoluções.',meus:'Acompanhe suas leituras e prazos.',relatorios:'Uma visão da circulação da biblioteca.',equipe:'Pessoas que cuidam da biblioteca.',config:'Parâmetros de funcionamento da biblioteca.',auditoria:'Histórico de ações do sistema.',sessoes:'Gerencie seus acessos ativos.'}[page] }}</p></div><button v-if="page === 'acervo' && can('book_create')" class="btn btn-primary" @click="openForm('bookForm')">＋ Novo livro</button><button v-if="page === 'leitores' && can('reader_create')" class="btn btn-primary" @click="openForm('readerForm')">＋ Novo leitor</button><button v-if="page === 'circulacao' && can('loan_create')" class="btn btn-primary" @click="openForm('loanForm')">＋ Novo empréstimo</button><button v-if="page === 'equipe'" class="btn btn-primary" @click="openForm('userForm')">＋ Novo usuário</button><button v-if="page === 'relatorios'" class="btn btn-outline" @click="downloadReport">Exportar CSV</button></div>
        <p v-if="busy" class="loading" role="status">Carregando...</p>
        <template v-if="page === 'catalogo' || page === 'acervo'">
          <form class="catalog-tools" @submit.prevent="load()"><select v-model="filters.field" aria-label="Campo de busca"><option value="titulo">Título</option><option value="autor">Autor</option><option v-if="page === 'acervo'" value="isbn">ISBN</option></select><div class="searchbox"><span>⌕</span><input v-model="search" aria-label="Buscar por título" placeholder="Busque pelo título do livro..."><button class="btn btn-primary">Buscar</button></div><select v-model="filters.genre" aria-label="Filtrar por gênero" @change="load()"><option value="">Todos os gêneros</option><option v-for="g in genres" :key="g.id" :value="g.id">{{ g.nome }}</option></select><select v-if="page === 'acervo'" v-model="filters.status" aria-label="Disponibilidade" @change="load()"><option value="">Todos os exemplares</option><option value="DISPONIVEL">Disponíveis</option><option value="INDISPONIVEL">Indisponíveis</option><option value="EMPRESTADO">Emprestados</option></select><button v-if="can('genre_manage')" type="button" class="btn btn-outline" @click="extra = 'genres'">Gêneros</button><label v-if="page === 'catalogo'" class="check-filter"><input v-model="available" type="checkbox" @change="load()"> Apenas disponíveis</label></form>
          <div class="result-line"><strong>{{ books.length }} {{ books.length === 1 ? 'livro encontrado' : 'livros encontrados' }}</strong><span>Explore novas histórias</span></div>
          <div class="book-grid"><button v-for="(book,i) in books" :key="book.id" class="book-card book-button" @click="openBook(book)"><div class="book-cover" :style="{'--cover': colors[i % colors.length]}"><span>ESTANTE LIVRE</span><strong>{{ book.titulo }}</strong><small>{{ book.autor }}</small></div><div class="book-info"><div class="genre-row"><span v-for="genre in genreNames(book)" :key="genre">{{ genre }}</span></div><h3>{{ book.titulo }}</h3><p class="author">{{ book.autor }}</p><div class="meta">{{ book.editora || 'Editora não informada' }} <span v-if="book.anoPublicacao"> · {{ book.anoPublicacao }}</span></div><div class="availability"><span :class="['status-dot', {unavailable: !book.exemplaresDisponiveis}]"></span>{{ book.exemplaresDisponiveis }} {{ book.exemplaresDisponiveis === 1 ? 'exemplar disponível' : 'exemplares disponíveis' }} <b>↗</b></div></div></button></div>
          <div v-if="!busy && !books.length && !error" class="empty">Nenhum livro encontrado.</div><div v-if="pagination.totalPages > 1" class="pager"><button class="btn btn-outline" :disabled="pagination.page === 0" @click="load(page,pagination.page-1)">Anterior</button><span>Página {{ pagination.page + 1 }} de {{ pagination.totalPages }}</span><button class="btn btn-outline" :disabled="pagination.page + 1 >= pagination.totalPages" @click="load(page,pagination.page+1)">Próxima</button></div>
          <div v-if="page === 'catalogo' && !user" class="visitor-note">✦ &nbsp; Faça login para acompanhar seus empréstimos e prazos.</div>
        </template>
        <div v-if="page === 'leitores'" class="panel table-wrap"><table><thead><tr><th>Leitor</th><th>Documento</th><th>Contato</th><th>Situação</th><th>Ações</th></tr></thead><tbody><tr v-for="r in readers" :key="r.id"><td><strong>{{ r.nome }}</strong></td><td>{{ r.documento }}</td><td>{{ r.email || '—' }}<br>{{ r.telefone }}</td><td>{{ r.suspenso ? 'Suspenso até ' + date(r.suspensoAte) : 'Ativo' }}</td><td><button v-if="can('reader_update')" class="text-btn" @click="openForm('readerForm',r)">Editar</button> <button v-if="can('loan_read')" class="text-btn" @click="extraData = r; extra = 'history'">Histórico</button> <button v-if="can('reader_update')" class="text-btn" @click="extraData = r; extra = 'access'">Acesso</button> <button v-if="can('reader_delete')" class="text-btn" @click="action(`/readers/${r.id}`,'DELETE',undefined,'Leitor inativado.')">Inativar</button></td></tr></tbody></table><p v-if="!readers.length && !busy" class="empty">Nenhum leitor encontrado.</p></div>
        <label v-if="page === 'circulacao' || page === 'meus'" class="loan-filter">Situação <select v-model="loanStatus" @change="load()"><option value="">Todas</option><option value="ATIVO">Ativos</option><option value="ATRASADO">Atrasados</option><option value="DEVOLVIDO">Devolvidos</option></select></label><div v-if="page === 'circulacao' || page === 'meus'" class="panel table-wrap"><table><thead><tr><th>Livro / exemplar</th><th v-if="page === 'circulacao'">Leitor</th><th>Devolução prevista</th><th>Situação</th><th>Ações</th></tr></thead><tbody><template v-for="loan in loans" :key="loan.id"><tr v-for="item in loan.itens" :key="item.id"><td><strong>{{ item.tituloLivro }}</strong><br>{{ item.codigoExemplar }}</td><td v-if="page === 'circulacao'">{{ loan.leitor.nome }}</td><td>{{ date(item.dataDevolucaoPrevista) }}</td><td>{{ item.situacao }}</td><td><template v-if="!item.dataDevolucaoReal"><button v-if="can('loan_renew') || can('loan_renew_own')" class="text-btn" @click="action(`/loans/items/${item.id}/renew`,'POST',undefined,'Empréstimo renovado.')">Renovar</button> <button v-if="can('loan_return')" class="text-btn" @click="selected = {...item, loanId: loan.id}; form = {condicao:'CONSERVADO'}; modal = 'return'">Devolver</button></template></td></tr></template></tbody></table><p v-if="!loans.length && !busy" class="empty">Nenhum empréstimo encontrado.</p></div>
        <button v-if="page === 'relatorios'" class="btn btn-outline extra-action" @click="extra = 'reports'">Relatórios detalhados</button><div v-if="page === 'relatorios' && summary" class="kpi-grid"><div v-for="(value,key) in summary" :key="key" class="kpi"><span>{{ key.replace(/([A-Z])/g, ' $1') }}</span><strong>{{ value }}</strong></div></div>
        <button v-if="page === 'equipe' && can('role_manage')" class="btn btn-outline extra-action" @click="extra = 'roles'">Perfis e permissões</button><div v-if="page === 'equipe'" class="panel table-wrap"><table><thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th></tr></thead><tbody><tr v-for="u in users" :key="u.id"><td>{{ u.nome }}</td><td>{{ u.email }}</td><td>{{ u.perfil }}</td><td><button class="text-btn" @click="extraData = u; extra = 'user'">Editar</button> <button v-if="can('session_manage')" class="text-btn" @click="openUserSessions(u)">Sessões</button></td></tr></tbody></table></div>
        <div v-if="page === 'sessoes'" class="panel table-wrap"><table><thead><tr><th>Dispositivo</th><th>IP</th><th>Último acesso</th><th></th></tr></thead><tbody><tr v-for="s in sessions" :key="s.id"><td>{{ s.userAgent }} <strong v-if="s.atual">(atual)</strong></td><td>{{ s.ip }}</td><td>{{ date(s.ultimoAcesso) }}</td><td><button class="text-btn" @click="endSession(s)">Encerrar</button></td></tr></tbody></table></div>
        <div v-if="page === 'config'" class="panel settings-list"><div v-for="s in settings" :key="s.chave" class="setting-row"><label :for="s.chave"><strong>{{ s.chave }}</strong><small>{{ s.descricao }}</small></label><input :id="s.chave" v-model="s.valor"><button class="btn btn-outline" @click="action(`/settings/${encodeURIComponent(s.chave)}`,'PUT',{valor:s.valor},'Configuração salva.')">Salvar</button></div></div>
        <div v-if="['leitores','circulacao','meus','equipe','auditoria'].includes(page) && pagination.totalPages > 1" class="pager"><button class="btn btn-outline" :disabled="pagination.page === 0" @click="load(page,pagination.page-1)">Anterior</button><span>Página {{ pagination.page + 1 }} de {{ pagination.totalPages }}</span><button class="btn btn-outline" :disabled="pagination.page + 1 >= pagination.totalPages" @click="load(page,pagination.page+1)">Próxima</button></div><div v-if="page === 'auditoria'" class="panel table-wrap"><table><thead><tr><th>Data</th><th>Usuário</th><th>Ação</th><th>Entidade</th><th>IP</th></tr></thead><tbody><tr v-for="a in audit" :key="a.id"><td>{{ date(a.dataHora) }}</td><td>{{ a.usuario?.nome || '—' }}</td><td>{{ a.acao }}</td><td>{{ a.entidade || '—' }}</td><td>{{ a.ip || '—' }}</td></tr></tbody></table></div>
      </div>
    </main>
    <Extras v-if="extra" :kind="extra" :data="extraData" @close="extra = ''; extraData = null" @changed="load(); if (modal === 'book') openBook(selected); request('/genres').then(v => genres = v)" />
    <div v-if="loginOpen || modal" class="modal-backdrop" @click.self="loginOpen = false; modal = ''; error = ''"><section class="modal" role="dialog" aria-modal="true" :aria-label="loginOpen ? 'Entrar' : 'Detalhes'"><div class="modal-head"><div><span class="eyebrow">BIBLIOTECA COMUNITÁRIA</span><h2>{{ loginOpen ? 'Bem-vindo de volta' : ({book:'Detalhes do livro',bookForm:form.id ? 'Editar livro' : 'Cadastrar livro',readerForm:form.id ? 'Editar leitor' : 'Cadastrar leitor',loanForm:'Novo empréstimo',return:'Registrar devolução',userForm:'Cadastrar usuário'})[modal] }}</h2></div><button class="icon-btn" aria-label="Fechar" @click="loginOpen = false; modal = ''; error = ''">×</button></div>
      <div v-if="error" class="alert" role="alert">{{ error }}</div>
      <form v-if="loginOpen" class="form-stack" @submit.prevent="login"><p>Entre para acessar sua conta.</p><label>E-mail<input v-model="credentials.email" type="email" autocomplete="username" required></label><label>Senha<input v-model="credentials.senha" type="password" autocomplete="current-password" required></label><button class="btn btn-primary" :disabled="busy">Entrar</button></form>
      <div v-else-if="modal === 'book' && selected" class="detail-body"><h2>{{ selected.titulo }}</h2><p>{{ selected.autor }} · {{ selected.editora || 'Editora não informada' }} <span v-if="selected.anoPublicacao">· {{ selected.anoPublicacao }}</span></p><p>{{ genreNames(selected).join(' · ') }}</p><p><strong>{{ selected.exemplaresDisponiveis }}</strong> exemplar(es) disponível(is)</p><div v-if="can('copy_read')"><h3>Exemplares</h3><ul class="copy-list"><li v-for="copy in copies" :key="copy.id">{{ copy.codigo }} <span>{{ copy.condicao }} · {{ copy.status }} <button v-if="can('copy_manage')" class="text-btn" @click="extraData = copy; extra = 'copy'">Editar</button></span></li></ul><button v-if="can('copy_manage')" class="btn btn-outline" @click="addCopies">＋ Adicionar exemplar</button></div><div v-if="can('book_update')" class="modal-actions"><button class="btn btn-primary" @click="openForm('bookForm',selected)">Editar livro</button><button v-if="can('book_delete')" class="btn btn-outline" @click="action(`/books/${selected.id}`,'DELETE',undefined,'Livro inativado.'); modal = ''">Inativar</button></div></div>
      <LoanForm v-else-if="modal === 'loanForm'" @saved="loanSaved" @cancel="modal = ''" />
      <form v-else class="form-stack" @submit.prevent="submitForm">
        <template v-if="modal === 'bookForm'"><label>Título<input v-model="form.titulo" required></label><label>Autor<input v-model="form.autor" required></label><div class="form-grid"><label>Editora<input v-model="form.editora"></label><label>Ano de publicação<input v-model="form.anoPublicacao" type="number" min="1"></label></div><label>ISBN<input v-model="form.isbn"></label><fieldset><legend>Gêneros (selecione ao menos um)</legend><label v-for="g in genres" :key="g.id" class="inline-check"><input v-model="form.generoIds" type="checkbox" :value="g.id"> {{ g.nome }}</label><p v-if="!genres.length">Nenhum gênero cadastrado.</p></fieldset></template>
        <template v-if="modal === 'readerForm'"><label>Nome<input v-model="form.nome" required></label><div class="form-grid"><label>Tipo de documento<select v-model="form.tipoDocumento"><option>CPF</option><option>RG</option></select></label><label>Documento<input v-model="form.documento" required></label></div><label>E-mail<input v-model="form.email" type="email"></label><label>Telefone<input v-model="form.telefone"></label><label>Endereço<input v-model="form.endereco"></label></template>
        <template v-if="modal === 'return'"><p>{{ selected.tituloLivro }} · {{ selected.codigoExemplar }}</p><label>Condição na devolução<select v-model="form.condicao" required><option>NOVO</option><option>EXCELENTE</option><option>CONSERVADO</option><option>ACEITAVEL</option><option>MAL_CONSERVADO</option><option>DANIFICADO</option></select></label></template>
        <template v-if="modal === 'userForm'"><label>Nome<input v-model="form.nome" required></label><label>E-mail<input v-model="form.email" type="email" required></label><label>Senha<input v-model="form.senha" type="password" minlength="8" required></label><label>Telefone<input v-model="form.telefone"></label><label>Perfil<select v-model="form.perfil" required><option value="FUNCIONARIO">Funcionário</option><option value="ADMIN">Administrador</option></select></label></template>
        <div class="modal-actions"><button type="button" class="btn btn-ghost" @click="modal = ''; error = ''">Cancelar</button><button class="btn btn-primary">Salvar</button></div>
      </form>
    </section></div>
    <div v-if="notice" class="toast-stack" role="status"><div class="toast">{{ notice }}</div></div>
  </div>
</template>
