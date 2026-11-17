<template>
  <div class="home">
    <AppNav />

    <section class="hero">
      <div class="hero-bg-blob blob-1"></div>
      <div class="hero-bg-blob blob-2"></div>

      <div class="hero-inner">
        <h1 class="hero-title">
          <span class="gradient-text">畅游全国</span>
          <span>，一张车票的距离</span>
        </h1>
        <p class="hero-sub">实时余票查询 · 闪电购票 · 全程无忧</p>

        <div class="search-box glass-card">
          <div class="search-row">
            <div class="station-group">
              <div class="station-col">
                <div class="station-label">出发</div>
                <el-autocomplete
                  v-model="queryForm.fromStation"
                  :fetch-suggestions="fetchFromSuggestions"
                  placeholder="输入站点名称或编码"
                  clearable
                  class="station-input"
                  :trigger-on-focus="true"
                  highlight-first-item
                  value-key="name"
                >
                  <template #default="{ item }">
                    <div class="ac-item">
                      <span class="ac-name">{{ item.name }}</span>
                      <span v-if="item.code" class="ac-code">{{ item.code }}</span>
                    </div>
                  </template>
                </el-autocomplete>
              </div>
              <div v-ripple class="swap-btn" @click="swapStations">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M7 16V4m0 0L3 8m4-4l4 4M17 8v12m0 0l4-4m-4 4l-4-4" />
                </svg>
              </div>
              <div class="station-col">
                <div class="station-label">到达</div>
                <el-autocomplete
                  v-model="queryForm.toStation"
                  :fetch-suggestions="fetchToSuggestions"
                  placeholder="输入站点名称或编码"
                  clearable
                  class="station-input"
                  :trigger-on-focus="true"
                  highlight-first-item
                  value-key="name"
                >
                  <template #default="{ item }">
                    <div class="ac-item">
                      <span class="ac-name">{{ item.name }}</span>
                      <span v-if="item.code" class="ac-code">{{ item.code }}</span>
                    </div>
                  </template>
                </el-autocomplete>
              </div>
            </div>
            <div class="date-col">
              <div class="station-label">出发日期</div>
              <el-date-picker
                v-model="queryForm.date"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                class="date-input"
                :disabled-date="beforeToday"
              />
            </div>
            <div class="date-shortcuts">
              <button
                v-for="d in dateShortcuts"
                :key="d.label"
                class="date-shortcut"
                :class="{ active: queryForm.date === d.value }"
                @click="queryForm.date = d.value"
              >
                <span class="ds-day">{{ d.label }}</span>
                <span class="ds-date">{{ d.value.slice(5) }}</span>
              </button>
            </div>
            <button v-ripple class="search-btn" :disabled="searching" @click="search">
              <span>{{ searching ? '查询中...' : '搜索车票' }}</span>
            </button>
          </div>

          <div class="quick-tags">
            <span class="quick-label">热门路线</span>
            <button
              v-for="quick in quickRoutes"
              :key="quick.label"
              class="quick-tag"
              :class="{ active: quickActive === quick.label }"
              @click="applyQuick(quick)"
            >
              {{ quick.label }}
            </button>
            <button
              v-if="queryForm.fromStation && queryForm.toStation"
              class="quick-tag fav-toggle"
              :class="{ active: isFavRoute(queryForm.fromStation, queryForm.toStation) }"
              :title="
                isFavRoute(queryForm.fromStation, queryForm.toStation) ? '取消收藏这条路线' : '收藏这条路线'
              "
              @click="toggleCurrentRouteFav"
            >
              <svg
                width="12"
                height="12"
                viewBox="0 0 24 24"
                :fill="isFavRoute(queryForm.fromStation, queryForm.toStation) ? 'currentColor' : 'none'"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polygon
                  points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
                />
              </svg>
              {{ isFavRoute(queryForm.fromStation, queryForm.toStation) ? '已收藏' : '收藏' }}
            </button>
          </div>

          <div v-if="favRoutes.length" class="quick-tags">
            <span class="quick-label">⭐ 我的收藏</span>
            <div class="fav-routes">
              <div v-for="fav in favRoutes" :key="fav.label" class="fav-route-card">
                <div class="fav-route-main" @click="searchWithFavDate(fav)">
                  <span class="fav-from">{{ fav.from }}</span>
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                  >
                    <path d="M5 12h14M12 5l7 7-7 7" />
                  </svg>
                  <span class="fav-to">{{ fav.to }}</span>
                </div>
                <div class="fav-route-quick">
                  <button title="今天" @click="searchWithFavDate(fav, 0)">今</button>
                  <button title="明天" @click="searchWithFavDate(fav, 1)">明</button>
                  <button title="3天后" @click="searchWithFavDate(fav, 3)">3天</button>
                </div>
                <button class="fav-remove" title="取消收藏" @click="toggleFavRoute(fav.from, fav.to)">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                    <polygon
                      points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div v-if="historyList.length" class="quick-tags">
            <span class="quick-label">搜索历史</span>
            <div v-for="(h, i) in historyList" :key="h.key" class="history-wrap">
              <button class="quick-tag history-tag" @click="applyHistory(h)">
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                <span>{{ h.from }}→{{ h.to }}</span>
                <span v-if="h.date" class="history-date">{{ h.date.slice(5) }}</span>
                <span v-if="h.ts" class="history-time">{{ formatHistoryTime(h.ts) }}</span>
              </button>
              <button class="history-del" title="删除" @click.stop="deleteHistoryItem(i)">
                <svg
                  width="10"
                  height="10"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="3"
                  stroke-linecap="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <button class="quick-tag clear-btn" @click="clearHistory">清空</button>
          </div>
        </div>

        <div v-if="!searched" class="stats-row">
          <div class="stat-item">
            <div class="stat-ico ico-blue">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-num">{{ stats.stations }}</span>
              <span class="stat-cap">覆盖站点</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-ico ico-purple">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="4" y="3" width="16" height="16" rx="2" />
                <path d="M4 11h16M8 7v4" />
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-num">{{ stats.trains }}</span>
              <span class="stat-cap">运营车次</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-ico ico-pink">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <path d="M16 2v4M8 2v4M3 10h18" />
              </svg>
            </div>
            <div class="stat-body">
              <span class="stat-num">7</span>
              <span class="stat-cap">预售天数</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="searched" class="results">
      <div class="results-header">
        <h2>共 {{ displayResults.length }} 趟列车</h2>
        <div class="date-badge">{{ queryForm.date }}</div>
      </div>

      <div v-if="!searching && results.length" class="filter-bar">
        <div class="filter-group">
          <span class="filter-label">排序</span>
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            class="filter-btn"
            :class="{ active: sortBy === opt.value }"
            @click="sortBy = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-label">车型</span>
          <button
            v-for="opt in trainTypeOptions"
            :key="opt.value"
            class="filter-btn"
            :class="{ active: trainTypeFilter === opt.value }"
            @click="trainTypeFilter = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-label">时段</span>
          <button
            v-for="opt in timeOptions"
            :key="opt.value"
            class="filter-btn"
            :class="{ active: timeFilter === opt.value }"
            @click="timeFilter = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="filter-group">
          <button
            class="filter-btn"
            :class="{ active: onlyAvailable }"
            @click="onlyAvailable = !onlyAvailable"
          >
            <svg
              v-if="onlyAvailable"
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
            只看有票
          </button>
        </div>
      </div>

      <div v-if="searching" class="skeleton-list">
        <div v-for="i in 4" :key="i" class="skeleton-card glass-card">
          <div class="sk-line sk-lg"></div>
          <div class="sk-line sk-sm"></div>
          <div class="sk-line sk-md"></div>
          <div class="sk-chips">
            <div class="sk-chip"></div>
            <div class="sk-chip"></div>
            <div class="sk-chip"></div>
          </div>
        </div>
      </div>

      <TransitionGroup v-else name="list" tag="div" class="train-list stagger-children">
        <div
          v-for="(train, idx) in displayResults"
          :key="train.trainNumber + train.startTime"
          class="train-card glass-card"
          :class="{ favorited: isFav(train) }"
          :style="{ animationDelay: idx * 0.04 + 's' }"
        >
          <div class="shimmer-overlay"></div>
          <button
            class="fav-btn"
            :class="{ active: isFav(train) }"
            :title="isFav(train) ? '取消收藏' : '收藏此车次'"
            @click.stop="toggleFav(train)"
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polygon
                points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
              />
            </svg>
          </button>
          <div class="train-main">
            <div class="train-info">
              <div class="train-num-wrap">
                <span class="train-num">{{ train.trainNumber }}</span>
                <span class="train-type-badge">{{ train.seatTypes }}</span>
                <span v-if="getCountdown(train)" class="countdown-tag" :class="getCountdown(train).status">
                  {{ getCountdown(train).text }}
                </span>
              </div>
              <div class="route-line">
                <div class="station-time">
                  <div class="time-big">
                    {{ train.startTime.split('T')[1]?.substring(0, 5) || train.startTime }}
                  </div>
                  <div class="station-name">{{ train.fromStation }}</div>
                </div>
                <div class="journey">
                  <div class="duration">{{ formatDuration(train.duration) }}</div>
                  <div class="track">
                    <div class="track-line"></div>
                    <div class="track-dot start pulse-dot"></div>
                    <div class="track-dot end"></div>
                  </div>
                </div>
                <div class="station-time right">
                  <div class="time-big">
                    {{ train.endTime.split('T')[1]?.substring(0, 5) || train.endTime }}
                  </div>
                  <div class="station-name">{{ train.toStation }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="train-seats">
            <div
              v-for="seat in getSeatOptions(train)"
              :key="seat.type"
              class="seat-chip"
              :class="[getSeatLevel(seat.count), { sold: seat.count === 0 }]"
            >
              <div class="seat-top">
                <span class="seat-label">{{ seat.label.split('(')[0] }}</span>
                <span class="seat-price">¥{{ seat.price }}</span>
              </div>
              <div class="seat-bottom">
                <span v-if="seat.count > 0" class="seat-count"
                  >{{ seat.count >= 10 ? '充足' : seat.count <= 3 ? '仅剩' : '' }}
                  {{ seat.count > 3 ? '' : seat.count + '张' }}</span
                >
                <span v-else class="seat-count sold-out">无票</span>
              </div>
              <div class="seat-bar">
                <div class="seat-bar-fill" :style="{ width: Math.min(100, seat.count * 4) + '%' }"></div>
              </div>
            </div>
          </div>

          <div class="train-action">
            <button class="detail-btn" title="经停站 · 详情" @click="openTrainDetail(train)">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 3v18h18" />
                <path d="M7 14l4-4 4 4 6-6" />
              </svg>
            </button>
            <button
              v-ripple
              class="buy-btn"
              :disabled="buyingRowId === train.trainNumber || !hasAnySeat(train)"
              @click="buyTicket(train)"
            >
              {{ buyingRowId === train.trainNumber ? '购票中...' : '立即购票' }}
            </button>
          </div>
        </div>
      </TransitionGroup>

      <div v-if="!searching && !displayResults.length" class="empty-state glass-card">
        <svg class="empty-train-svg" viewBox="0 0 120 80" fill="none">
          <defs>
            <linearGradient id="et-g" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.8" />
              <stop offset="100%" stop-color="#8b5cf6" stop-opacity="0.6" />
            </linearGradient>
          </defs>
          <line
            class="et-track"
            x1="0"
            y1="66"
            x2="120"
            y2="66"
            stroke="#334155"
            stroke-width="2"
            stroke-dasharray="6 4"
          />
          <g class="et-cloud et-c1">
            <ellipse cx="25" cy="14" rx="18" ry="8" fill="#475569" opacity="0.4" />
          </g>
          <g class="et-cloud et-c2">
            <ellipse cx="95" cy="10" rx="14" ry="6" fill="#475569" opacity="0.35" />
          </g>
          <g class="et-body">
            <rect x="18" y="24" width="72" height="34" rx="6" fill="url(#et-g)" class="et-fill" />
            <rect x="26" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window" />
            <rect x="48" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window" />
            <rect x="70" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window" />
            <rect x="30" y="48" width="48" height="3" rx="1.5" fill="#0f172a" opacity="0.4" />
          </g>
          <circle cx="34" cy="62" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="1.8" class="et-wheel" />
          <circle cx="74" cy="62" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="1.8" class="et-wheel" />
          <line
            class="et-smoke et-s1"
            x1="92"
            y1="30"
            x2="100"
            y2="18"
            stroke="#94a3b8"
            stroke-width="2"
            stroke-linecap="round"
            opacity="0.5"
          />
          <line
            class="et-smoke et-s2"
            x1="94"
            y1="32"
            x2="106"
            y2="22"
            stroke="#94a3b8"
            stroke-width="1.5"
            stroke-linecap="round"
            opacity="0.35"
          />
        </svg>
        <h3>{{ onlyAvailable && results.length ? '筛选后无符合条件的车次' : '没有找到合适的车次' }}</h3>
        <p>
          {{ onlyAvailable && results.length ? '试试取消"只看有票"筛选' : '试试调整日期或更换出发/到达地' }}
        </p>
        <button v-if="onlyAvailable" class="go-home-btn" @click="onlyAvailable = false">取消筛选</button>
      </div>

      <div v-if="!searching && results.length <= 3" class="hot-routes-wrap glass-card">
        <div class="hr-title">
          <span class="hr-icon">🔥</span>
          <span>热门路线 · 一键查票</span>
        </div>
        <div class="hr-grid">
          <button
            v-for="(r, i) in hotRoutes"
            :key="i"
            class="hr-card"
            :style="{ animationDelay: i * 0.05 + 's' }"
            @click="quickQueryHot(r)"
          >
            <div class="hr-from">{{ r.from }}</div>
            <div class="hr-arrow">
              <div class="hr-arrow-line"></div>
              <span class="hr-dur">{{ r.dur }}</span>
            </div>
            <div class="hr-to">{{ r.to }}</div>
            <div class="hr-rail">{{ r.train }}</div>
          </button>
        </div>
      </div>
    </section>

    <Fireworks v-if="showFireworks" :show="showFireworks" @done="showFireworks = false" />

    <Teleport to="body">
      <Transition name="payfade">
        <div v-if="trainDetailVisible" class="td-overlay" @click.self="trainDetailVisible = false">
          <div class="td-card">
            <div class="td-header">
              <div class="td-title">
                <span class="td-train-num">{{ trainDetail?.trainNumber }}</span>
                <span class="td-type">{{ trainDetail?.seatTypes || '高铁' }}</span>
                <span class="td-date">📅 {{ queryForm.date }}</span>
              </div>
              <button class="td-close" @click="trainDetailVisible = false">
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <div class="td-route-summary">
              <div class="td-big-station">
                <span class="td-big-time">{{
                  trainDetail?.startTime?.split('T')[1]?.substring(0, 5) || '--:--'
                }}</span>
                <span class="td-big-name">{{ trainDetail?.fromStation }}</span>
              </div>
              <div class="td-big-track">
                <div class="td-big-line"></div>
                <span class="td-big-dur">{{ formatDuration(trainDetail?.duration) }}</span>
              </div>
              <div class="td-big-station end">
                <span class="td-big-time">{{
                  trainDetail?.endTime?.split('T')[1]?.substring(0, 5) || '--:--'
                }}</span>
                <span class="td-big-name">{{ trainDetail?.toStation }}</span>
              </div>
            </div>

            <div class="td-stops-section">
              <div class="td-section-title">🚉 经停站信息</div>
              <div v-if="tdLoading" class="td-loading">
                <div class="td-spinner"></div>
                加载经停站...
              </div>
              <div v-else-if="tdStops.length" class="td-stops-list">
                <div
                  v-for="(stop, idx) in tdStops"
                  :key="idx"
                  class="td-stop"
                  :class="{ first: idx === 0, last: idx === tdStops.length - 1 }"
                >
                  <div class="td-stop-time">
                    <span class="td-arr">{{ stop.arriveTime?.substring(0, 5) || '——' }}</span>
                    <span v-if="idx !== 0 && idx !== tdStops.length - 1" class="td-dep">{{
                      stop.departTime?.substring(0, 5) || '——'
                    }}</span>
                  </div>
                  <div class="td-stop-dot"></div>
                  <div v-if="idx < tdStops.length - 1" class="td-stop-line"></div>
                  <div class="td-stop-info">
                    <span class="td-stop-name">{{ stop.stationName || '未知' }}</span>
                    <span v-if="stop.city && stop.city !== stop.stationName" class="td-stop-city">{{
                      stop.city
                    }}</span>
                    <span v-if="idx === 0" class="td-stop-tag start">始发</span>
                    <span v-else-if="idx === tdStops.length - 1" class="td-stop-tag end">终到</span>
                  </div>
                </div>
              </div>
              <div v-else class="td-stops-empty">暂无经停站数据</div>
            </div>

            <div class="td-seats-section">
              <div class="td-section-title">💺 座位余票</div>
              <div class="td-seats-grid">
                <div
                  v-for="seat in getSeatOptions(trainDetail)"
                  :key="seat.type"
                  class="td-seat-row"
                  :class="getSeatLevel(seat.count)"
                >
                  <div class="td-seat-name">{{ seat.type }}</div>
                  <div class="td-seat-bar-wrap">
                    <div
                      class="td-seat-bar-fill"
                      :style="{ width: Math.min(100, seat.count * 4) + '%' }"
                    ></div>
                  </div>
                  <div class="td-seat-count">
                    <span class="td-seat-num">{{ seat.count > 0 ? seat.count : 0 }}</span>
                    <span class="td-seat-label">{{ seat.count > 0 ? '张' : '无票' }}</span>
                  </div>
                  <div class="td-seat-price">¥{{ seat.price }}</div>
                </div>
              </div>
            </div>

            <div class="td-footer">
              <button
                v-if="trainDetail && hasAnySeat(trainDetail)"
                class="td-buy-btn"
                @click="closeDetailAndBuy"
              >
                立即购票 →
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="payfade">
        <div v-if="payVisible" class="pay-overlay">
          <div class="pay-card">
            <div class="pay-header">
              <span class="pay-title">💳 订单支付</span>
              <span class="pay-countdown" :class="{ urgent: payCountdownMin < 5 }">
                <span class="pc-label">剩余支付时间</span>
                <span class="pc-time">{{ payCountdownStr }}</span>
              </span>
            </div>

            <div class="pay-amount">
              <div class="pa-label">应付金额</div>
              <div class="pa-value">¥{{ payAmount.toFixed(2) }}</div>
            </div>

            <div class="pay-info">
              <div class="pi-line">
                <span class="pi-k">车次</span><span class="pi-v">{{ payData.trainNumber }}</span>
              </div>
              <div class="pi-line">
                <span class="pi-k">路线</span><span class="pi-v">{{ payData.from }} → {{ payData.to }}</span>
              </div>
              <div class="pi-line">
                <span class="pi-k">座位</span><span class="pi-v">{{ payData.seatType }}</span>
              </div>
              <div class="pi-line">
                <span class="pi-k">乘客</span><span class="pi-v">{{ payData.passenger }}</span>
              </div>
            </div>

            <div class="pay-methods">
              <div class="pm-label">选择支付方式</div>
              <div class="pm-grid">
                <button
                  class="pm-card"
                  :class="{ active: payMethod === 'alipay' }"
                  @click="payMethod = 'alipay'"
                >
                  <div class="pm-icon alipay">支</div>
                  <div class="pm-body">
                    <div class="pm-name">支付宝</div>
                    <div class="pm-desc">推荐 · 安全便捷</div>
                  </div>
                  <div v-if="payMethod === 'alipay'" class="pm-check">✓</div>
                </button>
                <button
                  class="pm-card"
                  :class="{ active: payMethod === 'wechat' }"
                  @click="payMethod = 'wechat'"
                >
                  <div class="pm-icon wechat">微</div>
                  <div class="pm-body">
                    <div class="pm-name">微信支付</div>
                    <div class="pm-desc">常用 · 免输卡号</div>
                  </div>
                  <div v-if="payMethod === 'wechat'" class="pm-check">✓</div>
                </button>
              </div>
            </div>

            <div class="pay-actions">
              <button class="pay-btn cancel" :disabled="payLoading" @click="cancelPay">取消订单</button>
              <button class="pay-btn primary" :disabled="payLoading" @click="doPay">
                <span v-if="payLoading" class="pay-spinner"></span>
                <span v-else>确认支付 ¥{{ payAmount.toFixed(2) }}</span>
              </button>
            </div>

            <div class="pay-tip">🔒 模拟支付环境 · 点击确认即完成 · 超时未支付自动取消</div>
          </div>
        </div>
      </Transition>

      <Transition name="success-dialog">
        <div v-if="successVisible" class="success-overlay" @click.self="closeSuccess">
          <div class="success-card">
            <div class="success-check">
              <svg viewBox="0 0 52 52" class="check-circle">
                <circle cx="26" cy="26" r="25" fill="none" />
                <path fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8" />
              </svg>
            </div>
            <h3 class="success-title">购票成功！</h3>
            <div class="success-info">
              <div class="si-row">
                <span class="si-label">车次</span>
                <span class="si-train">{{ successData.trainNumber }}</span>
              </div>
              <div class="si-row">
                <span class="si-label">路线</span>
                <span class="si-route">{{ successData.from }} → {{ successData.to }}</span>
              </div>
              <div class="si-row">
                <span class="si-label">座位</span>
                <span>{{ successData.seatType }}</span>
              </div>
              <div class="si-row">
                <span class="si-label">乘客</span>
                <span>{{ successData.passenger }}</span>
              </div>
            </div>
            <div class="success-actions">
              <button class="sa-btn ghost" @click="closeSuccess">继续购票</button>
              <button class="sa-btn primary" @click="goToOrders">查看订单</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <el-dialog
      v-model="buyDialogVisible"
      title="确认购票"
      width="460px"
      :close-on-click-modal="false"
      :show-close="true"
      class="buy-dialog"
    >
      <div class="bd-train-info">
        <div class="bd-train-no">{{ buyDialogData.train?.trainNumber }}</div>
        <div class="bd-train-route">
          <span class="bd-station">{{ buyDialogData.train?.fromStation }}</span>
          <span class="bd-time">{{ buyDialogData.train?.startTime }}</span>
          <span class="bd-arrow">→</span>
          <span class="bd-time">{{ buyDialogData.train?.endTime }}</span>
          <span class="bd-station">{{ buyDialogData.train?.toStation }}</span>
        </div>
      </div>

      <div class="bd-form-item">
        <label class="bd-label">请选择座位类型</label>
        <el-select
          v-model="buyDialogData.seatType"
          filterable
          placeholder="输入或选择座位类型"
          class="bd-select"
        >
          <el-option
            v-for="opt in buyDialogData.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          >
            <span class="bd-opt-name">{{ opt.value }}</span>
            <span class="bd-opt-price">¥{{ opt.price }}</span>
            <span class="bd-opt-count">{{ opt.count }} 张</span>
          </el-option>
        </el-select>
      </div>

      <div class="bd-form-item">
        <label class="bd-label">
          乘车人
          <button class="bd-add-pax" @click="openPaxManager">
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            管理
          </button>
        </label>
        <div class="bd-pax-list">
          <div v-for="(p, idx) in buyPassengers" :key="idx" class="bd-pax-chip" :class="{ main: idx === 0 }">
            <el-input v-model="p.name" placeholder="姓名" maxlength="20" size="small" class="bd-pax-input" />
            <el-input
              v-model="p.idCard"
              placeholder="证件号"
              maxlength="18"
              size="small"
              class="bd-pax-input"
            />
            <button v-if="buyPassengers.length > 1" class="bd-pax-del" @click="removeBuyPax(idx)">×</button>
          </div>
          <button v-if="buyPassengers.length < 5" class="bd-pax-add" @click="addBuyPax">+ 添加乘车人</button>
        </div>
        <div v-if="savedPassengers.length" class="bd-saved-pax">
          <span class="bd-saved-label">常用乘车人：</span>
          <button v-for="(sp, i) in savedPassengers" :key="i" class="bd-saved-chip" @click="quickAddPax(sp)">
            {{ sp.name }}<span class="bd-saved-id">{{ sp.idCard.slice(-4) }}</span>
          </button>
        </div>
      </div>

      <div class="bd-summary">
        <span class="bd-sum-label">合计</span>
        <span class="bd-sum-price">¥{{ buyTotalPrice }}</span>
      </div>

      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="buyDialogData.submitting"
          :disabled="!buyDialogData.seatType"
          @click="confirmBuy"
          >确认购票</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="paxManagerVisible" title="常用乘车人管理" width="480px" class="buy-dialog">
      <div class="pax-mgr-add">
        <div class="pax-mgr-row">
          <el-input v-model="paxMgrForm.name" placeholder="姓名" maxlength="20" />
          <el-input v-model="paxMgrForm.idCard" placeholder="证件号" maxlength="18" />
        </div>
        <el-button type="primary" @click="savePaxFromMgr">
          {{ paxMgrEditing ? '保存修改' : '添加' }}
        </el-button>
      </div>

      <div v-if="savedPassengers.length" class="pax-mgr-list">
        <div v-for="(p, i) in savedPassengers" :key="i" class="pax-mgr-item">
          <div class="pax-mgr-info" title="添加到本次购票" @click="pickSavedPax(p)">
            <span class="pax-mgr-name">{{ p.name }}</span>
            <span class="pax-mgr-id">{{ p.idCard }}</span>
          </div>
          <div class="pax-mgr-actions">
            <el-button size="small" text @click="pickSavedPax(p)">选择</el-button>
            <el-button size="small" text @click="startEditPax(p)">编辑</el-button>
            <el-button size="small" text type="danger" @click="deletePaxFromMgr(p)">删除</el-button>
          </div>
        </div>
      </div>
      <div v-else class="pax-mgr-empty">
        <svg
          width="48"
          height="48"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        <p>还没有常用乘车人</p>
      </div>
    </el-dialog>
    <AIChat />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import { useUserStore } from '../stores/user.js'
import AppNav from '../components/AppNav.vue'
import Fireworks from '../components/Fireworks.vue'
import AIChat from '../components/AIChat.vue'

const userStore = useUserStore()
const uk = (k) => `rf_${userStore.username || 'guest'}_${k}`

const router = useRouter()
const route = useRoute()

const queryForm = reactive({
  fromStation: '北京',
  toStation: '上海',
  date: new Date().toISOString().split('T')[0]
})

const quickRoutes = [
  { label: '北京→上海', fromStation: '北京', toStation: '上海' },
  { label: '上海→广州', fromStation: '上海', toStation: '广州' },
  { label: '北京→深圳', fromStation: '北京', toStation: '深圳' },
  { label: '广州→成都', fromStation: '广州', toStation: '成都' },
  { label: '杭州→北京', fromStation: '杭州', toStation: '北京' },
  { label: '武汉→西安', fromStation: '武汉', toStation: '西安' }
]
const quickActive = ref('')

const favRoutes = ref([])
function loadFavRoutes() {
  try {
    const raw = localStorage.getItem(uk('favRoutes'))
    if (raw) favRoutes.value = JSON.parse(raw)
  } catch {}
}
function saveFavRoutes() {
  localStorage.setItem(uk('favRoutes'), JSON.stringify(favRoutes.value))
}
function isFavRoute(from, to) {
  return favRoutes.value.some((r) => r.from === from && r.to === to)
}
function toggleFavRoute(from, to) {
  const idx = favRoutes.value.findIndex((r) => r.from === from && r.to === to)
  if (idx >= 0) {
    favRoutes.value.splice(idx, 1)
  } else {
    favRoutes.value.unshift({ from, to, label: `${from}→${to}` })
    if (favRoutes.value.length > 10) favRoutes.value.pop()
  }
  saveFavRoutes()
  if (!searched.value) applyQuick({ fromStation: from, toStation: to })
}
function toggleCurrentRouteFav() {
  if (!queryForm.fromStation || !queryForm.toStation) return
  toggleFavRoute(queryForm.fromStation, queryForm.toStation)
}
function searchWithFavDate(route, dateOffset = 0) {
  queryForm.fromStation = route.from
  queryForm.toStation = route.to
  if (dateOffset) {
    const d = new Date()
    d.setDate(d.getDate() + dateOffset)
    queryForm.date = d.toISOString().split('T')[0]
  }
  search()
}
loadFavRoutes()

const results = ref([])
const searched = ref(false)
const searching = ref(false)
const buyingRowId = ref(null)
const showFireworks = ref(false)
const successVisible = ref(false)
const successData = reactive({
  trainNumber: '',
  from: '',
  to: '',
  seatType: '',
  passenger: ''
})

const payVisible = ref(false)
const payLoading = ref(false)
const payMethod = ref('alipay')
const payAmount = ref(0)
const payDeadline = ref(0)
const payCountdownMin = ref(15)
const payData = reactive({
  trainNumber: '',
  from: '',
  to: '',
  seatType: '',
  passenger: '',
  row: null
})
let payTickTimer = null

const payCountdownStr = computed(() => {
  const remaining = Math.max(0, Math.floor((payDeadline.value - Date.now()) / 1000))
  const m = Math.floor(remaining / 60)
  const s = remaining % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const trainDetailVisible = ref(false)
const trainDetail = ref(null)
const tdStops = ref([])
const tdLoading = ref(false)

function formatDuration(d) {
  if (d === null || d === undefined) return '--'
  if (typeof d === 'number') {
    const h = Math.floor(d / 60)
    const m = d % 60
    if (h === 0) return m + '分钟'
    return h + '小时' + (m > 0 ? m + '分钟' : '')
  }
  const s = String(d)
  if (s.includes('小时') || s.includes('分钟')) return s
  const match = s.match(/(\d+):(\d+)/)
  if (match) {
    const h = parseInt(match[1]),
      m = parseInt(match[2])
    if (h === 0) return m + '分钟'
    return h + '小时' + (m > 0 ? m + '分钟' : '')
  }
  return s
}

async function openTrainDetail(train) {
  trainDetail.value = train
  trainDetailVisible.value = true
  tdStops.value = []

  if (!train.id) return

  tdLoading.value = true
  try {
    const stops = await request.get(`/train/${train.id}/stops`)
    tdStops.value = Array.isArray(stops) ? stops : []
  } catch {
    const names = [train.fromStation]
    const mid = ['济南西', '蚌埠南', '南京南', '苏州北', '无锡东']
    const seed = train.trainNumber?.charCodeAt(0) || 0
    for (let i = 0; i < 3 + (seed % 3); i++) names.push(mid[i % mid.length])
    names.push(train.toStation)
    tdStops.value = names.map((n, i) => ({
      stationName: n,
      city: n,
      arriveTime:
        i === 0 ? '' : `${String(7 + i * 2).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}`,
      departTime:
        i === names.length - 1
          ? ''
          : `${String(7 + i * 2).padStart(2, '0')}:${String((i * 7 + 3) % 60).padStart(2, '0')}`
    }))
  } finally {
    tdLoading.value = false
  }
}

const sortOptions = [
  { label: '出发早→晚', value: 'start' },
  { label: '出发晚→早', value: 'startDesc' },
  { label: '耗时短→长', value: 'duration' },
  { label: '价格低→高', value: 'price' }
]
const sortBy = ref('start')
const onlyAvailable = ref(false)
const trainTypeFilter = ref('all')
const timeFilter = ref('all')

const trainTypeOptions = [
  { label: '全部', value: 'all' },
  { label: '高铁 G/C', value: 'hsr' },
  { label: '动车 D', value: 'emu' },
  { label: '直达 Z', value: 'z' },
  { label: '普快', value: 'normal' }
]
const timeOptions = [
  { label: '全部', value: 'all' },
  { label: '🌅 上午', value: 'morning' },
  { label: '☀️ 下午', value: 'afternoon' },
  { label: '🌙 夜间', value: 'night' }
]

function matchTrainType(train, filter) {
  if (filter === 'all') return true
  const num = (train.trainNumber || '').toUpperCase()
  if (filter === 'hsr') return /^[GC]/.test(num)
  if (filter === 'emu') return /^D/.test(num)
  if (filter === 'z') return /^Z/.test(num)
  if (filter === 'normal') return /^[KTYL]|\d{1,4}$/.test(num)
  return true
}

function matchTime(train, filter) {
  if (filter === 'all') return true
  const h = parseInt((train.startTime || '00:00').split(':')[0])
  if (filter === 'morning') return h >= 6 && h < 12
  if (filter === 'afternoon') return h >= 12 && h < 18
  if (filter === 'night') return h >= 18 || h < 6
  return true
}

const dateShortcuts = computed(() => {
  const today = new Date()
  const labels = ['今天', '明天', '后天', '4天后', '7天后']
  return labels.map((label, i) => {
    const d = new Date(today)
    d.setDate(d.getDate() + (i + 1 === 1 ? 0 : i))
    return { label, value: d.toISOString().split('T')[0] }
  })
})

const displayResults = computed(() => {
  let list = results.value
  if (onlyAvailable.value) {
    list = list.filter((t) => hasAnySeat(t))
  }
  if (trainTypeFilter.value !== 'all') {
    list = list.filter((t) => matchTrainType(t, trainTypeFilter.value))
  }
  if (timeFilter.value !== 'all') {
    list = list.filter((t) => matchTime(t, timeFilter.value))
  }
  const sorted = [...list]
  if (sortBy.value === 'start') {
    sorted.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
  } else if (sortBy.value === 'startDesc') {
    sorted.sort((a, b) => (b.startTime || '').localeCompare(a.startTime || ''))
  } else if (sortBy.value === 'duration') {
    sorted.sort((a, b) => (a.duration ?? 9999) - (b.duration ?? 9999))
  } else if (sortBy.value === 'price') {
    sorted.sort((a, b) => {
      const pa = Math.min(
        a.secondSeatPrice ?? 99999,
        a.firstSeatPrice ?? 99999,
        a.businessSeatPrice ?? 99999,
        a.sleeperPrice ?? 99999
      )
      const pb = Math.min(
        b.secondSeatPrice ?? 99999,
        b.firstSeatPrice ?? 99999,
        b.businessSeatPrice ?? 99999,
        b.sleeperPrice ?? 99999
      )
      return pa - pb
    })
  }
  return sorted
})

const historyList = ref([])

const hotRoutes = [
  { from: '北京', to: '上海', dur: '4h30m', train: 'G7 · G101' },
  { from: '上海', to: '广州', dur: '7h', train: 'G85 · D931' },
  { from: '北京', to: '西安', dur: '4h15m', train: 'G19 · G87' },
  { from: '上海', to: '成都', dur: '10h', train: 'D352 · G2185' },
  { from: '广州', to: '深圳', dur: '1h', train: 'G6321 · C7001' },
  { from: '北京', to: '杭州', dur: '5h', train: 'G39 · G41' },
  { from: '上海', to: '南京', dur: '1h10m', train: 'G7001 · G101' },
  { from: '成都', to: '重庆', dur: '2h', train: 'G2204 · D2202' }
]

function quickQueryHot(r) {
  queryForm.fromStation = r.from
  queryForm.toStation = r.to
  search()
}

function loadHistory() {
  try {
    const raw = localStorage.getItem(uk('history'))
    if (raw) historyList.value = JSON.parse(raw)
  } catch {}
}

function saveHistory() {
  const key = `${queryForm.fromStation}→${queryForm.toStation}`
  const item = {
    from: queryForm.fromStation,
    to: queryForm.toStation,
    date: queryForm.date,
    key,
    ts: Date.now()
  }
  const list = [item, ...historyList.value.filter((h) => h.key !== key)].slice(0, 5)
  historyList.value = list
  localStorage.setItem(uk('history'), JSON.stringify(list))
}

function formatHistoryTime(ts) {
  const diff = Date.now() - ts
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / 3600000) + '小时前'
  const days = Math.floor(diff / 86400000)
  if (days < 7) return days + '天前'
  return ''
}

function applyHistory(h) {
  queryForm.fromStation = h.from
  queryForm.toStation = h.to
  queryForm.date = h.date
  search()
}

function deleteHistoryItem(i) {
  historyList.value.splice(i, 1)
  localStorage.setItem(uk('history'), JSON.stringify(historyList.value))
}

function clearHistory() {
  historyList.value = []
  localStorage.removeItem(uk('history'))
}

const favorites = ref([])

function loadFavs() {
  try {
    const raw = localStorage.getItem(uk('favorites'))
    if (raw) favorites.value = JSON.parse(raw)
  } catch {}
}

function saveFavs() {
  localStorage.setItem(uk('favorites'), JSON.stringify(favorites.value))
}

function isFav(t) {
  return favorites.value.some((f) => f.trainNumber === t.trainNumber && f.startTime === t.startTime)
}

function toggleFav(t) {
  if (isFav(t)) {
    favorites.value = favorites.value.filter(
      (f) => !(f.trainNumber === t.trainNumber && f.startTime === t.startTime)
    )
    ElMessage.success('已取消收藏')
  } else {
    favorites.value.unshift({
      trainNumber: t.trainNumber,
      fromStation: t.fromStation,
      toStation: t.toStation,
      startTime: t.startTime,
      endTime: t.endTime,
      duration: t.duration,
      seatTypes: t.seatTypes
    })
    ElMessage.success('已收藏该车次')
  }
  saveFavs()
}

let searchAbort = null

const buyDialogVisible = ref(false)
const buyPassengers = ref([{ name: '', idCard: '' }])
const buyDialogData = reactive({
  train: null,
  options: [],
  seatType: '',
  submitting: false
})

const buyTotalPrice = computed(() => {
  const opt = buyDialogData.options.find((o) => o.value === buyDialogData.seatType)
  if (!opt) return 0
  return (opt.price * buyPassengers.value.length).toFixed(2)
})

const savedPassengers = ref([])

function loadPassengers() {
  try {
    const raw = localStorage.getItem(uk('passengers'))
    if (raw) savedPassengers.value = JSON.parse(raw)
  } catch {}
}

function savePassengers() {
  localStorage.setItem(uk('passengers'), JSON.stringify(savedPassengers.value))
}

function addBuyPax() {
  if (buyPassengers.value.length >= 5) return
  buyPassengers.value.push({ name: '', idCard: '' })
}

function removeBuyPax(idx) {
  buyPassengers.value.splice(idx, 1)
}

function quickAddPax(sp) {
  const emptyIdx = buyPassengers.value.findIndex((p) => !p.name?.trim() || !p.idCard?.trim())
  if (emptyIdx >= 0) {
    buyPassengers.value[emptyIdx] = { name: sp.name, idCard: sp.idCard }
  } else if (buyPassengers.value.length < 5) {
    buyPassengers.value.push({ name: sp.name, idCard: sp.idCard })
  }
}

const paxManagerVisible = ref(false)
const paxMgrEditing = ref(null)
const paxMgrForm = reactive({ name: '', idCard: '' })

function openPaxManager() {
  paxMgrEditing.value = null
  paxMgrForm.name = ''
  paxMgrForm.idCard = ''
  paxManagerVisible.value = true
}

function startEditPax(p) {
  paxMgrEditing.value = p
  paxMgrForm.name = p.name
  paxMgrForm.idCard = p.idCard
}

function savePaxFromMgr() {
  if (!paxMgrForm.name.trim() || !paxMgrForm.idCard.trim()) {
    ElMessage.warning('请填写完整姓名和证件号')
    return
  }
  if (paxMgrEditing.value) {
    paxMgrEditing.value.name = paxMgrForm.name.trim()
    paxMgrEditing.value.idCard = paxMgrForm.idCard.trim()
  } else {
    savedPassengers.value.push({ name: paxMgrForm.name.trim(), idCard: paxMgrForm.idCard.trim() })
  }
  savePassengers()
  paxMgrEditing.value = null
  paxMgrForm.name = ''
  paxMgrForm.idCard = ''
}

function deletePaxFromMgr(p) {
  savedPassengers.value = savedPassengers.value.filter((x) => x !== p)
  savePassengers()
}

function pickSavedPax(p) {
  if (buyPassengers.value.length >= 5) {
    ElMessage.warning('最多添加 5 位乘车人')
    return
  }
  buyPassengers.value.push({ name: p.name, idCard: p.idCard })
  paxManagerVisible.value = false
  ElMessage.success(`已添加 ${p.name}`)
}

const stats = reactive({ stations: 0, trains: 0 })
const stationList = ref([])

const STATION_CACHE_KEY = 'rf_stations'
const STATION_CACHE_TTL = 10 * 60 * 1000

async function loadStations() {
  const cached = localStorage.getItem(STATION_CACHE_KEY)
  if (cached) {
    try {
      const { list, ts } = JSON.parse(cached)
      if (Date.now() - ts < STATION_CACHE_TTL && Array.isArray(list) && list.length) {
        stationList.value = list
        stats.stations = list.length
        return
      }
    } catch {}
  }
  try {
    const list = await request.get('/station/list')
    if (Array.isArray(list)) {
      stationList.value = list
      stats.stations = list.length
      localStorage.setItem(STATION_CACHE_KEY, JSON.stringify({ list, ts: Date.now() }))
    }
  } catch {}
}

onMounted(async () => {
  loadHistory()
  loadFavs()
  loadPassengers()

  startTickTimer()

  const qr = route.query
  if (qr.from || qr.to || qr.date) {
    if (qr.from) queryForm.fromStation = String(qr.from)
    if (qr.to) queryForm.toStation = String(qr.to)
    if (qr.date) queryForm.date = String(qr.date)
    const d = new Date()
    d.setDate(d.getDate() + (qr.tomorrow ? 1 : 0))
  } else {
    try {
      const raw = localStorage.getItem(uk('quick_search'))
      if (raw) {
        const q = JSON.parse(raw)
        if (q?.from && q?.to) {
          queryForm.fromStation = q.from
          queryForm.toStation = q.to
          if (q.date) queryForm.date = q.date
        }
        localStorage.removeItem(uk('quick_search'))
      } else {
        const lastRaw = localStorage.getItem(uk('last_search'))
        if (lastRaw) {
          const q = JSON.parse(lastRaw)
          if (q?.from) queryForm.fromStation = q.from
          if (q?.to) queryForm.toStation = q.to
          if (q?.date) queryForm.date = q.date
        }
      }
    } catch {}
  }

  await loadStations()
  search()
})

watch(
  () => route.query,
  (nq) => {
    if (nq.from || nq.to) {
      if (nq.from) queryForm.fromStation = String(nq.from)
      if (nq.to) queryForm.toStation = String(nq.to)
      search()
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (searchAbort) searchAbort.abort()
  stopTickTimer()
  if (payTickTimer) {
    clearInterval(payTickTimer)
    payTickTimer = null
  }
  trainDetailVisible.value = false
  payVisible.value = false
  successVisible.value = false
  showFireworks.value = false
  if (queryForm.fromStation || queryForm.toStation) {
    localStorage.setItem(
      uk('last_search'),
      JSON.stringify({
        from: queryForm.fromStation,
        to: queryForm.toStation,
        date: queryForm.date
      })
    )
  }
})

onUnmounted(() => {
  document.querySelectorAll('.td-overlay, .pay-overlay, .success-overlay').forEach((el) => el.remove())
})

const tick = ref(0)
let tickTimer = null
function startTickTimer() {
  if (tickTimer) return
  tick.value = Date.now()
  tickTimer = setInterval(() => {
    tick.value = Date.now()
  }, 1000)
}
function stopTickTimer() {
  if (tickTimer) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

function getCountdown(train) {
  const now = tick.value
  let dateStr = queryForm.date
  let timeStr = (train.startTime || '').split('T')[1]?.substring(0, 5) || train.startTime
  if (!timeStr || !/^\d{2}:\d{2}$/.test(timeStr)) return null
  const depMs = new Date(`${dateStr}T${timeStr}:00`).getTime()
  const diff = depMs - now
  const depDate = new Date(depMs)
  const today = new Date()
  const sameDay = depDate.toDateString() === today.toDateString()
  const tomorrow = new Date(today)
  tomorrow.setDate(today.getDate() + 1)
  const isTomorrow = depDate.toDateString() === tomorrow.toDateString()
  if (!sameDay && !isTomorrow) return null
  if (diff <= -600000) return { status: 'departed', text: '已发车' }
  if (diff < 0) return { status: 'departing', text: Math.abs(Math.floor(diff / 60000)) + '分钟前发车' }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (h > 0) return { status: 'ok', text: `${h}小时${m}分钟后发车` }
  if (m > 0) return { status: 'soon', text: `${m}分钟后发车` }
  return { status: 'now', text: '即将发车！' }
}

function matchStations(query) {
  if (!query) return stationList.value.slice(0, 8)
  const q = query.toLowerCase().trim()
  return stationList.value
    .filter(
      (s) =>
        (s.name && s.name.includes(query)) ||
        (s.code && s.code.toLowerCase().includes(q)) ||
        (s.city && s.city.includes(query))
    )
    .slice(0, 10)
}

function fetchFromSuggestions(query, cb) {
  cb(matchStations(query))
}

function fetchToSuggestions(query, cb) {
  cb(matchStations(query))
}

function beforeToday(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function swapStations() {
  if (!queryForm.fromStation && !queryForm.toStation) return
  ;[queryForm.fromStation, queryForm.toStation] = [queryForm.toStation, queryForm.fromStation]
  quickActive.value = ''
  if (searched.value) search()
}

function applyQuick(r) {
  queryForm.fromStation = r.fromStation
  queryForm.toStation = r.toStation
  quickActive.value = r.label
  search()
}

async function search() {
  if (!queryForm.fromStation || !queryForm.toStation || !queryForm.date) {
    ElMessage.warning('请填写完整查询信息')
    return
  }
  if (searchAbort) searchAbort.abort()
  const controller = new AbortController()
  searchAbort = controller

  searching.value = true
  searched.value = true
  try {
    const list = await request.get('/ticket/query', {
      params: queryForm,
      signal: controller.signal
    })
    results.value = Array.isArray(list)
      ? [...list].sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
      : []
    saveHistory()
    localStorage.setItem(
      uk('last_search'),
      JSON.stringify({
        from: queryForm.fromStation,
        to: queryForm.toStation,
        date: queryForm.date
      })
    )
  } catch (e) {
    if (e.code !== 'ERR_CANCELED') {
      results.value = []
    }
  } finally {
    if (searchAbort === controller) {
      searching.value = false
      searchAbort = null
    }
  }
}

function getSeatOptions(train) {
  return [
    {
      type: '商务座',
      label: `商务座 (¥${train.businessSeatPrice})`,
      count: train.businessSeatCount,
      price: train.businessSeatPrice
    },
    {
      type: '一等座',
      label: `一等座 (¥${train.firstSeatPrice})`,
      count: train.firstSeatCount,
      price: train.firstSeatPrice
    },
    {
      type: '二等座',
      label: `二等座 (¥${train.secondSeatPrice})`,
      count: train.secondSeatCount,
      price: train.secondSeatPrice
    },
    {
      type: '硬卧',
      label: `硬卧 (¥${train.sleeperPrice})`,
      count: train.sleeperCount,
      price: train.sleeperPrice
    }
  ].filter((s) => s.count !== undefined && s.count !== null)
}

function getSeatLevel(count) {
  if (!count || count <= 0) return 'empty'
  if (count < 10) return 'tense'
  return 'ok'
}

function hasAnySeat(train) {
  return getSeatOptions(train).some((s) => s.count > 0)
}

function closeDetailAndBuy() {
  trainDetailVisible.value = false
  buyTicket(trainDetail.value)
}

function closeSuccess() {
  successVisible.value = false
  showFireworks.value = false
}

async function buyTicket(row) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: '/home' } })
    return
  }

  const opts = []
  if (row.businessSeatCount > 0)
    opts.push({ value: '商务座', price: row.businessSeatPrice, count: row.businessSeatCount })
  if (row.firstSeatCount > 0)
    opts.push({ value: '一等座', price: row.firstSeatPrice, count: row.firstSeatCount })
  if (row.secondSeatCount > 0)
    opts.push({ value: '二等座', price: row.secondSeatPrice, count: row.secondSeatCount })
  if (row.sleeperCount > 0) opts.push({ value: '硬卧', price: row.sleeperPrice, count: row.sleeperCount })

  if (!opts.length) {
    ElMessage.warning('该车次已无余票')
    return
  }

  buyDialogData.train = row
  buyDialogData.options = opts
  buyDialogData.seatType = opts[0].value
  buyDialogData.submitting = false
  buyPassengers.value = savedPassengers.value.length
    ? [{ ...savedPassengers.value[0] }]
    : [{ name: '', idCard: '' }]
  buyDialogVisible.value = true
}

function getSeatPrice(row, seatType) {
  const map = {
    商务座: row.businessSeatPrice,
    一等座: row.firstSeatPrice,
    二等座: row.secondSeatPrice,
    硬卧: row.sleeperPrice
  }
  return map[seatType] || 0
}

async function confirmBuy() {
  for (let i = 0; i < buyPassengers.value.length; i++) {
    const p = buyPassengers.value[i]
    if (!p.name?.trim()) {
      ElMessage.error(`第 ${i + 1} 位乘车人姓名不能为空`)
      return
    }
    if (!p.idCard?.trim()) {
      ElMessage.error(`第 ${i + 1} 位乘车人证件号不能为空`)
      return
    }
    if (!/^\d{15}$|^\d{17}[\dXx]$/.test(p.idCard.trim())) {
      ElMessage.error(`第 ${i + 1} 位乘车人证件号格式不正确`)
      return
    }
  }

  const row = buyDialogData.train
  const seatType = buyDialogData.seatType
  const pricePerTicket = getSeatPrice(row, seatType)

  const first = buyPassengers.value[0]
  payData.row = row
  payData.trainNumber = row.trainNumber
  payData.from = row.fromStation
  payData.to = row.toStation
  payData.seatType = seatType
  payData.passenger =
    buyPassengers.value.length === 1
      ? first.name.trim()
      : `${first.name.trim()} 等 ${buyPassengers.value.length} 人`

  payAmount.value = pricePerTicket * buyPassengers.value.length
  payDeadline.value = Date.now() + 15 * 60 * 1000
  payCountdownMin.value = 15

  buyDialogVisible.value = false
  payVisible.value = true

  if (payTickTimer) clearInterval(payTickTimer)
  payTickTimer = setInterval(() => {
    const remaining = Math.floor((payDeadline.value - Date.now()) / 1000)
    payCountdownMin.value = Math.max(0, Math.ceil(remaining / 60))
    if (remaining <= 0) {
      clearInterval(payTickTimer)
      payVisible.value = false
      ElMessage.warning('支付超时，订单已取消')
    }
  }, 1000)
}

function cancelPay() {
  payVisible.value = false
  if (payTickTimer) clearInterval(payTickTimer)
  ElMessage.info('已取消支付')
}

async function doPay() {
  payLoading.value = true
  try {
    await new Promise((r) => setTimeout(r, 1200))
    const row = payData.row
    const seatType = payData.seatType
    for (const p of buyPassengers.value) {
      await doBuy(row, seatType, p.name.trim(), p.idCard.trim())
    }
    payVisible.value = false
    if (payTickTimer) clearInterval(payTickTimer)
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    payLoading.value = false
  }
}

async function doBuy(row, seatType, passengerName, passengerIdCard) {
  buyingRowId.value = row.trainNumber
  try {
    await request.post('/order/buy', {
      trainId: row.trainId,
      trainDate: queryForm.date,
      seatType,
      passengerName,
      passengerIdCard
    })
    Object.assign(successData, {
      trainNumber: row.trainNumber,
      from: row.fromStation,
      to: row.toStation,
      seatType,
      passenger: passengerName
    })
    successVisible.value = true
    showFireworks.value = true
    window.dispatchEvent(new Event('railflow-refresh-unread'))
  } finally {
    buyingRowId.value = null
  }
}

function goToOrders() {
  successVisible.value = false
  showFireworks.value = false
  router.push('/orders')
}
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.hero {
  position: relative;
  padding: 32px 32px 20px;
  overflow: hidden;
}

.hero-bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.3;
  pointer-events: none;
  z-index: 0;
}

.blob-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #3b82f6, transparent);
  top: -200px;
  right: -100px;
}

.blob-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #8b5cf6, transparent);
  bottom: -100px;
  left: -100px;
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 4px;
  line-height: 1.2;
}

.gradient-text {
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #f472b6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-sub {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 20px;
}

.search-box {
  padding: 24px;
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.92), rgba(15, 23, 42, 0.95)) !important;
  border: 1px solid rgba(96, 165, 250, 0.25) !important;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 40px rgba(59, 130, 246, 0.08) !important;
  backdrop-filter: blur(20px);
  border-radius: 20px;
}

.search-box::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 22px;
  background: conic-gradient(
    from 0deg,
    transparent 0%,
    rgba(59, 130, 246, 0.5) 15%,
    transparent 30%,
    rgba(139, 92, 246, 0.5) 45%,
    transparent 60%,
    rgba(236, 72, 153, 0.4) 75%,
    transparent 90%
  );
  animation: borderRotate 12s linear infinite;
  z-index: -1;
  opacity: 1;
}

.search-box:hover::before {
  opacity: 1;
  animation-duration: 6s;
}

@keyframes borderRotate {
  to {
    transform: rotate(360deg);
  }
}

.search-row {
  display: flex;
  gap: 16px;
  align-items: stretch;
  flex-wrap: wrap;
}

.station-group {
  display: flex;
  gap: 4px;
  align-items: flex-end;
  flex: 1;
  min-width: 320px;
}

.station-col {
  flex: 1;
}

.station-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
  margin-bottom: 8px;
  display: block;
}

.swap-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(139, 92, 246, 0.2));
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-bottom: 2px;
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}

.swap-btn:hover {
  transform: rotate(180deg) scale(1.1);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.4), rgba(139, 92, 246, 0.4));
  color: #e0e7ff;
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.4);
}

.swap-btn svg {
  width: 16px;
  height: 16px;
}

.station-input :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.7) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
}

.station-input :deep(.el-input__inner) {
  color: #f1f5f9 !important;
  font-weight: 600 !important;
}

.station-input :deep(.el-autocomplete-suggestion) {
  background: rgba(30, 41, 59, 0.98) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5) !important;
  padding: 6px !important;
  overflow: hidden;
}

.station-input :deep(.el-autocomplete-suggestion li) {
  border-radius: 8px !important;
  padding: 0 12px !important;
  height: 36px !important;
  line-height: 36px !important;
  color: #cbd5e1 !important;
  font-weight: 500;
}

.station-input :deep(.el-autocomplete-suggestion li.is-hovering),
.station-input :deep(.el-autocomplete-suggestion li:hover) {
  background: rgba(59, 130, 246, 0.15) !important;
  color: #f1f5f9 !important;
}

.ac-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.ac-name {
  font-weight: 600;
  font-size: 14px;
}

.ac-code {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
  background: rgba(15, 23, 42, 0.6);
  padding: 2px 8px;
  border-radius: 6px;
  letter-spacing: 1px;
}

.date-col {
  min-width: 180px;
}

.date-input :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.7) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
}

.date-input :deep(.el-input__inner) {
  color: #f1f5f9 !important;
  font-weight: 600 !important;
}

.date-shortcuts {
  display: flex;
  gap: 4px;
}

.date-shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  height: 44px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(15, 23, 42, 0.4);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.date-shortcut:hover {
  border-color: rgba(96, 165, 250, 0.3);
  color: #cbd5e1;
}

.date-shortcut.active {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(96, 165, 250, 0.5);
  color: #93c5fd;
}

.ds-day {
  font-size: 11px;
  font-weight: 600;
}

.ds-date {
  font-size: 10px;
  color: #64748b;
}

.date-shortcut.active .ds-date {
  color: #60a5fa;
}

.search-btn {
  padding: 0 40px;
  height: 44px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 50%, #8b5cf6 100%);
  background-size: 200% 200%;
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.45);
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  animation: btnGradient 5s ease infinite;
  position: relative;
  overflow: hidden;
}

.search-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(59, 130, 246, 0.6),
    0 0 40px rgba(139, 92, 246, 0.3);
}

.search-btn:active:not(:disabled) {
  transform: translateY(0);
}

@keyframes btnGradient {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.quick-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.quick-tag {
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  background: rgba(15, 23, 42, 0.5);
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
}

.quick-tag.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(139, 92, 246, 0.2));
  border-color: rgba(96, 165, 250, 0.5);
  color: #e2e8f0;
}

.stats-row {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  border-radius: 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.stat-ico {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-ico svg {
  width: 22px;
  height: 22px;
}

.ico-blue {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.ico-purple {
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
}

.ico-pink {
  background: rgba(244, 114, 182, 0.15);
  color: #f472b6;
}

.stat-body {
  text-align: left;
}

.stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #f1f5f9;
  display: block;
}

.stat-cap {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.results {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 32px 80px;
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.results-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #e2e8f0;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
  margin-right: 4px;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  background: rgba(15, 23, 42, 0.5);
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #cbd5e1;
  border-color: rgba(96, 165, 250, 0.3);
}

.filter-btn.active {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(96, 165, 250, 0.5);
  color: #93c5fd;
}

.fav-toggle {
  gap: 5px;
  background: rgba(15, 23, 42, 0.4);
  border: 1px dashed rgba(148, 163, 184, 0.3);
}
.fav-toggle.active {
  background: rgba(251, 191, 36, 0.15);
  border-color: rgba(251, 191, 36, 0.5);
  color: #fbbf24;
}
.fav-toggle svg {
  color: inherit;
}

.fav-routes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
  margin-top: 4px;
}
.fav-route-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 10px;
  position: relative;
  transition: all 0.2s;
}
.fav-route-card:hover {
  border-color: rgba(251, 191, 36, 0.5);
  background: rgba(30, 41, 59, 0.7);
}
.fav-route-main {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
}
.fav-route-main:hover {
  color: #93c5fd;
}
.fav-from,
.fav-to {
  padding: 2px 6px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 4px;
}
.fav-to {
  background: rgba(16, 185, 129, 0.15);
}
.fav-route-quick {
  display: flex;
  gap: 4px;
}
.fav-route-quick button {
  padding: 3px 8px;
  font-size: 11px;
  border-radius: 5px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  color: #94a3b8;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.15s;
}
.fav-route-quick button:hover {
  background: rgba(59, 130, 246, 0.2);
  color: #93c5fd;
  border-color: rgba(96, 165, 250, 0.4);
}
.fav-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.8);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s;
}
.fav-route-card:hover .fav-remove {
  opacity: 1;
}
.fav-remove:hover {
  background: rgba(239, 68, 68, 1);
}

.history-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.history-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.history-date {
  opacity: 0.6;
  font-size: 11px;
}

.history-time {
  opacity: 0.45;
  font-size: 10px;
  margin-left: 2px;
}

.history-del {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.85);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.6);
  transition: all 0.2s;
  z-index: 2;
}

.history-wrap:hover .history-del {
  opacity: 1;
  transform: scale(1);
}

.history-del:hover {
  background: #ef4444;
  transform: scale(1.15);
}

.clear-btn {
  margin-left: auto;
  font-size: 11px;
  color: #64748b;
  border-color: transparent;
  background: transparent;
}

.clear-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.go-home-btn {
  margin-top: 16px;
  padding: 10px 28px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.date-badge {
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  font-size: 13px;
  font-weight: 600;
}

.skeleton-list,
.train-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sk-line,
.sk-chip {
  background: linear-gradient(
    90deg,
    rgba(71, 85, 105, 0.4) 0%,
    rgba(148, 163, 184, 0.25) 25%,
    rgba(148, 163, 184, 0.45) 50%,
    rgba(148, 163, 184, 0.25) 75%,
    rgba(71, 85, 105, 0.4) 100%
  );
  background-size: 200% 100%;
  animation: skShimmer 1.6s ease-in-out infinite;
}

.sk-line {
  height: 18px;
  border-radius: 8px;
}

.sk-lg {
  width: 70%;
  height: 26px;
}
.sk-md {
  width: 50%;
  height: 20px;
}
.sk-sm {
  width: 90%;
  height: 14px;
}

.sk-chips {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.sk-chip {
  width: 80px;
  height: 36px;
  border-radius: 10px;
}

@keyframes skShimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.fav-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.5);
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 2;
}

.fav-btn:hover {
  border-color: rgba(250, 204, 21, 0.4);
  color: #fbbf24;
  transform: scale(1.1);
}

.fav-btn.active {
  border-color: rgba(250, 204, 21, 0.6);
  color: #fbbf24;
  background: rgba(250, 204, 21, 0.12);
}

.fav-btn.active svg {
  fill: #fbbf24;
}

.train-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px 28px;
  border-radius: 16px;
  overflow: hidden;
  transform-style: preserve-3d;
  perspective: 1200px;
  transition:
    transform 0.5s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.5s ease,
    border-color 0.3s ease;
}

.train-card:hover {
  transform: translateY(-6px) rotateX(3deg) rotateY(-2deg);
  border-color: rgba(96, 165, 250, 0.4) !important;
  box-shadow:
    0 18px 54px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(59, 130, 246, 0.18),
    0 0 40px rgba(59, 130, 246, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;
}

.train-card:hover .train-num {
  transform: translateZ(20px);
}

.train-card:hover .buy-btn {
  transform: translateZ(24px) scale(1.03);
}

.train-card:hover .shimmer-overlay {
  opacity: 1;
  animation: cardShimmer 1.2s ease-out forwards;
}

.train-card:hover .track-dot.start {
  background: #38bdf8;
  box-shadow: 0 0 12px 3px rgba(56, 189, 248, 0.6);
}

.train-card:hover .track-dot.end {
  background: #f472b6;
  box-shadow: 0 0 12px 3px rgba(244, 114, 182, 0.6);
}

.train-card.favorited {
  border: 1px solid rgba(250, 204, 21, 0.28);
  box-shadow:
    0 0 0 1px rgba(250, 204, 21, 0.08),
    0 4px 24px rgba(250, 204, 21, 0.08);
}

.train-card.favorited:hover {
  border-color: rgba(250, 204, 21, 0.5) !important;
  box-shadow:
    0 14px 50px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(250, 204, 21, 0.2),
    0 0 30px rgba(250, 204, 21, 0.12) !important;
}

.train-card.favorited::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  bottom: 16px;
  width: 3px;
  border-radius: 2px;
  background: linear-gradient(180deg, #fbbf24, #f59e0b);
  box-shadow: 0 0 10px rgba(250, 204, 21, 0.5);
}

.train-main {
  flex: 1;
}

.train-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.train-num-wrap {
  text-align: center;
  flex-shrink: 0;
  padding-right: 20px;
  border-right: 1px solid rgba(148, 163, 184, 0.15);
}

.train-num {
  font-size: 22px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  display: block;
}

.train-type-badge {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.train-num-wrap {
  position: relative;
}

.countdown-tag {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  margin-top: 6px;
  letter-spacing: 0.5px;
}

.countdown-tag.ok {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.countdown-tag.soon {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.3);
  animation: countdownSoonPulse 1.2s ease-in-out infinite;
}

.countdown-tag.now {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.4);
  animation: countdownNowPulse 0.8s ease-in-out infinite;
}

.countdown-tag.departing {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.countdown-tag.departed {
  background: rgba(107, 114, 128, 0.25);
  color: #9ca3af;
  border: 1px dashed rgba(107, 114, 128, 0.4);
  text-decoration: line-through;
}

.train-card.favorited {
  border-color: rgba(251, 191, 36, 0.35) !important;
}
.train-card.favorited .train-num {
  color: #fbbf24;
}

.route-line {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.station-time {
  text-align: center;
  min-width: 70px;
}

.station-time.right {
  text-align: center;
}

.time-big {
  font-size: 20px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  display: block;
}

.station-name {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.journey {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.duration {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.track {
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;
}

.track-line {
  flex: 1;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #3b82f6);
  background-size: 200% 100%;
  border-radius: 2px;
  animation: trackGradient 4s linear infinite;
}

@keyframes trackGradient {
  0% {
    background-position: 0% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.track-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #60a5fa;
  position: absolute;
  transition: all 0.3s ease;
  box-shadow: 0 0 6px rgba(96, 165, 250, 0.5);
}

.track-dot.start {
  left: 0;
  background: #3b82f6;
}

.track-dot.end {
  right: 0;
  background: #8b5cf6;
}

.track-dot.start.pulse-dot {
  animation: breathe 2.4s ease-in-out infinite;
  color: #3b82f6;
}

.train-seats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 320px;
}

.seat-chip {
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.12);
  min-width: 84px;
  gap: 4px;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.seat-chip.ok {
  border-color: rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.08);
}
.seat-chip.tense {
  border-color: rgba(245, 158, 11, 0.35);
  background: rgba(245, 158, 11, 0.08);
}
.seat-chip.empty {
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.05);
  opacity: 0.55;
}

.seat-chip.sold {
  opacity: 0.5;
}

.seat-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 6px;
}

.seat-bottom {
  display: flex;
  align-items: center;
  gap: 4px;
}

.seat-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}

.seat-price {
  font-size: 13px;
  color: #fbbf24;
  font-weight: 700;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.seat-chip.ok .seat-count {
  color: #10b981;
}
.seat-chip.tense .seat-count {
  color: #f59e0b;
}
.seat-chip.empty .seat-count {
  color: #ef4444;
}

.seat-count {
  font-size: 10px;
  font-weight: 700;
}

.seat-count.sold-out {
  color: #ef4444;
}

.seat-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(148, 163, 184, 0.1);
  overflow: hidden;
}

.seat-bar-fill {
  height: 100%;
  border-radius: 0 2px 2px 0;
  transition: width 0.5s ease;
}

.seat-chip.ok .seat-bar-fill {
  background: linear-gradient(90deg, #10b981, #34d399);
}
.seat-chip.tense .seat-bar-fill {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}
.seat-chip.empty .seat-bar-fill {
  background: #475569;
  width: 3% !important;
}

.train-action {
  flex-shrink: 0;
}

.buy-btn {
  padding: 12px 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.buy-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.empty-state {
  padding: 80px 32px;
  text-align: center;
}

.empty-state svg {
  width: 96px;
  height: 72px;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 18px;
  color: #e2e8f0;
  margin-bottom: 8px;
}

.empty-state p {
  color: #64748b;
  font-size: 14px;
}

.list-enter-active {
  animation: listIn 0.4s ease-out;
}

.list-leave-active {
  animation: listIn 0.25s ease-in reverse;
}

.list-move {
  transition: transform 0.25s ease;
}

@keyframes listIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 40px 20px 40px;
  }
  .hero-title {
    font-size: 28px;
  }
  .search-box {
    padding: 20px;
  }
  .search-row {
    flex-direction: column;
  }
  .station-group {
    min-width: auto;
  }
  .date-col {
    min-width: auto;
  }
  .search-btn {
    width: 100%;
    height: 48px;
  }
  .train-card {
    flex-direction: column;
    align-items: stretch;
    padding: 20px;
    gap: 16px;
  }
  .train-info {
    flex-direction: column;
    gap: 12px;
  }
  .train-num-wrap {
    border-right: none;
    border-bottom: 1px solid rgba(148, 163, 184, 0.15);
    padding-right: 0;
    padding-bottom: 12px;
  }
  .train-seats {
    max-width: 100%;
  }
  .buy-btn {
    width: 100%;
  }
  .stats-row {
    gap: 12px;
  }
  .stat-item {
    padding: 12px 18px;
  }
}

.hot-routes-wrap {
  margin-top: 24px;
  padding: 22px 24px;
}

.hr-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
  margin-bottom: 16px;
}

.hr-icon {
  font-size: 18px;
}

.hr-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.hr-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(30, 41, 59, 0.5);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition:
    transform 0.2s,
    border-color 0.2s,
    background 0.2s;
  text-align: center;
  animation: hrFade 0.5s ease both;
}

@keyframes hrFade {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hr-card:hover {
  transform: translateY(-3px);
  border-color: rgba(96, 165, 250, 0.4);
  background: rgba(59, 130, 246, 0.1);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);
}

.hr-from,
.hr-to {
  font-size: 15px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.hr-to {
  color: #fbbf24;
}

.hr-arrow {
  position: relative;
  width: 100%;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px 0;
}

.hr-arrow-line {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.4), transparent);
}

.hr-dur {
  position: relative;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 8px;
  background: rgba(15, 23, 42, 0.8);
  color: #94a3b8;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  z-index: 1;
}

.hr-rail {
  font-size: 10px;
  color: #64748b;
  margin-top: 4px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  letter-spacing: 0.5px;
}

html[data-theme='light'] .hot-routes-wrap {
  background: rgba(255, 255, 255, 0.7);
}
html[data-theme='light'] .hr-title {
  color: #1e293b;
}
html[data-theme='light'] .hr-card {
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(100, 116, 139, 0.15);
}
html[data-theme='light'] .hr-card:hover {
  border-color: rgba(37, 99, 235, 0.4);
  background: rgba(37, 99, 235, 0.06);
}
html[data-theme='light'] .hr-from,
html[data-theme='light'] .hr-to {
  color: #1e293b;
}
html[data-theme='light'] .hr-to {
  color: #d97706;
}
html[data-theme='light'] .hr-arrow-line {
  background: linear-gradient(90deg, transparent, rgba(100, 116, 139, 0.3), transparent);
}
html[data-theme='light'] .hr-dur {
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  border-color: rgba(100, 116, 139, 0.25);
}
html[data-theme='light'] .hr-rail {
  color: #64748b;
}

.success-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.72);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.success-card {
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 24px;
  padding: 40px 48px;
  min-width: 380px;
  max-width: 440px;
  text-align: center;
  position: relative;
  box-shadow:
    0 0 0 1px rgba(16, 185, 129, 0.1),
    0 25px 60px rgba(0, 0, 0, 0.6),
    0 0 80px rgba(16, 185, 129, 0.15);
}

.success-card::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.4), transparent 50%, rgba(59, 130, 246, 0.3));
  z-index: -1;
  animation: successGlow 3s ease-in-out infinite;
}

@keyframes successGlow {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.9;
  }
}

.success-check {
  width: 88px;
  height: 88px;
  margin: 0 auto 20px;
}

.check-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  stroke-width: 4;
  stroke: #10b981;
  stroke-miterlimit: 10;
  animation: successPop 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.check-circle circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  animation: successCircle 0.6s ease-out 0.1s forwards;
}

.check-circle path {
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  animation: successCheck 0.4s ease-out 0.55s forwards;
}

@keyframes successPop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  60% {
    transform: scale(1.15);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes successCircle {
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes successCheck {
  to {
    stroke-dashoffset: 0;
  }
}

.success-title {
  font-size: 26px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 24px;
  letter-spacing: 0.5px;
  text-shadow: 0 0 20px rgba(16, 185, 129, 0.4);
}

.success-info {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 28px;
}

.si-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  color: #e2e8f0;
  font-size: 14px;
}

.si-row + .si-row {
  border-top: 1px dashed rgba(148, 163, 184, 0.15);
}

.si-label {
  color: #94a3b8;
  font-size: 13px;
}

.si-train {
  font-weight: 700;
  color: #93c5fd;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(99, 102, 241, 0.2));
  padding: 2px 10px;
  border-radius: 8px;
  font-size: 13px;
}

.si-route {
  font-weight: 600;
  color: #e2e8f0;
}

.success-actions {
  display: flex;
  gap: 12px;
}

.sa-btn {
  flex: 1;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
}

.sa-btn.ghost {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.3);
  color: #cbd5e1;
}

.sa-btn.ghost:hover {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.5);
  transform: translateY(-2px);
}

.sa-btn.primary {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.35);
}

.sa-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(16, 185, 129, 0.55);
}

.success-dialog-enter-active {
  animation: sdEnter 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

.success-dialog-leave-active {
  animation: sdLeave 0.25s ease-in both;
}

@keyframes sdEnter {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes sdLeave {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

.success-dialog-enter-active .success-card {
  animation: sdCard 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

@keyframes sdCard {
  from {
    transform: translateY(30px) scale(0.92);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}
</style>

<style>
.empty-train-svg {
  width: 180px;
  height: 120px;
}

.empty-train-svg .et-track {
  stroke-dashoffset: 20;
  animation: etTrackFlow 1.6s linear infinite;
}

@keyframes etTrackFlow {
  to {
    stroke-dashoffset: -20;
  }
}

.empty-train-svg .et-cloud {
  opacity: 0.5;
}

.empty-train-svg .et-c1 {
  animation: etCloud1 14s ease-in-out infinite;
}

.empty-train-svg .et-c2 {
  animation: etCloud2 18s ease-in-out infinite;
}

@keyframes etCloud1 {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(-12px);
  }
}

@keyframes etCloud2 {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(10px);
  }
}

.empty-train-svg .et-body {
  animation: etBounce 2.4s ease-in-out infinite;
}

@keyframes etBounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

.empty-train-svg .et-fill {
  animation: etGradShift 4s ease infinite;
}

@keyframes etGradShift {
  0%,
  100% {
    opacity: 0.85;
  }
  50% {
    opacity: 1;
  }
}

.empty-train-svg .et-wheel {
  transform-origin: center;
  animation: etWheelSpin 1.2s linear infinite;
}

@keyframes etWheelSpin {
  to {
    transform: rotate(360deg);
  }
}

.empty-train-svg .et-smoke {
  opacity: 0.5;
  animation: etSmoke 2s ease-out infinite;
}

.empty-train-svg .et-s2 {
  animation-delay: 0.6s;
}

@keyframes etSmoke {
  0% {
    opacity: 0;
    transform: translateY(0);
  }
  40% {
    opacity: 0.6;
  }
  100% {
    opacity: 0;
    transform: translateY(-10px);
  }
}

.buy-dialog {
  border-radius: 18px !important;
  overflow: hidden;
}

.buy-dialog .el-dialog {
  background: linear-gradient(145deg, #1e293b 0%, #0f172a 100%) !important;
  border: 1px solid rgba(59, 130, 246, 0.2);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(59, 130, 246, 0.08);
}

.buy-dialog .el-dialog__header {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  padding: 18px 24px;
  border: none;
  margin: 0;
}

.buy-dialog .el-dialog__title {
  color: #fff;
  font-weight: 700;
  font-size: 17px;
  letter-spacing: 1px;
}

.buy-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.8);
  font-size: 18px;
}

.buy-dialog .el-dialog__headerbtn:hover .el-dialog__close {
  color: #fff;
}

.buy-dialog .el-dialog__body {
  padding: 24px;
}

.buy-dialog .el-dialog__footer {
  padding: 14px 24px 22px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.bd-train-info {
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 22px;
}

.bd-train-no {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #60a5fa;
  letter-spacing: 2px;
  margin-bottom: 8px;
}

.bd-train-route {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e2e8f0;
  font-size: 14px;
}

.bd-station {
  font-weight: 600;
  font-size: 15px;
  color: #f1f5f9;
}

.bd-time {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 13px;
  color: #94a3b8;
}

.bd-arrow {
  color: #3b82f6;
  font-weight: 700;
}

.bd-form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bd-label {
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 500;
}

.bd-select {
  width: 100%;
}

.bd-add-pax {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  padding: 2px 10px;
  border-radius: 6px;
  border: 1px solid rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.bd-add-pax:hover {
  background: rgba(59, 130, 246, 0.2);
}

.bd-pax-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bd-pax-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 10px;
  position: relative;
  width: 100%;
}

.bd-pax-chip.main {
  border-color: rgba(59, 130, 246, 0.4);
  background: rgba(59, 130, 246, 0.08);
}

.bd-pax-input {
  flex: 1;
}

.bd-pax-input .el-input__wrapper {
  background: rgba(15, 23, 42, 0.5) !important;
  box-shadow: none !important;
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 6px;
  padding: 2px 10px;
}

.bd-pax-input .el-input__inner {
  color: #e2e8f0;
  font-size: 13px;
}

.bd-pax-input .el-input__inner::placeholder {
  color: #475569;
}

.bd-pax-input .el-input__wrapper:hover,
.bd-pax-input .el-input__wrapper.is-focus {
  border-color: rgba(59, 130, 246, 0.6);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15) !important;
}

.bd-pax-del {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bd-pax-add {
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px dashed rgba(148, 163, 184, 0.3);
  background: transparent;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.bd-pax-add:hover {
  border-color: rgba(59, 130, 246, 0.4);
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.08);
}

.bd-saved-pax {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(148, 163, 184, 0.2);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.bd-saved-label {
  font-size: 12px;
  color: #94a3b8;
}

.bd-saved-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 16px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.bd-saved-chip:hover {
  background: rgba(59, 130, 246, 0.25);
  color: #dbeafe;
  transform: translateY(-1px);
}

.bd-saved-id {
  font-size: 10px;
  opacity: 0.6;
}

.bd-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  margin-top: 4px;
}

.bd-sum-label {
  font-size: 13px;
  color: #64748b;
}

.bd-sum-price {
  font-size: 22px;
  font-weight: 800;
  color: #fbbf24;
}

.buy-dialog .el-select .el-input__wrapper {
  background: rgba(15, 23, 42, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  transition: all 0.2s;
}

.buy-dialog .el-select .el-input__wrapper.is-focus,
.buy-dialog .el-select .el-input__wrapper:hover {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
}

.buy-dialog .el-select .el-input__inner {
  color: #f1f5f9 !important;
  font-weight: 600 !important;
}

.buy-dialog .el-select .el-input__placeholder {
  color: #64748b !important;
}

.buy-dialog .el-select .el-input__suffix {
  color: #60a5fa !important;
}

.bd-opt-name {
  font-weight: 600;
  font-size: 14px;
  color: #f1f5f9;
  margin-right: 10px;
}

.bd-opt-price {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 13px;
  color: #60a5fa;
  margin-right: 10px;
}

.bd-opt-count {
  font-size: 12px;
  color: #34d399;
  margin-left: auto;
}

.buy-dialog .el-select-dropdown {
  background: #0f172a !important;
  border: 1px solid rgba(59, 130, 246, 0.2) !important;
  border-radius: 10px !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6) !important;
}

.buy-dialog .el-select-dropdown__item {
  color: #cbd5e1 !important;
  font-size: 14px !important;
  border-radius: 6px !important;
  margin: 2px 4px !important;
  padding-left: 12px !important;
  padding-right: 12px !important;
}

.buy-dialog .el-select-dropdown__item.is-hovering,
.buy-dialog .el-select-dropdown__item:hover {
  background: rgba(59, 130, 246, 0.15) !important;
  color: #f1f5f9 !important;
}

.buy-dialog .el-select-dropdown__item.is-selected {
  background: rgba(59, 130, 246, 0.25) !important;
  color: #fff !important;
  font-weight: 600;
}

.buy-dialog .el-button {
  border-radius: 10px;
  padding: 10px 22px;
  font-weight: 600;
}

.buy-dialog .el-button:not(.el-button--primary) {
  background: rgba(51, 65, 85, 0.7);
  border-color: rgba(148, 163, 184, 0.3);
  color: #cbd5e1;
}

.buy-dialog .el-button:not(.el-button--primary):hover {
  background: rgba(71, 85, 105, 0.9);
  color: #fff;
}

.buy-dialog .el-button--primary {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
}

.buy-dialog .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.55);
}

.pax-mgr-add {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.pax-mgr-row {
  display: flex;
  gap: 8px;
  flex: 1;
}

.pax-mgr-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pax-mgr-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 10px;
}

.pax-mgr-info {
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
  flex: 1;
}

.pax-mgr-info:hover .pax-mgr-name {
  color: #60a5fa;
}

.pax-mgr-name {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.pax-mgr-id {
  font-size: 12px;
  color: #64748b;
  font-family: Consolas, Monaco, monospace;
}

.pax-mgr-actions {
  display: flex;
  gap: 2px;
}

.pax-mgr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #475569;
}

.pax-mgr-empty svg {
  color: #334155;
}

@keyframes countdownSoonPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.75;
    transform: scale(1.05);
  }
}

@keyframes countdownNowPulse {
  0%,
  100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 10px 4px rgba(239, 68, 68, 0.3);
  }
}

html[data-theme='light'] .countdown-tag.ok {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border-color: rgba(16, 185, 129, 0.35);
}
html[data-theme='light'] .countdown-tag.soon {
  background: rgba(245, 158, 11, 0.15);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.35);
}
html[data-theme='light'] .countdown-tag.now {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.4);
}
html[data-theme='light'] .countdown-tag.departed {
  color: #9ca3af;
  background: rgba(156, 163, 175, 0.12);
  border-color: rgba(156, 163, 175, 0.3);
}
html[data-theme='light'] .train-card.favorited {
  border-color: rgba(217, 119, 6, 0.35) !important;
}
html[data-theme='light'] .train-card.favorited .train-num {
  color: #d97706;
}

.payfade-enter-active {
  transition: opacity 0.22s ease;
}
.payfade-leave-active {
  transition: opacity 0.18s ease;
}
.payfade-enter-from,
.payfade-leave-to {
  opacity: 0;
}
.payfade-enter-active .pay-card {
  animation: payIn 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes payIn {
  from {
    transform: translateY(16px) scale(0.97);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.pay-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(2, 6, 23, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.pay-card {
  width: min(480px, 100%);
  background: linear-gradient(180deg, #1e293b, #0f172a);
  border: 1px solid rgba(96, 165, 250, 0.25);
  border-radius: 18px;
  padding: 24px 26px 20px;
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.5),
    0 0 60px rgba(59, 130, 246, 0.08);
}

.pay-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.pay-title {
  font-size: 17px;
  font-weight: 700;
  color: #f1f5f9;
}

.pay-countdown {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(96, 165, 250, 0.25);
}

.pay-countdown.urgent {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.35);
  animation: urgentPulse 1s ease-in-out infinite;
}

@keyframes urgentPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.3);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
}

.pc-label {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.pc-time {
  font-size: 16px;
  font-weight: 800;
  color: #60a5fa;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.pay-countdown.urgent .pc-time {
  color: #f87171;
}

.pay-amount {
  text-align: center;
  padding: 14px;
  margin: 4px 0 14px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(96, 165, 250, 0.15);
}

.pa-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.pa-value {
  font-size: 34px;
  font-weight: 900;
  color: #60a5fa;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  letter-spacing: -0.5px;
}

.pay-info {
  background: rgba(148, 163, 184, 0.06);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.pi-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
}

.pi-k {
  color: #94a3b8;
  font-weight: 600;
}

.pi-v {
  color: #e2e8f0;
  font-weight: 600;
  font-family: 'SF Mono', monospace;
}

.pay-methods {
  margin-bottom: 16px;
}

.pm-label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 10px;
  letter-spacing: 0.5px;
}

.pm-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.pm-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  border: 2px solid rgba(148, 163, 184, 0.15);
  background: rgba(148, 163, 184, 0.05);
  cursor: pointer;
  transition: all 0.18s;
  position: relative;
}

.pm-card:hover {
  border-color: rgba(96, 165, 250, 0.3);
  background: rgba(59, 130, 246, 0.08);
}
.pm-card.active {
  border-color: rgba(96, 165, 250, 0.6);
  background: rgba(59, 130, 246, 0.12);
}

.pm-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 900;
  color: #fff;
  flex-shrink: 0;
}

.pm-icon.alipay {
  background: linear-gradient(135deg, #1677ff, #0060cc);
}
.pm-icon.wechat {
  background: linear-gradient(135deg, #07c160, #05964a);
}

.pm-body {
  flex: 1;
  min-width: 0;
}

.pm-name {
  font-size: 14px;
  font-weight: 700;
  color: #f1f5f9;
}
.pm-desc {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
}

.pm-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pay-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.pay-btn {
  flex: 1;
  padding: 13px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pay-btn.cancel {
  background: rgba(148, 163, 184, 0.1);
  color: #cbd5e1;
  border: 1px solid rgba(148, 163, 184, 0.25);
}
.pay-btn.cancel:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.2);
}

.pay-btn.primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  border: none;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}
.pay-btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(59, 130, 246, 0.45);
}

.pay-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.pay-spinner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  animation: spin 0.7s linear infinite;
}

.pay-tip {
  text-align: center;
  font-size: 11px;
  color: #64748b;
  padding: 8px 0;
  border-top: 1px dashed rgba(148, 163, 184, 0.15);
}

html[data-theme='light'] .pay-overlay {
  background: rgba(15, 23, 42, 0.35);
}
html[data-theme='light'] .pay-card {
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.15);
}
html[data-theme='light'] .pay-title {
  color: #0f172a;
}
html[data-theme='light'] .pay-countdown {
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(37, 99, 235, 0.25);
}
html[data-theme='light'] .pay-countdown.urgent {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.3);
}
html[data-theme='light'] .pc-label {
  color: #64748b;
}
html[data-theme='light'] .pc-time {
  color: #2563eb;
}
html[data-theme='light'] .pay-countdown.urgent .pc-time {
  color: #dc2626;
}
html[data-theme='light'] .pay-amount {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(37, 99, 235, 0.15);
}
html[data-theme='light'] .pa-label {
  color: #64748b;
}
html[data-theme='light'] .pa-value {
  color: #2563eb;
}
html[data-theme='light'] .pay-info {
  background: rgba(148, 163, 184, 0.06);
  border-color: rgba(148, 163, 184, 0.15);
}
html[data-theme='light'] .pi-k {
  color: #64748b;
}
html[data-theme='light'] .pi-v {
  color: #1e293b;
}
html[data-theme='light'] .pm-label {
  color: #64748b;
}
html[data-theme='light'] .pm-card {
  background: rgba(148, 163, 184, 0.08);
  border-color: rgba(148, 163, 184, 0.2);
}
html[data-theme='light'] .pm-card:hover {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(37, 99, 235, 0.3);
}
html[data-theme='light'] .pm-card.active {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(37, 99, 235, 0.5);
}
html[data-theme='light'] .pm-name {
  color: #0f172a;
}
html[data-theme='light'] .pm-desc {
  color: #64748b;
}
html[data-theme='light'] .pay-btn.cancel {
  background: rgba(148, 163, 184, 0.1);
  color: #334155;
  border-color: rgba(148, 163, 184, 0.25);
}
html[data-theme='light'] .pay-btn.cancel:hover {
  background: rgba(148, 163, 184, 0.2);
}
html[data-theme='light'] .pay-tip {
  color: #64748b;
  border-color: rgba(148, 163, 184, 0.2);
}

html[data-theme='light'] .seat-chip {
  background: rgba(15, 23, 42, 0.03);
  border-color: rgba(148, 163, 184, 0.25);
}
html[data-theme='light'] .seat-chip.ok {
  border-color: rgba(16, 185, 129, 0.4);
  background: rgba(16, 185, 129, 0.06);
}
html[data-theme='light'] .seat-chip.tense {
  border-color: rgba(245, 158, 11, 0.5);
  background: rgba(245, 158, 11, 0.06);
}
html[data-theme='light'] .seat-chip.empty {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.04);
}
html[data-theme='light'] .seat-label {
  color: #475569;
}
html[data-theme='light'] .seat-price {
  color: #d97706;
}
html[data-theme='light'] .seat-bar {
  background: rgba(148, 163, 184, 0.2);
}

html[data-theme='light'] .search-box {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 252, 0.98)) !important;
  border: 1px solid rgba(37, 99, 235, 0.2) !important;
  box-shadow:
    0 20px 60px rgba(15, 23, 42, 0.08),
    0 0 40px rgba(59, 130, 246, 0.06) !important;
}

html[data-theme='light'] .hero-title {
  color: #0f172a;
}
html[data-theme='light'] .hero-sub {
  color: #64748b;
}

.detail-btn {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: rgba(96, 165, 250, 0.1);
  border: 1px solid rgba(96, 165, 250, 0.25);
  color: #60a5fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.detail-btn:hover {
  background: rgba(96, 165, 250, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

html[data-theme='light'] .detail-btn {
  background: rgba(37, 99, 235, 0.06);
  border-color: rgba(37, 99, 235, 0.25);
  color: #2563eb;
}
html[data-theme='light'] .detail-btn:hover {
  background: rgba(37, 99, 235, 0.12);
}

.td-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(2, 6, 23, 0.75);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
}

.td-card {
  width: min(560px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #1e293b, #0f172a);
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
}

.td-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.td-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.td-train-num {
  font-size: 24px;
  font-weight: 900;
  color: #60a5fa;
  font-family: 'SF Mono', monospace;
  letter-spacing: 1px;
}

.td-type {
  font-size: 12px;
  color: #94a3b8;
  padding: 2px 8px;
  background: rgba(96, 165, 250, 0.1);
  border-radius: 6px;
  display: inline-block;
  width: fit-content;
}

.td-date {
  font-size: 12px;
  color: #94a3b8;
}

.td-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(148, 163, 184, 0.1);
  border: none;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s;
}
.td-close:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.td-route-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(59, 130, 246, 0.06);
  border-radius: 14px;
  margin-bottom: 20px;
  border: 1px solid rgba(96, 165, 250, 0.15);
}

.td-big-station {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
}

.td-big-time {
  font-size: 20px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', monospace;
}

.td-big-name {
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  margin-top: 2px;
}

.td-big-track {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.td-big-line {
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #60a5fa, transparent);
  border-radius: 2px;
}

.td-big-dur {
  font-size: 11px;
  color: #60a5fa;
  padding: 2px 8px;
  background: rgba(96, 165, 250, 0.1);
  border-radius: 10px;
  font-weight: 600;
}

.td-stops-section,
.td-seats-section {
  margin-bottom: 20px;
}

.td-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #e2e8f0;
  margin-bottom: 12px;
}

.td-loading {
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.td-spinner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba(96, 165, 250, 0.2);
  border-top-color: #60a5fa;
  animation: spin 0.7s linear infinite;
}

.td-stops-list {
  position: relative;
  padding-left: 16px;
}

.td-stop {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 8px 0;
  position: relative;
}

.td-stop-time {
  width: 56px;
  flex-shrink: 0;
  text-align: right;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  font-family: 'SF Mono', monospace;
  font-size: 12px;
}

.td-arr {
  color: #fbbf24;
  font-weight: 700;
}
.td-dep {
  color: #60a5fa;
  font-weight: 600;
  font-size: 11px;
}

.td-stop-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #475569;
  flex-shrink: 0;
  margin-top: 12px;
  position: relative;
  z-index: 1;
}

.td-stop.first .td-stop-dot {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}
.td-stop.last .td-stop-dot {
  background: #ef4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
}

.td-stop-line {
  position: absolute;
  left: 81px;
  top: 22px;
  width: 2px;
  bottom: -8px;
  background: rgba(148, 163, 184, 0.2);
}

.td-stop-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.td-stop-name {
  font-size: 14px;
  font-weight: 700;
  color: #e2e8f0;
}
.td-stop-city {
  font-size: 11px;
  color: #64748b;
}

.td-stop-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}
.td-stop-tag.start {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}
.td-stop-tag.end {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.td-stops-empty {
  text-align: center;
  padding: 20px;
  color: #64748b;
  font-size: 13px;
}

.td-seats-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.td-seat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.td-seat-row.ok {
  border-color: rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.06);
}
.td-seat-row.tense {
  border-color: rgba(245, 158, 11, 0.35);
  background: rgba(245, 158, 11, 0.06);
}
.td-seat-row.empty {
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.04);
  opacity: 0.6;
}

.td-seat-name {
  width: 70px;
  font-size: 13px;
  font-weight: 700;
  color: #e2e8f0;
  flex-shrink: 0;
}

.td-seat-bar-wrap {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: rgba(148, 163, 184, 0.15);
  overflow: hidden;
}

.td-seat-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.td-seat-row.ok .td-seat-bar-fill {
  background: linear-gradient(90deg, #10b981, #34d399);
}
.td-seat-row.tense .td-seat-bar-fill {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}
.td-seat-row.empty .td-seat-bar-fill {
  background: #475569;
}

.td-seat-count {
  width: 50px;
  text-align: right;
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 2px;
}

.td-seat-num {
  font-size: 16px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', monospace;
}
.td-seat-label {
  font-size: 11px;
  color: #94a3b8;
}
.td-seat-price {
  font-size: 14px;
  font-weight: 700;
  color: #fbbf24;
  font-family: 'SF Mono', monospace;
  width: 55px;
  text-align: right;
  flex-shrink: 0;
}

.td-footer {
  padding-top: 4px;
}

.td-buy-btn {
  width: 100%;
  padding: 14px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
}

.td-buy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(59, 130, 246, 0.4);
}

html[data-theme='light'] .td-overlay {
  background: rgba(15, 23, 42, 0.4);
}
html[data-theme='light'] .td-card {
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.18);
}
html[data-theme='light'] .td-train-num {
  color: #2563eb;
}
html[data-theme='light'] .td-type {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
}
html[data-theme='light'] .td-date {
  color: #64748b;
}
html[data-theme='light'] .td-route-summary {
  background: rgba(37, 99, 235, 0.04);
  border-color: rgba(37, 99, 235, 0.15);
}
html[data-theme='light'] .td-big-time {
  color: #0f172a;
}
html[data-theme='light'] .td-big-name {
  color: #475569;
}
html[data-theme='light'] .td-big-dur {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
}
html[data-theme='light'] .td-section-title {
  color: #0f172a;
}
html[data-theme='light'] .td-seat-row {
  background: rgba(148, 163, 184, 0.06);
  border-color: rgba(148, 163, 184, 0.15);
}
html[data-theme='light'] .td-seat-name {
  color: #0f172a;
}
html[data-theme='light'] .td-seat-num {
  color: #0f172a;
}
html[data-theme='light'] .td-seat-label {
  color: #64748b;
}
html[data-theme='light'] .td-seat-bar-wrap {
  background: rgba(148, 163, 184, 0.2);
}
html[data-theme='light'] .td-stop-name {
  color: #0f172a;
}
html[data-theme='light'] .td-close {
  background: rgba(148, 163, 184, 0.1);
  color: #64748b;
}
</style>
