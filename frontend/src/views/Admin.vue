<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const activeTab = ref('users')
const users = ref([])
const orders = ref([])
const stations = ref([])
const trains = ref([])
const tickets = ref([])

const stationDialog = ref(false)
const stationForm = ref({ id: null, name: '', city: '', code: '' })
const trainDialog = ref(false)
const trainForm = ref({
  id: null,
  trainNumber: '',
  trainType: '高铁',
  fromStationId: null,
  toStationId: null,
  startTime: '',
  endTime: '',
  duration: null,
  businessSeatPrice: 0,
  firstSeatPrice: 0,
  secondSeatPrice: 0,
  sleeperPrice: 0
})
const ticketDialog = ref(false)
const ticketForm = ref({ trainId: null, trainDate: '', seatType: '二等座', delta: 1 })

const seatTypes = ['商务座', '一等座', '二等座', '硬卧']

async function loadAll() {
  const [u, o, s, t, k] = await Promise.all([
    request.get('/admin/users'),
    request.get('/admin/orders'),
    request.get('/admin/stations'),
    request.get('/admin/trains'),
    request.get('/admin/tickets')
  ])
  users.value = u || []
  orders.value = o || []
  stations.value = s || []
  trains.value = t || []
  tickets.value = k || []
}

function trainNumber(id) {
  return trains.value.find((t) => t.id === id)?.trainNumber || id
}

async function changeRole(row, role) {
  await request.put(`/admin/users/${row.id}/role`, { role })
  ElMessage.success(`已将 ${row.username} 设为${role === 'ADMIN' ? '管理员' : '普通用户'}`)
  await loadAll()
}

function openStation(row) {
  stationForm.value = row ? { ...row } : { id: null, name: '', city: '', code: '' }
  stationDialog.value = true
}

async function saveStation() {
  const form = stationForm.value
  await request[form.id ? 'put' : 'post'](form.id ? `/admin/stations/${form.id}` : '/admin/stations', form)
  stationDialog.value = false
  ElMessage.success('站点已保存')
  await loadAll()
}

async function removeStation(row) {
  await ElMessageBox.confirm(`确认删除站点「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/admin/stations/${row.id}`)
  ElMessage.success('站点已删除')
  await loadAll()
}

function openTrain(row) {
  trainForm.value = row
    ? { ...row }
    : {
        id: null,
        trainNumber: '',
        trainType: '高铁',
        fromStationId: null,
        toStationId: null,
        startTime: '',
        endTime: '',
        duration: null,
        businessSeatPrice: 0,
        firstSeatPrice: 0,
        secondSeatPrice: 0,
        sleeperPrice: 0
      }
  trainDialog.value = true
}

async function saveTrain() {
  const form = trainForm.value
  await request[form.id ? 'put' : 'post'](form.id ? `/admin/trains/${form.id}` : '/admin/trains', form)
  trainDialog.value = false
  ElMessage.success('车次已保存')
  await loadAll()
}

async function removeTrain(row) {
  await ElMessageBox.confirm(`确认删除车次「${row.trainNumber}」？`, '提示', { type: 'warning' })
  await request.delete(`/admin/trains/${row.id}`)
  ElMessage.success('车次已删除')
  await loadAll()
}

function openTicket(row) {
  ticketForm.value = {
    trainId: row.trainId,
    trainDate: typeof row.trainDate === 'string' ? row.trainDate : row.trainDate.slice(0, 10),
    seatType: '二等座',
    delta: 1
  }
  ticketDialog.value = true
}

async function saveTicket() {
  await request.post('/admin/tickets/adjust', ticketForm.value)
  ticketDialog.value = false
  ElMessage.success('余票已调整')
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div class="admin-page">
    <section class="admin-hero glass-card">
      <div class="admin-hero-text">
        <div class="admin-kicker">ADMIN CONSOLE</div>
        <h1>管理后台</h1>
        <p>维护用户角色、订单、站点、车次与余票。所有接口均需管理员令牌。</p>
      </div>
      <el-button type="primary" round @click="loadAll">刷新数据</el-button>
    </section>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" stripe max-height="520">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="role" label="角色" width="120" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button v-if="row.role !== 'ADMIN'" link type="primary" @click="changeRole(row, 'ADMIN')">
                设为管理员
              </el-button>
              <el-button v-else link type="warning" @click="changeRole(row, 'USER')">降为用户</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单管理" name="orders">
        <el-table :data="orders" stripe max-height="520">
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="trainNumber" label="车次" />
          <el-table-column prop="trainDate" label="日期" />
          <el-table-column prop="seatType" label="席别" />
          <el-table-column prop="seatNo" label="座位" />
          <el-table-column prop="price" label="票价" />
          <el-table-column prop="status" label="状态" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="站点管理" name="stations">
        <div class="admin-toolbar">
          <el-button type="primary" @click="openStation(null)">新增站点</el-button>
        </div>
        <el-table :data="stations" stripe max-height="480">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="站名" />
          <el-table-column prop="city" label="城市" />
          <el-table-column prop="code" label="代码" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openStation(row)">编辑</el-button>
              <el-button link type="danger" @click="removeStation(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="车次管理" name="trains">
        <div class="admin-toolbar">
          <el-button type="primary" @click="openTrain(null)">新增车次</el-button>
        </div>
        <el-table :data="trains" stripe max-height="480">
          <el-table-column prop="trainNumber" label="车次" />
          <el-table-column prop="trainType" label="类型" />
          <el-table-column label="起终点">
            <template #default="{ row }"> {{ row.fromStationId }} → {{ row.toStationId }} </template>
          </el-table-column>
          <el-table-column prop="startTime" label="发车" />
          <el-table-column prop="endTime" label="到达" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTrain(row)">编辑</el-button>
              <el-button link type="danger" @click="removeTrain(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="余票管理" name="tickets">
        <el-table :data="tickets" stripe max-height="520">
          <el-table-column label="车次">
            <template #default="{ row }">{{ trainNumber(row.trainId) }}</template>
          </el-table-column>
          <el-table-column prop="trainDate" label="日期" />
          <el-table-column prop="businessSeatCount" label="商务座" />
          <el-table-column prop="firstSeatCount" label="一等座" />
          <el-table-column prop="secondSeatCount" label="二等座" />
          <el-table-column prop="sleeperCount" label="硬卧" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTicket(row)">调整</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="stationDialog" :title="stationForm.id ? '编辑站点' : '新增站点'" width="480">
      <el-form :model="stationForm" label-width="80px">
        <el-form-item label="站名"><el-input v-model="stationForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="stationForm.city" /></el-form-item>
        <el-form-item label="代码"><el-input v-model="stationForm.code" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stationDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="trainDialog" :title="trainForm.id ? '编辑车次' : '新增车次'" width="620">
      <el-form :model="trainForm" label-width="90px">
        <el-form-item label="车次号"><el-input v-model="trainForm.trainNumber" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="trainForm.trainType" /></el-form-item>
        <el-form-item label="起点站">
          <el-select v-model="trainForm.fromStationId" placeholder="选择站点">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="终点站">
          <el-select v-model="trainForm.toStationId" placeholder="选择站点">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发车/到达">
          <el-input v-model="trainForm.startTime" placeholder="HH:mm" class="time-input" />
          <el-input v-model="trainForm.endTime" placeholder="HH:mm" class="time-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trainDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTrain">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketDialog" title="调整余票" width="460">
      <el-form :model="ticketForm" label-width="90px">
        <el-form-item label="席别">
          <el-select v-model="ticketForm.seatType">
            <el-option v-for="s in seatTypes" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="增减">
          <el-input-number v-model="ticketForm.delta" :min="-100" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ticketDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTicket">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 20px 80px;
}
.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 32px;
  margin-bottom: 20px;
}
.admin-kicker {
  font-size: 12px;
  letter-spacing: 3px;
  color: #60a5fa;
  font-weight: 700;
}
.admin-hero h1 {
  margin: 8px 0 6px;
  font-size: 30px;
}
.admin-hero p {
  margin: 0;
  color: #64748b;
}
.admin-tabs {
  padding: 20px;
}
.admin-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.time-input {
  width: 140px;
  margin-right: 10px;
}
</style>
